package com.assetstree.jira.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers the equipment a customer chose in the profile until the portal
 * creates the request. The request type does not need an asset field.
 */
public final class PortalAssetHold {
    private static final long KEPT_MS = 2L * 60L * 60L * 1000L;
    private static final ConcurrentHashMap<String, Held> RECENT = new ConcurrentHashMap<String, Held>();
    private static final ThreadLocal<Integer> DEPTH = new ThreadLocal<Integer>();
    private static final ThreadLocal<Integer> REQUEST = new ThreadLocal<Integer>();

    private PortalAssetHold() {
    }

    /** One request can be forwarded. The asset stays until the outer filter returns. */
    public static void enter() {
        Integer depth = DEPTH.get();
        DEPTH.set(depth == null ? Integer.valueOf(1) : Integer.valueOf(depth.intValue() + 1));
    }

    public static void leave() {
        Integer depth = DEPTH.get();
        if (depth == null || depth.intValue() <= 1) {
            DEPTH.remove();
            REQUEST.remove();
            return;
        }
        DEPTH.set(Integer.valueOf(depth.intValue() - 1));
    }

    public static void bindRequest(int assetId) {
        if (assetId > 0) {
            REQUEST.set(Integer.valueOf(assetId));
        }
    }

    public static Integer requestAsset() {
        return REQUEST.get();
    }

    public static void keep(String userKey, int assetId) {
        if (userKey == null || userKey.trim().isEmpty() || assetId <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        RECENT.put(userKey, new Held(assetId, now));
        if (RECENT.size() > 200) {
            for (Map.Entry<String, Held> entry : RECENT.entrySet()) {
                Held held = entry.getValue();
                if (held == null || now - held.at > KEPT_MS) {
                    RECENT.remove(entry.getKey(), held);
                }
            }
        }
    }

    public static Integer peek(String userKey) {
        Held held = held(userKey);
        return held == null ? null : Integer.valueOf(held.assetId);
    }

    public static void take(String userKey) {
        if (userKey == null || userKey.trim().isEmpty()) {
            return;
        }
        RECENT.remove(userKey);
    }

    private static Held held(String userKey) {
        if (userKey == null || userKey.trim().isEmpty()) {
            return null;
        }
        Held held = RECENT.get(userKey);
        if (held == null) {
            return null;
        }
        if (System.currentTimeMillis() - held.at > KEPT_MS) {
            RECENT.remove(userKey, held);
            return null;
        }
        return held;
    }

    private static final class Held {
        private final int assetId;
        private final long at;

        private Held(int assetId, long at) {
            this.assetId = assetId;
            this.at = at;
        }
    }
}
