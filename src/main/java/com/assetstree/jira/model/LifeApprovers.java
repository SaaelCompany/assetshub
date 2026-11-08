package com.assetstree.jira.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Write-off approvers stored as {@code placeId:userKey,placeId:userKey}.
 * The nearest place on the way up from the asset wins. A project-wide
 * approver is used only when the asset sits on no place at all.
 */
public final class LifeApprovers {
    private static final int MAX = 80;

    private LifeApprovers() {
    }

    public static String normalize(String raw) {
        Map<Integer, String> places = parse(raw);
        StringBuilder stored = new StringBuilder();
        for (Map.Entry<Integer, String> entry : places.entrySet()) {
            if (stored.length() > 0) {
                stored.append(',');
            }
            stored.append(entry.getKey().intValue()).append(':').append(entry.getValue());
        }
        return stored.toString();
    }

    /**
     * @param chain place ids from the asset's own location up to the root
     */
    public static String resolve(String places, String fallback, int[] chain) {
        Map<Integer, String> map = parse(places);
        if (chain != null && chain.length > 0) {
            for (int id : chain) {
                String who = map.get(Integer.valueOf(id));
                if (who != null && !who.isEmpty()) {
                    return who;
                }
            }
            return "";
        }
        return fallback == null ? "" : fallback.trim();
    }

    private static Map<Integer, String> parse(String raw) {
        Map<Integer, String> places = new LinkedHashMap<Integer, String>();
        if (raw == null || raw.trim().isEmpty()) {
            return places;
        }
        String[] parts = raw.split(",");
        for (String part : parts) {
            if (places.size() >= MAX || part == null) {
                continue;
            }
            int colon = part.indexOf(':');
            if (colon <= 0 || colon >= part.length() - 1) {
                continue;
            }
            String idText = part.substring(0, colon).trim();
            String user = part.substring(colon + 1).trim();
            int id;
            try {
                id = Integer.parseInt(idText);
            } catch (NumberFormatException ex) {
                continue;
            }
            if (id <= 0 || !user.matches("[A-Za-z0-9._@-]{1,80}")) {
                continue;
            }
            places.put(Integer.valueOf(id), user);
        }
        return places;
    }
}
