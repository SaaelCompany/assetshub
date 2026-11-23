package com.assetstree.jira.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PortalAssetMarkTest {
    @Test
    public void readsTheAssetFromThePortalCall() {
        assertEquals(Integer.valueOf(9), PortalAssetMark.parseId("9"));
        assertEquals(Integer.valueOf(12), PortalAssetMark.parseId("%31%32"));
        assertNull(PortalAssetMark.parseId("9%2C1"));
        assertNull(PortalAssetMark.parseId("0"));
        assertNull(PortalAssetMark.parseId("SD-14"));
        assertEquals(Integer.valueOf(9), PortalAssetMark.fromQuery("project=STP&asset=9"));
        assertEquals(Integer.valueOf(9), PortalAssetMark.fromReferer(
                "https://jira.example/servicedesk/customer/portal/1?asset=9&project=STP"));
        assertNull(PortalAssetMark.fromReferer("https://jira.example/servicedesk/customer/portal/1/create/5"));
    }

    @Test
    public void pendingAssetExpiresAndANewRequestCountsAsFresh() {
        long now = 1_700_000_000_000L;
        String stored = PortalAssetMark.pendingValue(9, now);
        assertEquals(Integer.valueOf(9), PortalAssetMark.pendingId(stored, now + 1000L));
        assertNull(PortalAssetMark.pendingId(stored, now + PortalAssetMark.KEPT_MS + 1L));
        assertTrue(PortalAssetMark.fresh(null, now));
        assertTrue(PortalAssetMark.fresh(Long.valueOf(now + 30_000L), now));
        assertFalse(PortalAssetMark.fresh(Long.valueOf(now - PortalAssetMark.KEPT_MS - 1L), now));
    }

    @Test
    public void requestAssetStaysVisibleUntilTheOuterFilterReturns() {
        try {
            PortalAssetHold.enter();
            PortalAssetHold.bindRequest(9);
            PortalAssetHold.enter();
            PortalAssetHold.leave();
            assertEquals(Integer.valueOf(9), PortalAssetHold.requestAsset());
            PortalAssetHold.leave();
            assertNull(PortalAssetHold.requestAsset());
        } finally {
            PortalAssetHold.leave();
            PortalAssetHold.leave();
        }
    }
}
