package com.assetstree.jira.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Place ids stored on an access grant. Empty means no extra places. */
public final class PlaceScope {
    private static final int LIMIT = 40;

    private PlaceScope() {
    }

    /** Keeps ids that belong to the project's places, in the order they were listed. */
    public static String keep(String raw, Set<Integer> places) {
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (Integer id : read(raw)) {
            if (places != null && !places.contains(id)) {
                continue;
            }
            if (count >= LIMIT) {
                break;
            }
            if (out.length() > 0) {
                out.append(',');
            }
            out.append(id.intValue());
            count++;
        }
        return out.toString();
    }

    public static Set<Integer> read(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<Integer> ids = new LinkedHashSet<Integer>();
        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length && ids.size() < LIMIT; i++) {
            String part = parts[i] == null ? "" : parts[i].trim();
            if (part.isEmpty()) {
                continue;
            }
            try {
                int id = Integer.parseInt(part);
                if (id > 0) {
                    ids.add(Integer.valueOf(id));
                }
            } catch (NumberFormatException ignored) {
                continue;
            }
        }
        return ids;
    }
}
