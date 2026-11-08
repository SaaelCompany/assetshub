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
    }

    @Test
    public void axesOfACompletedReport() {
        assertEquals(Arrays.asList("kind"), ScheduleDone.axes("done"));
        assertEquals(Arrays.asList("month", "who"), ScheduleDone.axes("done+month+who"));
        assertEquals(Arrays.asList(), ScheduleDone.axes("kind"));
    }
}
