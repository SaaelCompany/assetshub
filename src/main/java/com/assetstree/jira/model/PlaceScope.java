package com.assetstree.jira.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Place ids stored on an access grant. Empty means no extra places. A star means every place. */
public final class PlaceScope {
    public static final String ALL = "*";
    private static final int LIMIT = 40;

    private PlaceScope() {
    }

    /** True when the grant covers every place, including places created later. */
    public static boolean all(String raw) {
        if (raw == null || raw.isEmpty()) {
            return false;
        }
        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i] == null ? "" : parts[i].trim();
            if (ALL.equals(part)) {
                return true;
            }
        }
        return false;
    }

    /** Keeps ids that belong to the project's places, in the order they were listed. */
    public static String keep(String raw, Set<Integer> places) {
        if (all(raw)) {
            return ALL;
        }
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
