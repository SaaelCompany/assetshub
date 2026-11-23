package com.assetstree.jira.model;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FaultReportTest {
    @Test
    public void windowDefaultsToTheMonthAndKeepsAShortRange() {
        ReportWindow window = ReportWindow.of("", "", LocalDate.of(2026, 11, 17), ZoneOffset.UTC);
        assertEquals(LocalDate.of(2026, 11, 1), window.getStart());
        assertEquals(LocalDate.of(2026, 11, 17), window.getEnd());
        ReportWindow swapped = ReportWindow.of("2026-11-10", "2026-11-02", LocalDate.of(2026, 11, 17), ZoneOffset.UTC);
        assertEquals(LocalDate.of(2026, 11, 2), swapped.getStart());
        assertEquals(LocalDate.of(2026, 11, 10), swapped.getEnd());
        assertTrue(swapped.containsMillis(swapped.getStartMillis()));
        assertTrue(!swapped.containsMillis(swapped.getEndMillis()));
    }

    @Test
    public void repairStayCountsOnlyTheOverlap() {
        long day = RepairTime.DAY;
        List<RepairTime.Change> changes = Arrays.asList(
                new RepairTime.Change(day, Statuses.REPAIR),
                new RepairTime.Change(day * 4, Statuses.IN_USE));
        List<RepairTime.Span> spans = RepairTime.spans(changes, false, day * 2, day * 5);
        assertEquals(1, spans.size());
        assertEquals(day * 2, RepairTime.overlap(spans));
        assertEquals(2, RepairTime.days(RepairTime.overlap(spans)));
        List<RepairTime.Span> open = RepairTime.spans(Collections.singletonList(
                new RepairTime.Change(0L, Statuses.REPAIR)), true, day, day * 3);
        assertEquals(day * 2, RepairTime.overlap(open));
        assertEquals(1, RepairTime.spans(Collections.<RepairTime.Change>emptyList(), true, 0L, day).size());
        assertEquals(0, RepairTime.spans(Collections.<RepairTime.Change>emptyList(), false, 0L, day).size());
    }

    @Test
    public void maintenanceRollsUpTheUnitAndKeepsMissedDays() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 4, 15);
        LocalDate today = LocalDate.of(2026, 3, 20);
        String moves = ServiceDue.rememberDone("", "2026-02-01", "2026-02-01");
        List<PtoWindow.Item> items = PtoWindow.items(moves, "2026-01-01", 1, ServiceDue.MONTH, "", false,
                "ПТО", start, end, today);
        assertEquals(3, items.size());
        assertEquals(ScheduleDone.DONE, items.get(0).getState());
        assertEquals("2026-02-01", items.get(0).getDay());
        assertEquals(ScheduleDone.OVERDUE, items.get(1).getState());
        assertEquals("2026-03-01", items.get(1).getDay());
        assertEquals(ScheduleDone.EXPECTED, items.get(2).getState());
        assertEquals(PtoWindow.LATE, PtoWindow.rollup(items));
        assertEquals(PtoWindow.ONTIME, PtoWindow.rollup(Collections.singletonList(items.get(0))));
    }
}
