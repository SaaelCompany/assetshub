package com.assetstree.jira.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RequestStatusesTest {
    @Test
    public void emptyCountsEveryStatusAndOpenSkipsDone() {
        assertEquals("", RequestStatuses.store(null));
        assertEquals("", RequestStatuses.store("  "));
        assertTrue(RequestStatuses.counts("", "10001", RequestStatuses.DONE));
        assertTrue(RequestStatuses.counts("open", "10001", "indeterminate"));
        assertTrue(RequestStatuses.counts("open", "10001", null));
        assertFalse(RequestStatuses.counts("OPEN", "10001", "Done"));
        assertEquals(RequestStatuses.NONE, RequestStatuses.store("-"));
        assertFalse(RequestStatuses.counts("-", "10001", "new"));
    }

    @Test
    public void aListKeepsNumericIds() {
        assertEquals("1,10001", RequestStatuses.store(" 1, open, 10001, abc, 1 "));
        assertTrue(RequestStatuses.counts("1,10001", "10001", RequestStatuses.DONE));
        assertFalse(RequestStatuses.counts("1,10001", "3", "new"));
        assertFalse(RequestStatuses.counts("1,10001", "", "new"));
    }
}
