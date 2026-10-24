package com.assetstree.jira.model;

import java.util.Set;

/** Next free name for a copied object: "Name (copy)", then "Name (copy 2)". */
public final class CopyNames {
    private CopyNames() {
    }

    public static String next(String base, String word, Set<String> taken) {
        String stem = base == null ? "" : base.trim();
        String label = word == null ? "" : word.trim();
        if (label.isEmpty()) {
            label = "copy";
        }
        for (int number = 1; number < 1000; number++) {
            String suffix = number == 1 ? " (" + label + ")" : " (" + label + " " + number + ")";
            int room = AssetValidator.MAX_NAME - suffix.length();
            if (room < 1) {
                room = 1;
            }
            String head = stem.length() > room ? stem.substring(0, room).trim() : stem;
            String candidate = head + suffix;
            if (candidate.length() > AssetValidator.MAX_NAME) {
                candidate = candidate.substring(0, AssetValidator.MAX_NAME);
            }
            if (taken == null || !taken.contains(candidate)) {
                return candidate;
            }
        }
        return stem.length() > AssetValidator.MAX_NAME ? stem.substring(0, AssetValidator.MAX_NAME) : stem;
    }
}
