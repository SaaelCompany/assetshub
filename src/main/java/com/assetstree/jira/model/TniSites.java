package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Which site a host name belongs to. A line {@code KRUL = Крулево} puts {@code KRUL-IT}
 * and {@code KRUL-BUH} on the site whose card is named Крулево.
 */
public final class TniSites {
    private static final int RULES = 40;

    private TniSites() {
    }

    /** Stored form without the separators used by the module settings. */
    public static String store(String raw) {
        List<Rule> rules = parse(raw);
        StringBuilder stored = new StringBuilder();
        for (int i = 0; i < rules.size(); i++) {
            if (i > 0) {
                stored.append(',');
            }
            stored.append(rules.get(i).code).append('~').append(rules.get(i).place);
        }
        return stored.toString();
    }

    /** Lines for the settings field. */
    public static String text(String stored) {
        List<Rule> rules = parse(stored);
        StringBuilder lines = new StringBuilder();
        for (int i = 0; i < rules.size(); i++) {
            if (i > 0) {
                lines.append('\n');
            }
            lines.append(rules.get(i).code).append(" = ").append(rules.get(i).place);
        }
        return lines.toString();
    }

    /** Site card name for this host or object name, or an empty string. The longest code wins. */
    public static String match(String rules, String host, String name) {
        String station = TniText.shortHost(host);
        if (station.isEmpty()) {
            station = TniText.shortHost(name);
        }
        if (station.isEmpty()) {
            return "";
        }
        String place = "";
        int length = -1;
        List<Rule> parsed = parse(rules);
        for (int i = 0; i < parsed.size(); i++) {
            Rule rule = parsed.get(i);
            if (rule.code.length() <= length || !fits(station, rule.code)) {
                continue;
            }
            place = rule.place;
            length = rule.code.length();
        }
        return place;
    }

    private static boolean fits(String station, String code) {
        return station.equals(code) || station.startsWith(code + "-");
    }

    private static List<Rule> parse(String raw) {
        List<Rule> rules = new ArrayList<Rule>();
        if (raw == null || raw.trim().isEmpty()) {
            return rules;
        }
        String[] lines = raw.indexOf('\n') >= 0 || raw.indexOf('=') >= 0
                ? raw.split("\\r?\\n")
                : new String[]{raw};
        if (raw.indexOf('\n') < 0 && raw.indexOf('=') < 0) {
            lines = raw.split(",");
        }
        for (int i = 0; i < lines.length && rules.size() < RULES; i++) {
            Rule rule = rule(lines[i]);
            if (rule != null) {
                rules.add(rule);
            }
        }
        return rules;
    }

    private static Rule rule(String line) {
        if (line == null) {
            return null;
        }
        String text = line.trim();
        if (text.isEmpty() || text.charAt(0) == '#') {
            return null;
        }
        int mark = text.indexOf('=');
        int stored = text.indexOf('~');
        if (mark < 0 || (stored >= 0 && stored < mark)) {
            mark = stored;
        }
        if (mark <= 0 || mark >= text.length() - 1) {
            return null;
        }
        String code = code(text.substring(0, mark));
        String place = place(text.substring(mark + 1));
        if (code.isEmpty() || place.isEmpty()) {
            return null;
        }
        return new Rule(code, place);
    }

    private static String code(String raw) {
        String text = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (text.isEmpty() || text.length() > 32) {
            return "";
        }
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            boolean ok = (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '-'
                    || Character.isLetter(ch);
            if (!ok) {
                return "";
            }
        }
        if (text.charAt(0) == '-' || text.charAt(text.length() - 1) == '-') {
            return "";
        }
        return text;
    }

    private static String place(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        if (text.length() > 80) {
            text = text.substring(0, 80).trim();
        }
        StringBuilder kept = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == ',' || ch == ';' || ch == '=' || ch == '~' || ch == '\n' || ch == '\r') {
                ch = ' ';
            }
            kept.append(ch);
        }
        return kept.toString().trim();
    }

    private static final class Rule {
        private final String code;
        private final String place;

        private Rule(String code, String place) {
            this.code = code;
            this.place = place;
        }
    }
}
