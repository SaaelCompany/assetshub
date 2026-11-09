package com.assetstree.jira.service;

import com.assetstree.jira.model.TniAddress;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

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
        if (icmp(ip)) {
            return true;
        }
        for (int port : PORTS) {
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(ip, port), 500);
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

    /**
     * One ICMP echo through the operating-system ping. Java's own reachability check
     * cannot send ICMP without extra privileges, so a workstation that answers ping
     * and nothing else looked silent.
     */
    static boolean icmp(InetAddress ip) {
        if (ip == null) {
            return false;
        }
        String literal = TniAddress.local(ip.getHostAddress());
        if (literal.isEmpty()) {
            return false;
        }
        String os = System.getProperty("os.name", "");
        boolean windows = os.toLowerCase(Locale.ROOT).indexOf("win") >= 0;
        List<String> command = new ArrayList<String>();
        command.add("ping");
        if (windows) {
            command.add("-n");
            command.add("1");
            command.add("-w");
            command.add("1000");
        } else {
            command.add("-c");
            command.add("1");
            command.add("-W");
            command.add("1");
        }
        command.add(literal);
        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            process = builder.start();
            final java.io.InputStream output = process.getInputStream();
            Thread drain = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[256];
                    try {
                        while (output.read(buffer) >= 0) {
                            // Ping writes a few lines. Leaving them unread would stall the process.
                        }
                    } catch (IOException ignored) {
                        // The process has already finished.
                    }
                }
            }, "tni-ping");
            drain.setDaemon(true);
            drain.start();
            boolean done = process.waitFor(1500L, TimeUnit.MILLISECONDS);
            if (!done) {
                process.destroy();
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException ex) {
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        } catch (SecurityException ex) {
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}
