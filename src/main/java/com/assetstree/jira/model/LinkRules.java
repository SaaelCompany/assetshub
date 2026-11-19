package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Directed link between two equipment cards: a type and a few named attributes. */
public final class LinkRules {
    public static final String PART = "part";
    public static final String DEPENDS = "depends";
    public static final String INSTALLED = "installed";
    public static final int MAX_ATTRS = 12;
    public static final int MAX_VALUE = 200;
    public static final int MAX_INBOUND = 80;

    private LinkRules() {
    }

    public static boolean builtIn(String key) {
        return PART.equals(key) || DEPENDS.equals(key) || INSTALLED.equals(key);
    }

    /** Stored type, and whether the sentence is read from the other card. */
    public static final class Sense {
        private final String type;
        private final boolean reverse;

        public Sense(String type, boolean reverse) {
            this.type = type == null ? "" : type;
            this.reverse = reverse;
        }

        public String getType() {
            return type;
        }

        public boolean isReverse() {
            return reverse;
        }
    }

    /**
     * {@code carries} is «несёт»: the other object is installed on this one.
     * {@code includes} and {@code needed} are the reverse of part and depends.
     */
    public static Sense sense(String raw) {
        String key = typeKey(raw);
        if (key.isEmpty()) {
            return null;
        }
        if ("carries".equals(key)) {
            return new Sense(INSTALLED, true);
        }
        if ("includes".equals(key)) {
            return new Sense(PART, true);
        }
        if ("needed".equals(key) || "needed-by".equals(key)) {
            return new Sense(DEPENDS, true);
        }
        return new Sense(key, false);
    }

    /** A phrase from the card or from Excel. A latin slug is a custom type. */
    public static Sense phrase(String raw) {
        String text = fold(raw);
        if (text.isEmpty()) {
            return null;
        }
        if (text.equals("несет") || text.equals("carries")) {
            return sense("carries");
        }
        if (text.equals("установлено на") || text.equals("установлен на") || text.equals("installed on")
                || text.equals("installed")) {
            return sense(INSTALLED);
        }
        if (text.equals("включает") || text.equals("includes")) {
            return sense("includes");
        }
        if (text.equals("входит в") || text.equals("part of") || text.equals("part")) {
            return sense(PART);
        }
        if (text.equals("нужен для") || text.equals("needed by") || text.equals("needed")) {
            return sense("needed");
        }
        if (text.equals("зависит от") || text.equals("depends on") || text.equals("depends")) {
            return sense(DEPENDS);
        }
        return sense(raw);
    }

    /** {@code port=1; vlan=10} becomes the stored attribute lines. */
    public static String attrsFromCell(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        List<Attr> rows = new ArrayList<Attr>();
        String[] parts = raw.split("[;\\n]");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i] == null ? "" : parts[i].trim();
            int mark = part.indexOf('=');
            if (mark <= 0) {
                continue;
            }
            rows.add(new Attr(part.substring(0, mark).trim(), part.substring(mark + 1).trim()));
        }
        return write(rows);
    }

    public static String attrsToCell(String stored) {
        List<Attr> rows = read(stored);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            if (out.length() > 0) {
                out.append("; ");
            }
            out.append(rows.get(i).getKey()).append('=').append(rows.get(i).getValue());
        }
        return out.toString();
    }

    private static String fold(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim().toLowerCase(Locale.ROOT).replace('ё', 'е');
        StringBuilder builder = new StringBuilder();
        boolean space = false;
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (Character.isWhitespace(current)) {
                space = builder.length() > 0;
                continue;
            }
            if (space) {
                builder.append(' ');
                space = false;
            }
            builder.append(current);
        }
        return builder.toString();
    }

    public static String inbound(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.replace('\n', ' ').replace('\r', ' ').trim();
        if (value.length() > MAX_INBOUND) {
            return value.substring(0, MAX_INBOUND);
        }
        return value;
    }

    /** Ui key for the default phrase. A stored inbound sentence replaces it on the way in. */
    public static String phraseKey(String type, boolean incoming) {
        if (PART.equals(type)) {
            return incoming ? "linkIncludes" : "linkPart";
        }
        if (DEPENDS.equals(type)) {
            return incoming ? "linkNeeded" : "linkDepends";
        }
        if (INSTALLED.equals(type)) {
            return incoming ? "linkCarries" : "linkInstalled";
        }
        return "";
    }

    public static String shownPhrase(String type, boolean incoming, String inboundText) {
        String text = inbound(inboundText);
        if (incoming && !text.isEmpty()) {
            return text;
        }
        String key = phraseKey(type, incoming);
        return key.isEmpty() ? (type == null ? "" : type) : key;
    }

    /** Empty when the type is not a short latin slug. */
    public static String typeKey(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty() || value.length() > 32 || !value.matches("[a-z][a-z0-9-]{0,31}")) {
            return "";
        }
        return value;
    }

    public static final class Attr {
        private final String key;
        private final String value;

        public Attr(String key, String value) {
            this.key = key == null ? "" : key;
            this.value = value == null ? "" : value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }

    public static List<Attr> read(String stored) {
        List<Attr> rows = new ArrayList<Attr>();
        if (stored == null || stored.isEmpty()) {
            return rows;
        }
        String[] lines = stored.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int mark = line.indexOf('=');
            if (mark <= 0) {
                continue;
            }
            String key = typeKey(line.substring(0, mark));
            if (key.isEmpty()) {
                continue;
            }
            rows.add(new Attr(key, clip(line.substring(mark + 1))));
            if (rows.size() >= MAX_ATTRS) {
                break;
            }
        }
        return rows;
    }

    public static String write(List<Attr> rows) {
        if (rows == null || rows.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (int i = 0; i < rows.size(); i++) {
            Attr row = rows.get(i);
            if (row == null) {
                continue;
            }
            String key = typeKey(row.getKey());
            String value = clip(row.getValue());
            if (key.isEmpty() || value.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(key).append('=').append(value);
            count++;
            if (count >= MAX_ATTRS) {
                break;
            }
        }
        return out.toString();
    }

    /**
     * A null patch keeps the stored attributes. A blank value keeps the stored one
     * when {@code skipBlank} is true and removes it otherwise. Keys that are not
     * in the patch stay.
     */
    public static String merge(String stored, List<Attr> patch, boolean skipBlank) {
        Map<String, String> values = new LinkedHashMap<String, String>();
        List<Attr> current = read(stored);
        for (int i = 0; i < current.size(); i++) {
            values.put(current.get(i).getKey(), current.get(i).getValue());
        }
        if (patch == null) {
            return write(current);
        }
        for (int i = 0; i < patch.size(); i++) {
            Attr row = patch.get(i);
            if (row == null) {
                continue;
            }
            String key = typeKey(row.getKey());
            if (key.isEmpty()) {
                continue;
            }
            String value = clip(row.getValue());
            if (value.isEmpty()) {
                if (!skipBlank) {
                    values.remove(key);
                }
                continue;
            }
            if (!values.containsKey(key) && values.size() >= MAX_ATTRS) {
                continue;
            }
            values.put(key, value);
        }
        List<Attr> merged = new ArrayList<Attr>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            merged.add(new Attr(entry.getKey(), entry.getValue()));
        }
        return write(merged);
    }

    private static String clip(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.replace('\n', ' ').replace('\r', ' ').trim();
        if (value.length() > MAX_VALUE) {
            return value.substring(0, MAX_VALUE);
        }
        return value;
    }
}
