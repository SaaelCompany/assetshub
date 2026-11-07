package com.assetstree.jira.model;

import com.assetstree.jira.service.TniBook;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TniAddressTest {
    @Test
    public void acceptsOnlyAPrivateAddress() {
        assertEquals("192.168.1.10", TniAddress.local(" 192.168.1.10 "));
        assertEquals("10.1.2.3", TniAddress.local("10.1.2.3"));
        assertEquals("172.16.5.5", TniAddress.local("172.16.5.5"));
        assertEquals("", TniAddress.local("8.8.8.8"));
        assertEquals("", TniAddress.local("127.0.0.1"));
        assertEquals("", TniAddress.local("pc.corp.local"));
    }

    @Test
    public void waitsFifteenMinutesBetweenChecks() {
        long now = 1_700_000_000_000L;
        assertTrue(TniBook.watchDue(0L, now));
        assertFalse(TniBook.watchDue(now - 60_000L, now));
        assertTrue(TniBook.watchDue(now - TniClock.MIN_GAP, now));
    }
}
