package com.assetstree.jira.model;

import java.util.Locale;

public final class ProjectKeys {
    private ProjectKeys() {
    }

    public static String slug(String projectKey) {
        String raw = projectKey == null ? "" : projectKey.trim().toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char current = raw.charAt(i);
            boolean letter = current >= 'a' && current <= 'z';
            boolean digit = current >= '0' && current <= '9';
            if (letter || digit) {
                builder.append(current);
            } else if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '-') {
                builder.append('-');
            }
        }
        while (builder.length() > 0 && builder.charAt(builder.length() - 1) == '-') {
            builder.setLength(builder.length() - 1);
        }
        String slug = builder.toString();
        if (slug.isEmpty() || slug.charAt(0) < 'a' || slug.charAt(0) > 'z') {
            slug = "p" + slug;
        }
        if (slug.length() > 20) {
            slug = slug.substring(0, 20);
        }
        return slug;
    }

    public static String typeKey(String projectKey, String baseKey) {
        return slug(projectKey) + "-" + baseKey;
    }

    public static boolean same(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }
}
