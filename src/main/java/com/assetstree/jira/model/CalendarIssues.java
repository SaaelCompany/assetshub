package com.assetstree.jira.model;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Issue keys created from a calendar plan, stored as {@code yyyy-mm-dd=KEY} pairs.
 * The raised-day list stays dates only, so a key never looks like a handed-over day.
 */
public final class CalendarIssues {
    private static final int KEEP = 400;

    private CalendarIssues() {
    }

    /** Remembers the request created for one series day and drops days more than 400 days behind {@code today}. */
    public static String remember(String stored, String source, String issueKey, LocalDate today) {
        String day = day(source);
        String key = clean(issueKey);
        Map<String, String> pairs = pairs(stored, today);
        if (day.isEmpty() || key.isEmpty()) {
            return join(pairs);
        }
        pairs.remove(day);
        pairs.put(day, key);
        while (pairs.size() > KEEP) {
            pairs.remove(pairs.keySet().iterator().next());
        }
        return join(pairs);
    }

    /** The request key stored for this series day, or an empty string. */
    public static String keyOn(String stored, String source) {
        String day = day(source);
        if (day.isEmpty()) {
            return "";
        }
        String key = pairs(stored, null).get(day);
        return key == null ? "" : key;
    }

    private static Map<String, String> pairs(String stored, LocalDate today) {
        Map<String, String> kept = new LinkedHashMap<String, String>();
        if (stored == null || stored.isEmpty()) {
            return kept;
        }
        String cutoff = today == null ? "" : today.minusDays(KEEP).toString();
        String[] parts = stored.split(",");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i] == null ? "" : parts[i].trim();
            int split = part.indexOf('=');
            if (split <= 0) {
                continue;
            }
            String day = day(part.substring(0, split));
            String key = clean(part.substring(split + 1));
            if (day.isEmpty() || key.isEmpty()) {
                continue;
            }
            if (!cutoff.isEmpty() && day.compareTo(cutoff) < 0) {
                continue;
            }
            kept.put(day, key);
        }
        return kept;
    }

    private static String join(Map<String, String> pairs) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> entry : pairs.entrySet()) {
            if (out.length() > 0) {
                out.append(',');
            }
            out.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return out.toString();
    }

    private static String day(String raw) {
        String value = FieldDates.canonical(raw);
        return value == null ? "" : value;
    }

    private static String clean(String issueKey) {
        String key = AssetValidator.normalizeIssueKey(issueKey == null ? "" : issueKey.trim());
        return AssetValidator.isIssueKey(key) ? key : "";
    }
}
