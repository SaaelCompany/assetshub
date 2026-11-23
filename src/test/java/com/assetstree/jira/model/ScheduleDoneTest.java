package com.assetstree.jira.model;

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ScheduleDoneTest {
    @Test
    public void finishedDaysFollowTheMarksTheCalendarKeeps() {
        assertEquals(Arrays.asList("2026-01-05", "2026-03-01"),
                ScheduleDone.days("2026-03-01>done,2026-02-01>2026-02-03,2026-01-05>done",
                        "2026-03-01", "month", "2026-03-02", false));
        assertEquals(Arrays.asList("2026-01-05", "2026-04-01"),
                ScheduleDone.days("2026-01-05>done", "2026-04-01", "month", "2026-04-01", false));
        assertEquals(Arrays.asList("2026-05-01"),
                ScheduleDone.days("", "2026-05-01", "once", "2026-05-02", true));
        assertEquals(Arrays.asList(),
                ScheduleDone.days("", "2026-05-01", "once", "", false));
        assertEquals(Arrays.asList(),
                ScheduleDone.days("2026-06-01>2026-06-02", "2026-06-01", "month", "", false));
        assertEquals(Arrays.asList("2026-10-01"),
                ScheduleDone.days("2026-10-01>done@2026-11-08", "", "month", "", false));
        assertEquals(ScheduleDone.LATE, ScheduleDone.marks("2026-10-01>done@2026-11-08", "", "month", "", false).get(0).getState());
        assertEquals(ScheduleDone.ONTIME, ScheduleDone.marks("2026-10-01>done", "2026-10-01", "month", "2026-10-01", false).get(0).getState());
        assertEquals(ScheduleDone.DONE, ScheduleDone.marks("2026-10-01>done", "2026-10-01", "month", "", false).get(0).getState());
        assertEquals("2026-12-01", ScheduleDone.expectedAfter("2026-08-01", 1, "month", java.time.LocalDate.parse("2026-11-08")));
        assertEquals("", ScheduleDone.expectedAfter("2026-08-15", 1, "once", java.time.LocalDate.parse("2026-11-08")));
        String late = ServiceDue.rememberDone("2026-09-01>done", "2026-10-01", "2026-11-08");
        assertEquals("2026-09-01>done,2026-10-01>done@2026-11-08", late);
    }

    @Test
    public void axesOfACompletedReport() {
        assertEquals(Arrays.asList("kind"), ScheduleDone.axes("done"));
        assertEquals(Arrays.asList("month", "who"), ScheduleDone.axes("done+month+who"));
        assertEquals(Arrays.asList(), ScheduleDone.axes("kind"));
    }
}
