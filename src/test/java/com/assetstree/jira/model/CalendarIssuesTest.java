package com.assetstree.jira.model;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

public class CalendarIssuesTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 17);

    @Test
    public void aRequestKeyStaysOnItsDay() {
        String stored = CalendarIssues.remember("", "2026-11-17", "sd-14", TODAY);
        assertEquals("2026-11-17=SD-14", stored);
        assertEquals("SD-14", CalendarIssues.keyOn(stored, "2026-11-17"));
        String replaced = CalendarIssues.remember(stored, "2026-11-17", "SD-20", TODAY);
        assertEquals("2026-11-17=SD-20", replaced);
        assertEquals("", CalendarIssues.keyOn(replaced, "2026-11-16"));
    }

    @Test
    public void aBrokenTokenIsDropped() {
        String stored = CalendarIssues.remember("not-a-day=SD-1,2026-11-16=hello", "2026-11-17", "SD-2", TODAY);
        assertEquals("2026-11-17=SD-2", stored);
    }

    @Test
    public void oldDaysLeaveTheDay() {
        String stored = CalendarIssues.remember("2024-01-01=SD-1,2026-11-16=SD-9", "2026-11-17", "SD-10", TODAY);
        assertEquals("2026-11-16=SD-9,2026-11-17=SD-10", stored);
    }
}
