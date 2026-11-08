package com.assetstree.jira.model;

import com.assetstree.jira.service.TniReach;
import com.assetstree.jira.service.TniScan;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TniRangeTest {
    @Test
    public void cidrSkipsNetworkAndBroadcast() {
        List<String> hosts = TniRange.expand("192.168.10.0/24");
        assertEquals(254, hosts.size());
        assertEquals("192.168.10.1", hosts.get(0));
        assertEquals("192.168.10.254", hosts.get(253));
    }

    @Test
    public void rangeListAndSpacesAreOneTask() {
        List<String> hosts = TniRange.expand("192.168.1.10-192.168.1.12, 10.0.0.5\n10.1.1.1 10.1.1.2");
        assertEquals(Arrays.asList("192.168.1.10", "192.168.1.11", "192.168.1.12", "10.0.0.5", "10.1.1.1", "10.1.1.2"), hosts);
    }

    @Test
    public void pointToPointAndSingleHostStay() {
        assertEquals(Arrays.asList("10.2.2.1", "10.2.2.2"), TniRange.expand("10.2.2.0/30"));
        assertEquals(Arrays.asList("172.16.5.5"), TniRange.expand("172.16.5.5/32"));
    }

    @Test
    public void publicLoopbackAndHugeNetworksAreRefused() {
        assertKey(TniRange.PUBLIC, "8.8.8.8");
        assertKey(TniRange.PUBLIC, "1.2.3.0/24");
        assertKey(TniRange.BAD, "127.0.0.1");
        assertKey(TniRange.BAD, "localhost");
        assertKey(TniRange.BAD, "");
        assertKey(TniRange.LARGE, "10.0.0.0/16");
        assertKey(TniRange.LARGE, "192.168.0.1-192.168.8.1");
    }

    @Test
    public void scanKeepsOnlyHostsThatAnswer() {
        final Set<String> created = new HashSet<String>();
        final int[] steps = new int[] {0};
        TniScan.run(TniRange.expand("10.9.0.1-10.9.0.4"), new TniReach.Probe() {
            @Override
            public boolean up(String address) {
                return "10.9.0.2".equals(address) || "10.9.0.4".equals(address);
            }
        }, TniScan.NONE, new TniScan.Sink() {
            @Override
            public boolean keep(String address, String name) {
                if ("10.9.0.2".equals(address)) {
                    created.add(address);
                    return true;
                }
                return false;
            }
        }, new TniScan.Tick() {
            @Override
            public void step(int scanned, int total, int online, int added, String log) {
                steps[0] = scanned;
                if (scanned == total) {
                    assertEquals(4, total);
                    assertEquals(2, online);
                    assertEquals(1, added);
                    assertTrue(log.contains("10.9.0.2\t\tnew"));
                    assertTrue(log.contains("10.9.0.4\t\tknown"));
                }
            }
        });
        assertEquals(4, steps[0]);
        assertEquals(new HashSet<String>(Arrays.asList("10.9.0.2")), created);
    }

    private static void assertKey(String key, String raw) {
        try {
            TniRange.expand(raw);
            fail(raw);
        } catch (AssetException ex) {
            assertEquals(key, ex.getMessageKey());
        }
    }
}
