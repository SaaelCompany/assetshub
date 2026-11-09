package com.assetstree.jira.service;

import com.assetstree.jira.model.TniAddress;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** One short reachability check. A stored literal address is tried as itself. A host name is resolved first. */
public final class TniReach {
    public interface Probe {
        boolean up(String address);
    }

    /** Whether the host answered, and the text the ping command printed. */
    public static final class Report {
        private final boolean up;
        private final String output;

        private Report(boolean up, String output) {
            this.up = up;
            this.output = output == null ? "" : output;
        }

        public boolean up() {
            return up;
        }

        public String output() {
            return output;
        }

        static Report up(String output) {
            return new Report(true, output);
        }

        static Report down(String output) {
            return new Report(false, output);
        }
    }

    public static final Probe LIVE = new Probe() {
        @Override
        public boolean up(String address) {
            return station(address);
        }
    };

    private static final int[] PORTS = {445, 135, 22, 3389, 80, 443};
    private static final long BUDGET_MS = 7000L;

    private TniReach() {
    }

    /**
     * Checks a host name or a private address. A short name such as {@code KRUL-IT}
     * is handed to the operating-system ping, which uses the server DNS suffix and
     * NetBIOS. Java often cannot resolve that name by itself. A name that Java
     * resolves only to a public address is not contacted.
     */
    public static boolean station(String name) {
        return ask(name).up();
    }

    /** One target, including the command text. */
    public static Report ask(String name) {
        String host = TniAddress.station(name);
        if (host.isEmpty()) {
            return Report.down("");
        }
        if (!TniAddress.local(host).isEmpty()) {
            return address(host);
        }
        boolean shortName = host.indexOf('.') < 0;
        Echo first = null;
        if (shortName) {
            first = ping(host);
            if (first.up) {
                return Report.up(first.text);
            }
        }
        InetAddress[] found = lookup(host);
        if (!officeName(found)) {
            return Report.down(join(first == null ? "" : first.text, "Not contacted: the name points outside the office network."));
        }
        Echo second = first;
        if (!shortName) {
            second = ping(host);
            if (second.up) {
                return Report.up(second.text);
            }
        }
        if (found == null) {
            return Report.down(second == null ? "" : second.text);
        }
        String text = second == null ? "" : second.text;
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
            String open = ports(ip);
            if (!open.isEmpty()) {
                return Report.up(join(text, open));
            }
        }
        return Report.down(text);
    }

    /**
     * Tries the names in order and stops at the first answer.
     * The wait is capped so the page that started the check still receives the result.
     */
    public static Report askAll(List<String> targets) {
        if (targets == null || targets.isEmpty()) {
            return Report.down("");
        }
        long start = System.currentTimeMillis();
        String text = "";
        int tried = 0;
        for (int i = 0; i < targets.size(); i++) {
            if (tried >= 2 || System.currentTimeMillis() - start > BUDGET_MS) {
                break;
            }
            String target = targets.get(i);
            if (target == null || target.isEmpty()) {
                continue;
            }
            tried++;
            Report one = ask(target);
            text = join(text, one.output());
            if (one.up()) {
                return Report.up(clip(text));
            }
        }
        return Report.down(clip(text));
    }

    /** Java did not find the name, or at least one answer is on the office network. */
    static boolean officeName(InetAddress[] found) {
        if (found == null || found.length == 0) {
            return true;
        }
        for (int i = 0; i < found.length; i++) {
            InetAddress ip = found[i];
            if (ip != null && !TniAddress.local(ip.getHostAddress()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static Report address(String literal) {
        Echo echo = ping(literal);
        if (echo.up) {
            return Report.up(echo.text);
        }
        try {
            InetAddress ip = InetAddress.getByName(literal);
            if (ip == null || !literal.equals(ip.getHostAddress())) {
                return Report.down(echo.text);
            }
            String open = ports(ip);
            if (!open.isEmpty()) {
                return Report.up(join(echo.text, open));
            }
        } catch (IOException ex) {
            return Report.down(echo.text);
        } catch (SecurityException ex) {
            return Report.down(echo.text);
        }
        return Report.down(echo.text);
    }

    private static InetAddress[] lookup(String host) {
        try {
            return InetAddress.getAllByName(host);
        } catch (IOException ex) {
            return null;
        } catch (SecurityException ex) {
            return null;
        }
    }

    static boolean reachable(String address) {
        return address(address).up();
    }

    /** The first open workstation port, as one line, or an empty string. */
    private static String ports(InetAddress ip) {
        if (ip == null) {
            return "";
        }
        for (int port : PORTS) {
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(ip, port), 400);
                return "TCP " + port + ": open";
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
        return "";
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
        return ping(literal).up;
    }

    /** One ICMP echo. {@code target} is a private address or a host name already checked by {@link TniAddress#station}. */
    private static Echo ping(String target) {
        String host = TniAddress.local(target);
        if (host.isEmpty()) {
            host = TniAddress.station(target);
        }
        if (host.isEmpty()) {
            return new Echo(false, "");
        }
        String os = System.getProperty("os.name", "");
        boolean windows = os.toLowerCase(Locale.ROOT).indexOf("win") >= 0;
        List<String> command = new ArrayList<String>();
        command.add("ping");
        if (windows) {
            command.add("-n");
            command.add("1");
            command.add("-w");
            command.add("2000");
        } else {
            command.add("-c");
            command.add("1");
            command.add("-W");
            command.add("2");
        }
        command.add(host);
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < command.size(); i++) {
            if (i > 0) {
                line.append(' ');
            }
            line.append(command.get(i));
        }
        Process process = null;
        final StringBuilder captured = new StringBuilder();
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            process = builder.start();
            final java.io.InputStream output = process.getInputStream();
            final Charset charset = pingCharset(windows);
            Thread drain = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[512];
                    try {
                        int read;
                        while ((read = output.read(buffer)) >= 0) {
                            if (read > 0) {
                                captured.append(new String(buffer, 0, read, charset));
                            }
                        }
                    } catch (IOException ignored) {
                        // The process has already finished.
                    }
                }
            }, "tni-ping");
            drain.setDaemon(true);
            drain.start();
            boolean done = process.waitFor(2500L, TimeUnit.MILLISECONDS);
            if (!done) {
                process.destroy();
            }
            drain.join(500L);
            String body = captured.toString().replace("\r\n", "\n").replace('\r', '\n').trim();
            String text = body.isEmpty() ? line.toString() : line + "\n" + body;
            return new Echo(done && process.exitValue() == 0, text);
        } catch (IOException ex) {
            return new Echo(false, line.toString());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new Echo(false, line.toString());
        } catch (SecurityException ex) {
            return new Echo(false, line.toString());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static Charset pingCharset(boolean windows) {
        if (windows) {
            try {
                return Charset.forName("Cp866");
            } catch (RuntimeException ignored) {
                // The console page is missing. The platform charset still prints ASCII.
            }
        }
        return Charset.defaultCharset();
    }

    private static String join(String left, String right) {
        if (left == null || left.isEmpty()) {
            return right == null ? "" : right;
        }
        if (right == null || right.isEmpty()) {
            return left;
        }
        return left + "\n" + right;
    }

    private static String clip(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder kept = new StringBuilder();
        for (int i = 0; i < text.length() && kept.length() < 8000; i++) {
            char ch = text.charAt(i);
            if (ch == '\n' || ch == '\t' || ch >= ' ') {
                kept.append(ch);
            }
        }
        return kept.toString();
    }

    private static final class Echo {
        private final boolean up;
        private final String text;

        private Echo(boolean up, String text) {
            this.up = up;
            this.text = text == null ? "" : text;
        }
    }
}
