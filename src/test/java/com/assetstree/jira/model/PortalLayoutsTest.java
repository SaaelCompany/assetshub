package com.assetstree.jira.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PortalLayoutsTest {
    @Test
    public void onlyTheThreeLayoutsAreKept() {
        assertEquals(PortalLayouts.TILES, PortalLayouts.canonical(null));
        assertEquals(PortalLayouts.TILES, PortalLayouts.canonical("  "));
        assertEquals(PortalLayouts.TILES, PortalLayouts.canonical("grid"));
        assertEquals(PortalLayouts.TILES, PortalLayouts.canonical(" Tiles "));
        assertEquals(PortalLayouts.CARDS, PortalLayouts.canonical("Cards"));
        assertEquals(PortalLayouts.LIST, PortalLayouts.canonical("list"));
        assertTrue(PortalLayouts.known("tiles"));
        assertTrue(PortalLayouts.known("CARDS"));
        assertFalse(PortalLayouts.known("grid"));
        assertFalse(PortalLayouts.known(null));
    }
}
