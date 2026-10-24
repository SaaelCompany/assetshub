package com.assetstree.jira.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Finds the nearest location that should keep a child add, move, or delete.
 * Nodes in the skip set are being removed, so the log lands on the place that remains.
 */
public final class PlaceHistory {
    private PlaceHistory() {
    }

    public static Integer container(Integer startId, Map<Integer, Boolean> locationById,
                                    Map<Integer, Integer> parents, Set<Integer> skip) {
        Integer current = startId;
        Set<Integer> seen = new HashSet<Integer>();
        while (current != null && seen.add(current)) {
            if (skip != null && skip.contains(current)) {
                current = parents == null ? null : parents.get(current);
                continue;
            }
            Boolean location = locationById == null ? null : locationById.get(current);
            if (location != null && location.booleanValue()) {
                return current;
            }
            current = parents == null ? null : parents.get(current);
        }
        return null;
    }
}
