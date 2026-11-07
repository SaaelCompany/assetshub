package com.assetstree.jira.service;

import com.assetstree.jira.model.TniAddress;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/** One short reachability check. A stored literal address is tried as itself. A host name is resolved first. */
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

    /**
     * Checks a host name or a private address. DNS is asked on every check.
     * A name that resolves outside the local ranges is not contacted.
     */
    public static boolean station(String name) {
        String host = TniAddress.station(name);
        if (host.isEmpty()) {
            return false;
        }
        if (!TniAddress.local(host).isEmpty()) {
            return reachable(host);
        }
        InetAddress[] found;
        try {
            found = InetAddress.getAllByName(host);
        } catch (IOException ex) {
            return false;
        } catch (SecurityException ex) {
            return false;
        }
        if (found == null) {
            return false;
        }
        int seen = 0;
        for (int i = 0; i < found.length; i++) {
            if (seen >= 4) {
                break;
            }
            InetAddress ip = found[i];
            if (ip == null) {
                continue;
            }
            seen++;
            String literal = TniAddress.local(ip.getHostAddress());
            if (literal.isEmpty() || !literal.equals(ip.getHostAddress())) {
                continue;
            }
            if (probe(ip)) {
                return true;
            }
        }
        return false;
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
            return probe(ip);
        } catch (IOException ex) {
            return false;
        } catch (SecurityException ex) {
            return false;
        }
    }

    private static boolean probe(InetAddress ip) {
        try {
            if (ip.isReachable(800)) {
                return true;
            }
        } catch (IOException ignored) {
            // ICMP is often blocked. A TCP connect is the next check.
        }
        for (int port : PORTS) {
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(ip, port), 400);
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
