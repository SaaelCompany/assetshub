package com.assetstree.jira.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Statuses that count as downtime. An empty setting means the built-in repair status.
 * The stored value "-" means the list was cleared.
 */
public final class Downtime {
    public static final String NONE = "-";
    private static final int LIMIT = 40;

    private Downtime() {
    }

    /** Keys ready to store. Blank stays blank. "-" stays "-". Invalid tokens are dropped. */
    public static String store(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return "";
        }
        if (NONE.equals(text)) {
            return NONE;
        }
        StringBuilder out = new StringBuilder();
        int count = 0;
        Set<String> seen = new LinkedHashSet<String>();
        String[] parts = text.split(",");
        for (int i = 0; i < parts.length; i++) {
            String key = parts[i] == null ? "" : parts[i].trim().toLowerCase(Locale.ROOT);
            if (key.isEmpty()) {
                continue;
            }
            key = Statuses.canonical(key);
            if ((!AssetValidator.isStatusKey(key) && !Statuses.ALL.contains(key)) || seen.contains(key) || count >= LIMIT) {
                continue;
            }
            seen.add(key);
            if (out.length() > 0) {
                out.append(',');
            }
            out.append(key);
            count++;
        }
        return out.length() == 0 ? NONE : out.toString();
    }

    /** Status keys that count. Missing setting is repair. A cleared list is empty. */
    public static Set<String> statuses(String stored) {
        String clean = store(stored);
        if (clean.isEmpty()) {
            return Collections.singleton(Statuses.REPAIR);
        }
        if (NONE.equals(clean)) {
            return Collections.emptySet();
        }
        LinkedHashSet<String> keys = new LinkedHashSet<String>();
        String[] parts = clean.split(",");
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                keys.add(parts[i]);
            }
        }
        return Collections.unmodifiableSet(keys);
    }

    public static boolean counts(String stored, String status) {
        return statuses(stored).contains(Statuses.canonical(status));
    }
}
