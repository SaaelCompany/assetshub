package com.assetstree.jira.model;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class PlaceMolTest {
    @Test
    public void nearestPlaceWithAPersonWins() {
        PlaceMol.Node branch = new PlaceMol.Node(1, null, true, "ivanova");
        PlaceMol.Node room = new PlaceMol.Node(2, Integer.valueOf(1), true, "");
        PlaceMol.Node cabinet = new PlaceMol.Node(3, Integer.valueOf(2), true, "petrov");
        PlaceMol.Node computer = new PlaceMol.Node(4, Integer.valueOf(3), false, "other");
        assertEquals("petrov", PlaceMol.of(Arrays.asList(branch, room, cabinet, computer), Integer.valueOf(4)));
        assertEquals("petrov", PlaceMol.of(Arrays.asList(branch, room, cabinet), Integer.valueOf(3)));
        assertEquals("ivanova", PlaceMol.of(Arrays.asList(branch, room), Integer.valueOf(2)));
        assertEquals("", PlaceMol.of(Arrays.asList(new PlaceMol.Node(1, null, true, "  ")), Integer.valueOf(1)));
        assertEquals("", PlaceMol.of(Arrays.asList(branch), null));
    }
}
