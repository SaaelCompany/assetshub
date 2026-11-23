package com.assetstree.jira.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PlanUnderlayTest {
    @Test
    public void jpegScanRoundTripsAndSvgDoesNot() {
        String src = "data:image/jpeg;base64,abc";
        String stored = PlanUnderlay.store(src, 10, 20, 400, 300);
        PlanUnderlay.Sheet sheet = PlanUnderlay.read(stored);
        assertEquals(src, sheet.src);
        assertEquals(10, sheet.x);
        assertEquals(400, sheet.w);
        assertEquals("", PlanUnderlay.store("data:image/svg+xml;base64,abc", 0, 0, 100, 100));
        assertTrue(PlanUnderlay.store(src, 0, 0, 4, 100).isEmpty());
    }
}
