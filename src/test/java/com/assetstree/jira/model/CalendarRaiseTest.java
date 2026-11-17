package com.assetstree.jira.model;

import org.junit.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CalendarRaiseTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 17);

    @Test
    public void maintenanceOnThatDayIsReadyAfterItsStart() {
        List<CalendarRaise.Hand> early = CalendarRaise.due("2026-10-17", 1, "month", "", "09:00", "11:00",
                "", false, "", TODAY, "08:45");
        assertTrue(early.isEmpty());
        List<CalendarRaise.Hand> due = CalendarRaise.due("2026-10-17", 1, "month", "", "09:00", "11:00",
                "", false, "", TODAY, "09:00");
        assertEquals(1, due.size());
        assertEquals("2026-11-17", due.get(0).getSource());
        assertEquals("2026-11-17", due.get(0).getDate());
        assertEquals("09:00", due.get(0).getStart());
        assertEquals("11:00", due.get(0).getEnd());
    }

    @Test
    public void theSameDayIsHandedOverOnce() {
        String raised = CalendarRaise.remember("", "2026-11-17", TODAY);
        List<CalendarRaise.Hand> again = CalendarRaise.due("2026-10-17", 1, "month", "", "", "",
                "", false, raised, TODAY, "12:00");
        assertTrue(again.isEmpty());
        assertEquals(raised, CalendarRaise.remember(raised, "2026-11-17", TODAY));
    }

    @Test
    public void aFinishedDayStaysOffTheRule() {
        List<CalendarRaise.Hand> done = CalendarRaise.due("2026-11-17", 1, "once", "2026-11-17>done", "", "",
                "2026-11-17", true, "", TODAY, "");
        assertTrue(done.isEmpty());
        List<CalendarRaise.Hand> marked = CalendarRaise.due("2026-10-17", 1, "month", "2026-11-17>done", "", "",
                "", false, "", TODAY, "");
        assertTrue(marked.isEmpty());
    }

    @Test
    public void aMovedDayFollowsTheCalendar() {
        List<CalendarRaise.Hand> home = CalendarRaise.due("2026-11-17", 1, "once", "2026-11-17>2026-11-18", "", "",
                "", false, "", TODAY, "");
        assertTrue(home.isEmpty());
        List<CalendarRaise.Hand> moved = CalendarRaise.due("2026-11-17", 1, "once", "2026-11-17>2026-11-18@10:00-12:00",
                "08:00", "09:00", "", false, "", LocalDate.of(2026, 11, 18), "10:30");
        assertEquals(1, moved.size());
        assertEquals("2026-11-17", moved.get(0).getSource());
        assertEquals("2026-11-18", moved.get(0).getDate());
        assertEquals("10:00", moved.get(0).getStart());
    }

    @Test
    public void yesterdayIsPickedUpOnTheNextRun() {
        List<CalendarRaise.Hand> due = CalendarRaise.due("2026-11-16", 1, "once", "", "18:00", "",
                "", false, "", TODAY, "08:00");
        assertEquals(1, due.size());
        assertEquals("2026-11-16", due.get(0).getDate());
    }

    @Test
    public void missedDaysSinceTheLastHandoverAreRaised() {
        List<CalendarRaise.Hand> due = CalendarRaise.due("2026-11-13", 1, "day", "", "", "",
                "", false, "2026-11-14", TODAY, "");
        assertEquals(3, due.size());
        assertEquals("2026-11-15", due.get(0).getDate());
        assertEquals("2026-11-16", due.get(1).getDate());
        assertEquals("2026-11-17", due.get(2).getDate());
    }

    @Test
    public void anUntouchedHistoryStaysAtYesterday() {
        List<CalendarRaise.Hand> due = CalendarRaise.due("2026-01-01", 1, "once", "", "", "",
                "", false, "", TODAY, "");
        assertTrue(due.isEmpty());
    }

    @Test
    public void aFutureDayWaits() {
        List<CalendarRaise.Hand> due = CalendarRaise.due("2026-11-20", 1, "once", "", "", "",
                "", false, "", TODAY, "23:00");
        assertTrue(due.isEmpty());
    }

    @Test
    public void rememberDropsDaysFarBehind() {
        String raised = CalendarRaise.remember("2024-01-01,2026-11-16", "2026-11-17", TODAY);
        assertEquals("2026-11-16,2026-11-17", raised);
    }
}
