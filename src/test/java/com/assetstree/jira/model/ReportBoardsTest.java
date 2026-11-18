package com.assetstree.jira.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReportBoardsTest {
    @Test
    public void aBoardKeepsReportChartAndWidth() {
        String stored = ReportBoards.write(Arrays.asList(
                new ReportBoards.Panel("status", "donut", "full"),
                new ReportBoards.Panel("s4", "bars", "half"),
                new ReportBoards.Panel("requests", "line", "half")));
        List<ReportBoards.Panel> panels = ReportBoards.read(stored);
        assertEquals(3, panels.size());
        assertEquals("status", panels.get(0).getReport());
        assertEquals("donut", panels.get(0).getChart());
        assertEquals("full", panels.get(0).getWidth());
        assertEquals("s4", panels.get(1).getReport());
        assertEquals("half", panels.get(2).getWidth());
    }

    @Test
    public void aBrokenPanelIsDropped() {
        List<ReportBoards.Panel> panels = ReportBoards.read("nope|donut|full,status|columns|wide,idle|bars|half");
        assertEquals(2, panels.size());
        assertEquals("histogram", panels.get(0).getChart());
        assertEquals("half", panels.get(0).getWidth());
        assertEquals("idle", panels.get(1).getReport());
    }

    @Test
    public void theTitleAndTheCountStayBounded() {
        assertEquals("", ReportBoards.name("  "));
        assertEquals("Эксплуатация", ReportBoards.name(" Эксплуатация "));
        assertTrue(ReportBoards.name(repeat('a', 81)).isEmpty());
        StringBuilder raw = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            if (raw.length() > 0) {
                raw.append(',');
            }
            raw.append("status|donut|half");
        }
        assertEquals(ReportBoards.MAX_PANELS, ReportBoards.read(raw.toString()).size());
    }

    private static String repeat(char ch, int count) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < count; i++) {
            out.append(ch);
        }
        return out.toString();
    }
}
