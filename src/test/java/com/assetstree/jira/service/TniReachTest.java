package com.assetstree.jira.service;

import org.junit.Test;

import java.net.InetAddress;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TniReachTest {
    @Test
    public void shortHostIsPingedWhenJavaDoesNotKnowIt() throws Exception {
        assertTrue(TniReach.officeName(null));
        assertTrue(TniReach.officeName(new InetAddress[0]));
        assertTrue(TniReach.officeName(new InetAddress[]{InetAddress.getByName("10.8.0.4")}));
        assertFalse(TniReach.officeName(new InetAddress[]{InetAddress.getByName("8.8.8.8")}));
    }
}
