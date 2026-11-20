package com.assetstree.jira.model;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Which card blocks one person keeps open. 1 is collapsed, 0 is open. */
public final class CardFolds {
    public static final String KEY = "asset-tree.cardFolds";
    private static final List<String> BLOCKS = Arrays.asList(
            "details", "description", "service", "life", "issues", "files", "activity",
            "people", "links", "dates", "schemes", "inside", "types");

    private CardFolds() {
    }

    public static String clean(String raw) {
        Map<String, String> chosen = new LinkedHashMap<String, String>();
        if (raw != null) {
            String[] parts = raw.split(",");
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i] == null ? "" : parts[i].trim();
                int cut = part.indexOf('=');
                if (cut < 1) {
                    continue;
                }
                String key = part.substring(0, cut);
                String bit = part.substring(cut + 1);
                if (!BLOCKS.contains(key) || (!"0".equals(bit) && !"1".equals(bit))) {
                    continue;
                }
                chosen.put(key, bit);
            }
        }
        StringBuilder kept = new StringBuilder();
        for (int i = 0; i < BLOCKS.size(); i++) {
            String key = BLOCKS.get(i);
            if (!chosen.containsKey(key)) {
                continue;
            }
            if (kept.length() > 0) {
                kept.append(',');
            }
            kept.append(key).append('=').append(chosen.get(key));
        }
        return kept.toString();
    }
}
