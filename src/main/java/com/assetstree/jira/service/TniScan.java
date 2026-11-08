package com.assetstree.jira.service;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Walks a prepared address list, keeps the hosts that answer, and reports progress. */
public final class TniScan {
    public interface Sink {
        /** @return true when a new asset was stored for this address */
        boolean keep(String address, String name);
    }

    public interface Tick {
        void step(int scanned, int total, int online, int added, String log);
    }

    public interface Names {
        String name(String address);
    }

    public static final Names NONE = new Names() {
        @Override
        public String name(String address) {
            return "";
        }
    };

    public static final Names DNS = new Names() {
        @Override
        public String name(String address) {
            return lookup(address);
        }
    };

    private static final int NAME_WAIT_MS = 400;
    private static final int LOG_LIMIT = 200;

    private TniScan() {
    }

    public static void run(List<String> addresses, TniReach.Probe probe, Names names, Sink sink, Tick tick) {
        List<String> hosts = addresses == null ? new ArrayList<String>() : addresses;
        TniReach.Probe check = probe == null ? TniReach.LIVE : probe;
        Names lookup = names == null ? NONE : names;
        int total = hosts.size();
        if (total == 0) {
            if (tick != null) {
                tick.step(0, 0, 0, 0, "");
            }
            return;
        }
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(8, total), daemonFactory());
        try {
            List<Future<Boolean>> waits = new ArrayList<Future<Boolean>>();
            for (final String address : hosts) {
                waits.add(pool.submit(new Callable<Boolean>() {
                    @Override
                    public Boolean call() {
                        try {
                            return Boolean.valueOf(check.up(address));
                        } catch (RuntimeException ex) {
                            return Boolean.FALSE;
                        }
                    }
                }));
            }
            int online = 0;
            int added = 0;
            StringBuilder log = new StringBuilder();
            int logged = 0;
            for (int i = 0; i < waits.size(); i++) {
                boolean up = false;
                try {
                    up = Boolean.TRUE.equals(waits.get(i).get());
                } catch (Exception ignored) {
                    up = false;
                }
                String address = hosts.get(i);
                if (up) {
                    online++;
                    String name = safeName(lookup, address);
                    boolean created = false;
                    boolean stored = false;
                    if (sink != null) {
                        try {
                            created = sink.keep(address, name);
                            stored = true;
                        } catch (RuntimeException ignored) {
                            stored = false;
                        }
                    } else {
                        stored = true;
                    }
                    if (created) {
                        added++;
                    }
                    if (stored && logged < LOG_LIMIT) {
                        if (log.length() > 0) {
                            log.append('\n');
                        }
                        log.append(address).append('\t').append(name).append('\t').append(created ? "new" : "known");
                        logged++;
                    }
                }
                if (tick != null) {
                    tick.step(i + 1, total, online, added, log.toString());
                }
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static String safeName(Names lookup, String address) {
        try {
            String name = lookup.name(address);
            if (name == null) {
                return "";
            }
            String text = name.trim();
            if (text.equalsIgnoreCase(address)) {
                return "";
            }
            if (text.length() > 80) {
                return text.substring(0, 80);
            }
            return text;
        } catch (RuntimeException ex) {
            return "";
        }
    }

    private static String lookup(final String address) {
        ExecutorService one = Executors.newSingleThreadExecutor(daemonFactory());
        try {
            Future<String> wait = one.submit(new Callable<String>() {
                @Override
                public String call() {
                    try {
                        InetAddress ip = InetAddress.getByName(address);
                        String host = ip.getHostName();
                        return host == null ? "" : host;
                    } catch (Exception ex) {
                        return "";
                    }
                }
            });
            String name = wait.get(NAME_WAIT_MS, TimeUnit.MILLISECONDS);
            return name == null ? "" : name;
        } catch (Exception ex) {
            return "";
        } finally {
            one.shutdownNow();
        }
    }

    private static ThreadFactory daemonFactory() {
        return new ThreadFactory() {
            private final AtomicInteger seq = new AtomicInteger();

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "tni-scan-" + seq.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        };
    }
}
