package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.List;

/** Reports and boards a project shows on the portal profile, and who may see them. */
public final class PortalShowcase {
    public static final int CHART_LIMIT = 6;
    public static final int SLICE_LIMIT = 12;

    private PortalShowcase() {
    }

    public static String reports(String raw) {
        return tokens(raw, 8, 32, false);
    }

    public static String boards(String raw) {
        return tokens(raw, 6, 12, true);
    }

    public static String people(String raw) {
        StringBuilder stored = new StringBuilder();
        if (raw == null) {
            return "";
        }
        List<String> seen = new ArrayList<String>();
        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length; i++) {
            String key = parts[i] == null ? "" : parts[i].trim();
            if (key.isEmpty() || key.length() > 80 || !key.matches("[A-Za-z0-9._@:-]+") || seen.contains(key)) {
                continue;
            }
            if (seen.size() >= 40) {
                break;
            }
            seen.add(key);
            if (stored.length() > 0) {
                stored.append(',');
            }
            stored.append(key);
        }
        return stored.toString();
    }

    public static boolean sees(String stored, String userKey) {
        if (userKey == null || userKey.trim().isEmpty()) {
            return false;
        }
        String wanted = userKey.trim();
        String[] parts = people(stored).split(",");
        for (int i = 0; i < parts.length; i++) {
            if (wanted.equals(parts[i])) {
                return true;
            }
        }
        return false;
    }

    public static List<String> list(String stored) {
        List<String> found = new ArrayList<String>();
        if (stored == null || stored.trim().isEmpty()) {
            return found;
        }
        String[] parts = stored.split(",");
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                found.add(parts[i]);
            }
        }
        return found;
    }

    private static String tokens(String raw, int limit, int keyLimit, boolean digits) {
        StringBuilder stored = new StringBuilder();
        if (raw == null) {
            return "";
        }
        List<String> seen = new ArrayList<String>();
        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length; i++) {
            String key = parts[i] == null ? "" : parts[i].trim().toLowerCase();
            if (key.isEmpty() || key.length() > keyLimit || seen.contains(key)) {
                continue;
            }
            if (digits) {
                if (!key.matches("[0-9]{1,12}")) {
                    continue;
                }
            } else if (!key.matches("[a-z0-9+]{1,32}")) {
                continue;
            }
            if (seen.size() >= limit) {
                break;
            }
            seen.add(key);
            if (stored.length() > 0) {
                stored.append(',');
            }
            stored.append(key);
        }
        return stored.toString();
    }
}
