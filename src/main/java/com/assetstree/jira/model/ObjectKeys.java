package com.assetstree.jira.model;

/**
 * Object keys already issued stay as they are.
 * New keys use the asset prefix stored for the project, not the Jira project key.
 */
public final class ObjectKeys {
    private ObjectKeys() {
    }

    public static String prefix(String projectKey) {
        String raw = projectKey == null ? "" : projectKey.trim();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char current = raw.charAt(i);
            if (current >= 'a' && current <= 'z') {
                builder.append((char) (current - 32));
            } else if ((current >= 'A' && current <= 'Z') || (current >= '0' && current <= '9')) {
                builder.append(current);
            }
        }
        if (builder.length() == 0) {
            return "AST";
        }
        if (builder.length() > 20) {
            builder.setLength(20);
        }
        return builder.toString();
    }

    public static String format(String projectKey, int number) {
        return prefix(projectKey) + "-" + number;
    }

    /** The number after {@code prefix-}, or 0 when the key is not that sequence. */
    public static int suffix(String prefix, String objectKey) {
        if (prefix == null || prefix.isEmpty() || objectKey == null) {
            return 0;
        }
        String head = prefix + "-";
        if (objectKey.length() <= head.length() || !objectKey.regionMatches(true, 0, head, 0, head.length())) {
            return 0;
        }
        String tail = objectKey.substring(head.length());
        if (tail.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < tail.length(); i++) {
            char current = tail.charAt(i);
            if (current < '0' || current > '9') {
                return 0;
            }
        }
        try {
            return Integer.parseInt(tail);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
