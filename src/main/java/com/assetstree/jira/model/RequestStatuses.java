package com.assetstree.jira.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Which Jira request statuses the period report counts.
 * An empty setting counts every status. {@code open} skips the Done category.
 * {@code -} counts nothing. A comma-separated list counts those status ids.
 */
public final class RequestStatuses {
    public static final String OPEN = "open";
    public static final String NONE = "-";
    public static final String DONE = "done";
    private static final int LIMIT = 40;

    private RequestStatuses() {
    }

    public static String store(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return "";
        }
        if (NONE.equals(text) || OPEN.equals(text)) {
            return text;
        }
        StringBuilder out = new StringBuilder();
        Set<String> seen = new LinkedHashSet<String>();
        String[] parts = text.split(",");
        for (int i = 0; i < parts.length; i++) {
            String id = parts[i] == null ? "" : parts[i].trim();
            if (!isId(id) || seen.contains(id) || seen.size() >= LIMIT) {
                continue;
            }
            seen.add(id);
            if (out.length() > 0) {
                out.append(',');
            }
            out.append(id);
        }
        return out.length() == 0 ? NONE : out.toString();
    }

    /** {@code category} is the Jira status category key, such as {@code done}. */
    public static boolean counts(String stored, String statusId, String category) {
        String clean = store(stored);
        if (clean.isEmpty()) {
            return true;
        }
        if (NONE.equals(clean)) {
            return false;
        }
        if (OPEN.equals(clean)) {
            return category == null || !DONE.equals(category.trim().toLowerCase(Locale.ROOT));
        }
        String id = statusId == null ? "" : statusId.trim();
        return !id.isEmpty() && statuses(clean).contains(id);
    }

    public static Set<String> statuses(String stored) {
        String clean = store(stored);
        if (clean.isEmpty() || OPEN.equals(clean) || NONE.equals(clean)) {
            return Collections.emptySet();
        }
        LinkedHashSet<String> keys = new LinkedHashSet<String>();
        String[] parts = clean.split(",");
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                keys.add(parts[i]);
            }
        }
        return Collections.unmodifiableSet(keys);
    }

    private static boolean isId(String id) {
        if (id == null || id.isEmpty() || id.length() > 18) {
            return false;
        }
        for (int i = 0; i < id.length(); i++) {
            char current = id.charAt(i);
            if (current < '0' || current > '9') {
                return false;
            }
        }
        return true;
    }
}
