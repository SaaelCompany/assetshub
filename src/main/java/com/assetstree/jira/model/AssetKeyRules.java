package com.assetstree.jira.model;

/**
 * An asset prefix is chosen once per Jira project and is never the project key.
 * Already issued object keys are left as they are.
 */
public final class AssetKeyRules {
    private AssetKeyRules() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current >= 'a' && current <= 'z') {
                builder.append((char) (current - 32));
            } else {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    /** {@code null} when the prefix may be stored. Otherwise an error key. */
    public static String invalid(String normalized) {
        if (normalized == null || normalized.isEmpty()) {
            return "asset-tree.error.assetKey.required";
        }
        if (normalized.length() < 2 || normalized.length() > 10) {
            return "asset-tree.error.assetKey.format";
        }
        char first = normalized.charAt(0);
        if (first < 'A' || first > 'Z') {
            return "asset-tree.error.assetKey.format";
        }
        for (int i = 0; i < normalized.length(); i++) {
            char current = normalized.charAt(i);
            boolean letter = current >= 'A' && current <= 'Z';
            boolean digit = current >= '0' && current <= '9';
            if (!letter && !digit) {
                return "asset-tree.error.assetKey.format";
            }
        }
        return null;
    }
}
