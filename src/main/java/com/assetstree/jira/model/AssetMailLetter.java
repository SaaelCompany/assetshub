package com.assetstree.jira.model;

import java.util.List;

/** Joins the ticked lines of an equipment letter. An empty selection keeps every line. */
public final class AssetMailLetter {
    private AssetMailLetter() {
    }

    public static boolean includes(String parts, String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        if (parts == null || parts.trim().isEmpty()) {
            return true;
        }
        if ("none".equals(parts.trim())) {
            return false;
        }
        String[] bits = parts.split(",");
        for (int i = 0; i < bits.length; i++) {
            if (token.equals(bits[i] == null ? "" : bits[i].trim())) {
                return true;
            }
        }
        return false;
    }

    public static String compose(List<String> lines, String extra) {
        StringBuilder body = new StringBuilder();
        if (lines != null) {
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                if (body.length() > 0) {
                    body.append('\n');
                }
                body.append(line.trim());
            }
        }
        if (extra != null && !extra.trim().isEmpty()) {
            if (body.length() > 0) {
                body.append("\n\n");
            }
            body.append(extra.trim());
        }
        return body.toString();
    }
}
