package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Expands a scanner task the way Total Network Inventory does:
 * a network in CIDR form, an address range, or a list of addresses.
 * Only local IPv4 networks are accepted, and one task stays within {@link #MAX_HOSTS}.
 */
public final class TniRange {
    public static final int MAX_HOSTS = 1024;
    public static final String BAD = "asset-tree.error.tni.scan";
    public static final String PUBLIC = "asset-tree.error.tni.public";
    public static final String LARGE = "asset-tree.error.tni.large";

    private TniRange() {
    }

    public static List<String> expand(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new AssetException(400, BAD);
        }
        String text = raw.trim();
        if (text.length() > 2000) {
            throw new AssetException(400, BAD);
        }
        Set<String> hosts = new LinkedHashSet<String>();
        String[] chunks = text.split("[,;\\r\\n]+");
        for (String chunk : chunks) {
            if (chunk == null) {
                continue;
            }
            String piece = chunk.trim();
            if (piece.isEmpty()) {
                continue;
            }
            addPiece(hosts, piece);
            if (hosts.size() > MAX_HOSTS) {
                throw new AssetException(400, LARGE);
            }
        }
        if (hosts.isEmpty()) {
            throw new AssetException(400, BAD);
        }
        return new ArrayList<String>(hosts);
    }

    private static void addPiece(Set<String> hosts, String piece) {
        String range = rangeOf(piece);
        if (range != null) {
            addRange(hosts, range);
            return;
        }
        if (piece.indexOf('/') >= 0 && piece.indexOf(' ') < 0) {
            addCidr(hosts, piece);
            return;
        }
        String[] words = piece.split("\\s+");
        if (words.length == 1) {
            hosts.add(one(words[0]));
            return;
        }
        for (String word : words) {
            if (word.indexOf('/') >= 0) {
                addCidr(hosts, word);
            } else if (rangeOf(word) != null) {
                addRange(hosts, word);
            } else {
                hosts.add(one(word));
            }
            if (hosts.size() > MAX_HOSTS) {
                throw new AssetException(400, LARGE);
            }
        }
    }

    private static void addCidr(Set<String> hosts, String token) {
        int slash = token.indexOf('/');
        if (slash <= 0 || slash != token.lastIndexOf('/')) {
            throw new AssetException(400, BAD);
        }
        String address = one(token.substring(0, slash));
        String prefixText = token.substring(slash + 1).trim();
        if (prefixText.isEmpty() || prefixText.length() > 2 || !digits(prefixText)) {
            throw new AssetException(400, BAD);
        }
        int prefix = Integer.parseInt(prefixText);
        if (prefix < 0 || prefix > 32) {
            throw new AssetException(400, BAD);
        }
        int hostBits = 32 - prefix;
        if (hostBits > 10) {
            throw new AssetException(400, LARGE);
        }
        long base = toLong(address);
        long mask = prefix == 0 ? 0L : (0xffffffffL << hostBits) & 0xffffffffL;
        long network = base & mask;
        long count = 1L << hostBits;
        long start = network;
        long end = network + count - 1L;
        if (prefix <= 30) {
            start++;
            end--;
        }
        if (end < start) {
            throw new AssetException(400, BAD);
        }
        addSpan(hosts, start, end);
    }

    private static void addRange(Set<String> hosts, String token) {
        int dash = token.indexOf('-');
        String left = one(token.substring(0, dash).trim());
        String right = one(token.substring(dash + 1).trim());
        long start = toLong(left);
        long end = toLong(right);
        if (end < start) {
            throw new AssetException(400, BAD);
        }
        if (end - start + 1L > MAX_HOSTS) {
            throw new AssetException(400, LARGE);
        }
        addSpan(hosts, start, end);
    }

    private static void addSpan(Set<String> hosts, long start, long end) {
        for (long value = start; value <= end; value++) {
            hosts.add(one(fromLong(value)));
            if (hosts.size() > MAX_HOSTS) {
                throw new AssetException(400, LARGE);
            }
        }
    }

    private static String rangeOf(String piece) {
        int dash = piece.indexOf('-');
        if (dash <= 0 || dash != piece.lastIndexOf('-')) {
            return null;
        }
        String left = piece.substring(0, dash).trim();
        String right = piece.substring(dash + 1).trim();
        if (left.isEmpty() || right.isEmpty() || left.indexOf('/') >= 0 || right.indexOf('/') >= 0) {
            return null;
        }
        return piece;
    }

    private static String one(String token) {
        String address = TniAddress.choose(java.util.Collections.singletonList(token == null ? "" : token.trim()));
        if (address.isEmpty()) {
            throw new AssetException(400, BAD);
        }
        if (!TniAddress.isPrivate(address)) {
            throw new AssetException(400, PUBLIC);
        }
        return address;
    }

    private static long toLong(String address) {
        String[] parts = address.split("\\.");
        long value = 0L;
        for (String part : parts) {
            value = (value << 8) | Integer.parseInt(part);
        }
        return value;
    }

    private static String fromLong(long value) {
        return ((value >> 24) & 255L) + "."
                + ((value >> 16) & 255L) + "."
                + ((value >> 8) & 255L) + "."
                + (value & 255L);
    }

    private static boolean digits(String text) {
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch < '0' || ch > '9') {
                return false;
            }
        }
        return true;
    }

    static String clip(String value, int max) {
        if (value == null) {
            return "";
        }
        String text = value.trim();
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }

    public static String name(String raw) {
        String text = clip(raw, 80);
        if (text.isEmpty()) {
            throw new AssetException(400, "asset-tree.error.tni.name");
        }
        return text;
    }
}
