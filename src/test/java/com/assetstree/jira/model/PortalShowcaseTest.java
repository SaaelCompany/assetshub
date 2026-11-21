package com.assetstree.jira.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PortalShowcaseTest {
    @Test
    public void manyNamesLeaveTheColumns() {
        assertTrue(ChartLayout.columns(8));
        assertFalse(ChartLayout.columns(9));
        assertTrue(ChartLayout.ring("donut", 6));
        assertFalse(ChartLayout.ring("donut", 9));
        assertFalse(ChartLayout.ring("histogram", 4));
    }

    @Test
    public void onlyListedPeopleSeeTheProfileReports() {
        assertEquals("status,s12", PortalShowcase.reports("status, s12, nope!, s12"));
        assertEquals("4,9", PortalShowcase.boards("4, board, 9"));
        assertEquals("JIRAUSER10000", PortalShowcase.people("JIRAUSER10000, "));
        assertTrue(PortalShowcase.sees("JIRAUSER10000,JIRAUSER10001", "JIRAUSER10001"));
        assertFalse(PortalShowcase.sees("", "JIRAUSER10000"));
        assertFalse(PortalShowcase.sees("JIRAUSER10000", "JIRAUSER10001"));
    }
}
