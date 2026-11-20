package com.assetstree.jira.model;

import java.util.Map;
import java.util.Set;

/**
 * A person may change a card when they are its custodian, or when the card
 * sits on a place assigned to them. A place is assigned when they are its
 * custodian or its write-off approver. Several grants combine: one wide grant
 * covers the whole project.
 */
public final class OwnReach {
    private OwnReach() {
    }

    public static boolean covers(String userKey, int assetId, Map<Integer, Integer> parents,
                                 Map<Integer, String> custodians, Set<Integer> heldPlaces) {
        if (userKey == null || userKey.isEmpty() || assetId <= 0) {
            return false;
        }
        String mine = custodians == null ? null : custodians.get(Integer.valueOf(assetId));
        if (userKey.equals(mine)) {
            return true;
        }
        Integer current = Integer.valueOf(assetId);
        int guard = 0;
        while (current != null && guard++ < 48) {
            if (heldPlaces != null && heldPlaces.contains(current)) {
                return true;
            }
            Integer next = parents == null ? null : parents.get(current);
            if (next == null || next.equals(current)) {
                break;
            }
            current = next;
        }
        return false;
    }
}
