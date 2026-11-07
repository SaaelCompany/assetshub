package com.assetstree.jira.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FloorBoundsTest {
    @Test
    public void diagramShapesStayOnThePlan() {
        assertEquals("box", FloorBounds.kind("box"));
        assertEquals("ellipse", FloorBounds.kind("Ellipse"));
        assertEquals("flow", FloorBounds.kind("flow"));
        assertEquals("chart", FloorBounds.kind("chart"));
        assertEquals("room", FloorBounds.kind("waffle"));
        assertEquals("pen", FloorBounds.kind("pen"));
        assertEquals("rack", FloorBounds.kind("rack"));
        assertEquals("switch", FloorBounds.kind("switch"));
        assertEquals(8, FloorBounds.minSize("diamond"));
        assertEquals(8, FloorBounds.minSize("rack"));
        assertEquals(4, FloorBounds.minSize("flow"));
        assertEquals(4, FloorBounds.minSize("pen"));
    }
}
