package com.assetstree.jira.model;

import java.net.InetAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * Reverse name for a private address Jira already accepted a connection from.
 * A missing DNS record stays blank. The lookup does not open a connection to the computer.
 */
public final class TniName {
    private static final long WAIT_MS = 1500L;

    private TniName() {
    }

    public static String lookup(final String address) {
        if (!TniAddress.allowed(address) || !TniAddress.isPrivate(address)) {
            return "";
        }
        FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() {
                try {
                    String name = InetAddress.getByName(address).getHostName();
                    return clean(name);
                } catch (Exception ex) {
                    return "";
                }
            }
        });
        Thread thread = new Thread(task, "tni-name");
        thread.setDaemon(true);
        thread.start();
        try {
            String name = task.get(WAIT_MS, TimeUnit.MILLISECONDS);
            return name == null ? "" : name;
        } catch (Exception ex) {
            task.cancel(true);
            return "";
        }
    }

    public static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        if (text.endsWith(".")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.length() < 2 || text.length() > 253 || TniAddress.allowed(text)) {
            return "";
        }
        boolean letter = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) {
                letter = true;
            } else if (!((ch >= '0' && ch <= '9') || ch == '.' || ch == '-')) {
                return "";
            }
        }
        if (!letter || text.charAt(0) == '.' || text.charAt(0) == '-' || text.contains("..")) {
            return "";
        }
        return text;
    }
}
