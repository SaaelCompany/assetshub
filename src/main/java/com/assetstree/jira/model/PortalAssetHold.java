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

    private PortalAssetHold() {
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
