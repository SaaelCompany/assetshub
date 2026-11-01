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

    private LinkRules() {
    }

    public static boolean builtIn(String key) {
        return PART.equals(key) || DEPENDS.equals(key) || INSTALLED.equals(key);
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
