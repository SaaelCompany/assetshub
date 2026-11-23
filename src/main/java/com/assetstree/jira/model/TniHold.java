package com.assetstree.jira.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the device report from the portal create request until the new
 * issue is stored. The issue event can run on another thread, so this is
 * kept by the reporter key for a few minutes.
 */
public final class TniHold {
    private static final long KEPT_MS = 10L * 60L * 1000L;
    private static final ConcurrentHashMap<String, Held> RECENT = new ConcurrentHashMap<String, Held>();

    private TniHold() {
    }

    public static void keep(String userKey, TniSightDraft draft) {
        if (userKey == null || userKey.trim().isEmpty() || draft == null) {
            return;
        }
        long now = System.currentTimeMillis();
        RECENT.put(userKey, new Held(draft, now));
        if (RECENT.size() > 200) {
            for (Map.Entry<String, Held> entry : RECENT.entrySet()) {
                Held held = entry.getValue();
                if (held == null || now - held.at > KEPT_MS) {
                    RECENT.remove(entry.getKey(), held);
                }
            }
        }
    }

    public static TniSightDraft take(String userKey) {
        if (userKey == null || userKey.trim().isEmpty()) {
            return null;
        }
        Held held = RECENT.remove(userKey);
        if (held == null || held.draft == null) {
            return null;
        }
        if (System.currentTimeMillis() - held.at > KEPT_MS) {
            return null;
        }
        return held.draft;
    }

    private static final class Held {
        private final TniSightDraft draft;
        private final long at;

        private Held(TniSightDraft draft, long at) {
            this.draft = draft;
            this.at = at;
        }
    }
}
