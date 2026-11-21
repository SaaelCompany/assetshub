package com.assetstree.jira.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PlaceScopeTest {
    @Test
    public void keepsOnlyKnownPlacesAndDropsRepeats() {
        HashSet<Integer> places = new HashSet<Integer>(Arrays.asList(Integer.valueOf(4), Integer.valueOf(9)));
        assertEquals("4,9", PlaceScope.keep(" 4, no, 4, 9, 2 ", places));
        assertEquals("", PlaceScope.keep("", places));
        assertEquals("", PlaceScope.keep("3", Collections.<Integer>emptySet()));
        assertTrue(PlaceScope.read("4,9").contains(Integer.valueOf(9)));
        assertTrue(PlaceScope.read(null).isEmpty());
    }
}
