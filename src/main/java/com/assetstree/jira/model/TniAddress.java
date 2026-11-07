package com.assetstree.jira.model;

import java.util.List;

/** Picks one IPv4 address the plugin may later try to reach. Host names are not accepted. */
public final class TniAddress {
    private TniAddress() {
    }

    public static String choose(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String publicAddress = "";
        int seen = 0;
        for (String item : raw) {
            if (seen >= 8) {
                break;
            }
            seen++;
            String address = literal(item);
            if (address.isEmpty() || !allowed(address)) {
                continue;
            }
            if (isPrivate(address)) {
                return address;
            }
            if (publicAddress.isEmpty()) {
                publicAddress = address;
            }
        }
        return publicAddress;
    }

    /**
     * A host name or a private IPv4 address chosen for one asset.
     * The name is stored as typed. Each check resolves it again, so a DHCP lease can move.
     */
    public static String station(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        String local = local(text);
        if (!local.isEmpty()) {
            return local;
        }
        if (text.isEmpty() || text.length() > 253 || text.charAt(0) == '.' || text.charAt(text.length() - 1) == '.') {
            return "";
        }
        if (text.indexOf("..") >= 0 || "localhost".equalsIgnoreCase(text)) {
            return "";
        }
        boolean letter = false;
        int start = 0;
        for (int i = 0; i <= text.length(); i++) {
            if (i < text.length() && text.charAt(i) != '.') {
                char c = text.charAt(i);
                boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-';
                if (!ok) {
                    return "";
                }
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                    letter = true;
                }
                continue;
            }
            int length = i - start;
            if (length < 1 || length > 63) {
                return "";
            }
            char first = text.charAt(start);
            char last = text.charAt(i - 1);
            if (first == '-' || last == '-') {
                return "";
            }
            start = i + 1;
        }
        return letter ? text : "";
    }

    /** A private IPv4 address an administrator chose for one asset, or an empty string. */
    public static String local(String raw) {
        String address = literal(raw);
        if (address.isEmpty() || !allowed(address) || !isPrivate(address)) {
            return "";
        }
        return address;
    }

    public static boolean allowed(String address) {
        int[] octets = octets(address);
        if (octets == null) {
            return false;
        }
        int first = octets[0];
        if (first == 0 || first == 127 || first >= 224) {
            return false;
        }
        if (first == 169 && octets[1] == 254) {
            return false;
        }
        return true;
    }

    /** Docker bridge gateways. The computer that opened the portal is never this address. */
    static boolean isGateway(String address) {
        int[] octets = octets(address);
        if (octets == null) {
            return false;
        }
        return octets[0] == 172 && octets[1] >= 17 && octets[1] <= 31
                && octets[2] == 0 && octets[3] == 1;
    }

    static boolean isLoopback(String address) {
        int[] octets = octets(address);
        return octets != null && octets[0] == 127;
    }

    static boolean isPrivate(String address) {
        int[] octets = octets(address);
        if (octets == null) {
            return false;
        }
        if (octets[0] == 10) {
            return true;
        }
        if (octets[0] == 192 && octets[1] == 168) {
            return true;
        }
        return octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31;
    }

    private static String literal(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        int[] octets = octets(text);
        if (octets == null) {
            return "";
        }
        return octets[0] + "." + octets[1] + "." + octets[2] + "." + octets[3];
    }

    private static int[] octets(String address) {
        if (address == null || address.isEmpty() || address.length() > 15) {
            return null;
        }
        String[] parts = address.split("\\.");
        if (parts.length != 4) {
            return null;
        }
        int[] octets = new int[4];
        for (int i = 0; i < 4; i++) {
            String part = parts[i];
            if (part.isEmpty() || part.length() > 3 || !digits(part)) {
                return null;
            }
            if (part.length() > 1 && part.charAt(0) == '0') {
                return null;
            }
            int value = Integer.parseInt(part);
            if (value > 255) {
                return null;
            }
            octets[i] = value;
        }
        return octets;
    }

    private static boolean digits(String part) {
        for (int i = 0; i < part.length(); i++) {
            char ch = part.charAt(i);
            if (ch < '0' || ch > '9') {
                return false;
            }
        }
        return true;
    }
}
