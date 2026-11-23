package com.assetstree.jira.model;

import java.util.Locale;

/**
 * Address of the computer that opened the portal, as far as Jira can see it.
 * A Docker bridge gateway such as 172.17.0.1 is the container host, so it is
 * not recorded. When the connection comes from that gateway or from localhost,
 * the address is taken from the proxy headers. A direct connection ignores them.
 */
public final class TniPeer {
    private TniPeer() {
    }

    public static String address(String remote) {
        String text = literal(remote);
        if (!TniAddress.allowed(text) || TniAddress.isGateway(text)) {
            return "";
        }
        return text;
    }

    public static String client(String remote, String forwardedFor, String realIp, String forwarded) {
        String direct = literal(remote);
        if (TniAddress.allowed(direct) && !TniAddress.isGateway(direct)) {
            return direct;
        }
        if (!TniAddress.isGateway(direct) && !TniAddress.isLoopback(direct)) {
            return "";
        }
        String via = listed(forwardedFor);
        if (via.isEmpty()) {
            via = listed(forwarded);
        }
        if (via.isEmpty()) {
            via = listed(realIp);
        }
        return via;
    }

    private static String listed(String header) {
        if (header == null || header.isEmpty()) {
            return "";
        }
        String[] parts = header.split(",");
        for (int i = parts.length - 1; i >= 0; i--) {
            String candidate = address(forValue(parts[i]));
            if (!candidate.isEmpty()) {
                return candidate;
            }
        }
        return "";
    }

    private static String forValue(String token) {
        if (token == null) {
            return "";
        }
        String text = token.trim();
        int forAt = text.toLowerCase(Locale.ROOT).indexOf("for=");
        if (forAt >= 0) {
            text = text.substring(forAt + 4).trim();
            int semi = text.indexOf(';');
            if (semi >= 0) {
                text = text.substring(0, semi).trim();
            }
        }
        if (text.length() >= 2 && text.charAt(0) == '"' && text.charAt(text.length() - 1) == '"') {
            text = text.substring(1, text.length() - 1).trim();
        }
        return text;
    }

    private static String literal(String remote) {
        if (remote == null) {
            return "";
        }
        String text = remote.trim();
        if (text.startsWith("/")) {
            text = text.substring(1).trim();
        }
        int zone = text.indexOf('%');
        if (zone > 0) {
            text = text.substring(0, zone);
        }
        if (text.regionMatches(true, 0, "::ffff:", 0, 7)) {
            text = text.substring(7);
        }
        if (text.startsWith("[") && text.indexOf(']') > 1) {
            text = text.substring(1, text.indexOf(']'));
        }
        return text;
    }
}
