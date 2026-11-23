package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.List;

/** Names, people and places of a clinic access group. */
public final class ClinicCircles {
    public static final int CIRCLE_LIMIT = 40;
    public static final int MEMBER_LIMIT = 500;
    public static final int PLACE_LIMIT = 30;

    private ClinicCircles() {
    }

    public static String name(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim().replaceAll("\\s+", " ");
        if (text.length() > 80) {
            text = text.substring(0, 80).trim();
        }
        return text;
    }

    public static List<String> members(List<String> raw) {
        List<String> kept = new ArrayList<String>();
        if (raw == null) {
            return kept;
        }
        for (String value : raw) {
            String key = value == null ? "" : value.trim();
            if (key.isEmpty() || key.length() > 80 || !key.matches("[A-Za-z0-9._@:-]+") || kept.contains(key)) {
                continue;
            }
            kept.add(key);
            if (kept.size() >= MEMBER_LIMIT) {
                break;
            }
        }
        return kept;
    }

    public static List<Integer> places(List<Integer> raw) {
        List<Integer> kept = new ArrayList<Integer>();
        if (raw == null) {
            return kept;
        }
        for (Integer value : raw) {
            if (value == null || value.intValue() <= 0 || kept.contains(value)) {
                continue;
            }
            kept.add(value);
            if (kept.size() >= PLACE_LIMIT) {
                break;
            }
        }
        return kept;
    }
}
