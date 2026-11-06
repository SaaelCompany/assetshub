package com.assetstree.jira.service;

import com.assetstree.jira.model.TniAddress;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/** One short reachability check. Only a literal address the module already stored is tried. */
public final class TniReach {
    public interface Probe {
        boolean up(String address);
    }

    public static final Probe LIVE = new Probe() {
        @Override
        public boolean up(String address) {
            return reachable(address);
        }
    };

    private static final int[] PORTS = {445, 135, 22, 3389, 80, 443};

    private TniReach() {
    }

    static boolean reachable(String address) {
        if (!TniAddress.allowed(address)) {
            return false;
        }
        try {
            InetAddress ip = InetAddress.getByName(address);
            if (ip == null || !address.equals(ip.getHostAddress())) {
                return false;
            }
            if (ip.isLoopbackAddress() || ip.isAnyLocalAddress() || ip.isLinkLocalAddress() || ip.isMulticastAddress()) {
                return false;
            }
            try {
                if (ip.isReachable(800)) {
                    return true;
                }
            } catch (IOException ignored) {
                // ICMP is often blocked. A TCP connect is the next check.
            }
        } catch (IOException ex) {
            return false;
        } catch (SecurityException ex) {
            return false;
        }
        for (int port : PORTS) {
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(address, port), 400);
                return true;
            } catch (IOException ignored) {
                // This port is closed. Another common workstation port may still answer.
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {
                    // The socket is already closed.
                }
            }
        }
        return false;
    }
}
