package com.assetstree.jira.model;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SchedulePlaceScopeTest {
    @Test
    public void parentScopeIncludesNestedPlacesAndEquipmentButNotSiblingBranch() {
        Map<Integer, Integer> parents = new HashMap<Integer, Integer>();
        parents.put(1, null); // branch A
        parents.put(2, 1);    // department
        parents.put(3, 2);    // cabinet
        parents.put(4, 3);    // equipment
        parents.put(5, null); // branch B
        parents.put(6, 5);    // equipment

        assertTrue(inside(parents, 1, 1));
        assertTrue(inside(parents, 2, 1));
        assertTrue(inside(parents, 3, 1));
        assertTrue(inside(parents, 4, 1));
        assertFalse(inside(parents, 5, 1));
        assertFalse(inside(parents, 6, 1));
        assertTrue(inside(parents, 4, 3));
        assertFalse(inside(parents, 2, 3));
    }

    @Test
    public void missingParentStopsTraversalAndProjectWideScopeIsUnfiltered() {
        Map<Integer, Integer> parents = new HashMap<Integer, Integer>();
        parents.put(7, 99);
        assertFalse(inside(parents, 7, 100));
        assertTrue(visible(7, parents, 0));
    }

    private boolean visible(int assetId, Map<Integer, Integer> parents, int placeId) {
        return placeId <= 0 || inside(parents, assetId, placeId);
    }

    private boolean inside(Map<Integer, Integer> parents, int assetId, int placeId) {
        Integer cursor = Integer.valueOf(assetId);
        for (int guard = 0; cursor != null && guard < 80; guard++) {
            if (cursor.intValue() == placeId) return true;
            cursor = parents.get(cursor);
        }
        return false;
    }
}
