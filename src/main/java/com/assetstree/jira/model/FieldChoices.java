package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Options and stored values for the choice, label, link, and version fields.
 * A list is kept as one value with a line between items.
 */
public final class FieldChoices {
    public static final int MAX_OPTIONS = 40;
    public static final int MAX_OPTION = 80;
    public static final int MAX_LABELS = 20;
    public static final int MAX_LABEL = 40;
    public static final int MAX_VERSIONS = 12;

    private static final Pattern URL = Pattern.compile("^https?://\\S{1,480}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern VERSION = Pattern.compile("^(?=.*\\d)[0-9A-Za-z][0-9A-Za-z .+_-]{0,39}$");

    private FieldChoices() {
    }

    public static boolean needsOptions(String kind) {
        return FieldKinds.SELECT.equals(kind) || FieldKinds.SELECTS.equals(kind)
                || FieldKinds.CHECKS.equals(kind) || FieldKinds.RADIO.equals(kind);
    }

    public static boolean handles(String kind) {
        return needsOptions(kind) || FieldKinds.LABELS.equals(kind) || FieldKinds.URL.equals(kind)
                || FieldKinds.VERSION.equals(kind);
    }

    public static boolean multiple(String kind) {
        return FieldKinds.SELECTS.equals(kind) || FieldKinds.CHECKS.equals(kind)
                || FieldKinds.LABELS.equals(kind) || FieldKinds.VERSION.equals(kind);
    }

    public static String errorKey(String kind) {
        if (FieldKinds.URL.equals(kind)) {
            return "asset-tree.error.url";
        }
        if (FieldKinds.VERSION.equals(kind)) {
            return "asset-tree.error.version";
        }
        if (FieldKinds.LABELS.equals(kind)) {
            return "asset-tree.error.label";
        }
        return "asset-tree.error.field.choice";
    }

    /** One option per line. Null when the list is empty, too long, or has a duplicate spelling. */
    public static String canonicalOptions(String raw) {
        List<String> lines = lines(raw);
        if (lines.isEmpty() || lines.size() > MAX_OPTIONS) {
            return null;
        }
        Set<String> seen = new LinkedHashSet<String>();
        List<String> kept = new ArrayList<String>();
        for (String line : lines) {
            if (line.length() > MAX_OPTION) {
                return null;
            }
            String key = line.toLowerCase(Locale.ROOT);
            if (!seen.add(key)) {
                continue;
            }
            kept.add(line);
        }
        if (kept.isEmpty()) {
            return null;
        }
        return join(kept, "\n");
    }

    public static List<String> optionsOf(String stored) {
        return lines(stored);
    }

    /**
     * Empty stays empty. A usable value comes back in the stored form.
     * Anything that is not allowed for the kind is null.
     */
    public static String canonicalValue(String kind, String options, String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            return "";
        }
        if (FieldKinds.SELECT.equals(kind) || FieldKinds.RADIO.equals(kind)) {
            return matchOption(optionsOf(options), text);
        }
        if (FieldKinds.SELECTS.equals(kind) || FieldKinds.CHECKS.equals(kind)) {
            return canonicalChoices(optionsOf(options), text);
        }
        if (FieldKinds.LABELS.equals(kind)) {
            return canonicalLabels(text);
        }
        if (FieldKinds.URL.equals(kind)) {
            return URL.matcher(text).matches() ? text : null;
        }
        if (FieldKinds.VERSION.equals(kind)) {
            return canonicalVersions(text);
        }
        return text;
    }

    /** A stored list reads as a single line for a sheet cell and a chart label. */
    public static String display(String stored) {
        List<String> items = lines(stored);
        if (items.isEmpty()) {
            return stored == null ? "" : stored;
        }
        return join(items, ", ");
    }

    private static String canonicalChoices(List<String> options, String raw) {
        List<String> tokens = tokens(raw, options);
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }
        List<String> ordered = new ArrayList<String>();
        for (String option : options) {
            for (String token : tokens) {
                if (option.equalsIgnoreCase(token) && !contains(ordered, option)) {
                    ordered.add(option);
                }
            }
        }
        if (ordered.size() != uniqueCount(tokens)) {
            return null;
        }
        return join(ordered, "\n");
    }

    private static String canonicalLabels(String raw) {
        List<String> tokens = tokens(raw, null);
        if (tokens == null || tokens.isEmpty() || tokens.size() > MAX_LABELS) {
            return null;
        }
        List<String> kept = new ArrayList<String>();
        for (String token : tokens) {
            if (token.length() > MAX_LABEL || token.indexOf(',') >= 0) {
                return null;
            }
            if (!contains(kept, token)) {
                kept.add(token);
            }
        }
        return kept.isEmpty() ? null : join(kept, "\n");
    }

    private static String canonicalVersions(String raw) {
        List<String> tokens = tokens(raw, null);
        if (tokens == null || tokens.isEmpty() || tokens.size() > MAX_VERSIONS) {
            return null;
        }
        List<String> kept = new ArrayList<String>();
        for (String token : tokens) {
            if (!VERSION.matcher(token).matches()) {
                return null;
            }
            if (!contains(kept, token)) {
                kept.add(token);
            }
        }
        return kept.isEmpty() ? null : join(kept, "\n");
    }

    private static String matchOption(List<String> options, String token) {
        for (String option : options) {
            if (option.equals(token)) {
                return option;
            }
        }
        for (String option : options) {
            if (option.equalsIgnoreCase(token)) {
                return option;
            }
        }
        return null;
    }

    /**
     * A line break separates items. A comma does too, unless the whole text is one known option.
     * Returns null when a comma-separated piece is blank.
     */
    private static List<String> tokens(String raw, List<String> options) {
        String text = raw.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (text.indexOf('\n') >= 0) {
            return lines(text);
        }
        if (options != null && matchOption(options, text) != null) {
            List<String> one = new ArrayList<String>();
            one.add(text);
            return one;
        }
        if (text.indexOf(',') < 0) {
            List<String> one = new ArrayList<String>();
            one.add(text);
            return one;
        }
        String[] parts = text.split(",", -1);
        List<String> tokens = new ArrayList<String>();
        for (String part : parts) {
            String token = part.trim();
            if (token.isEmpty()) {
                return null;
            }
            tokens.add(token);
        }
        return tokens;
    }

    private static List<String> lines(String raw) {
        List<String> items = new ArrayList<String>();
        if (raw == null || raw.trim().isEmpty()) {
            return items;
        }
        String[] parts = raw.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (String part : parts) {
            String line = part.trim();
            if (!line.isEmpty()) {
                items.add(line);
            }
        }
        return items;
    }

    private static boolean contains(List<String> items, String token) {
        for (String item : items) {
            if (item.equalsIgnoreCase(token)) {
                return true;
            }
        }
        return false;
    }

    private static int uniqueCount(List<String> tokens) {
        Set<String> seen = new LinkedHashSet<String>();
        for (String token : tokens) {
            seen.add(token.toLowerCase(Locale.ROOT));
        }
        return seen.size();
    }

    private static String join(List<String> items, String separator) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                builder.append(separator);
            }
            builder.append(items.get(i));
        }
        return builder.toString();
    }
}
