package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The asset field sometimes stores the visible path instead of the numeric id.
 * These hints are the strings worth matching against an object key or a name.
 */
public final class AssetFieldText {
    private AssetFieldText() {
    }

    public static List<String> lookupHints(String raw) {
        List<String> hints = new ArrayList<String>();
        if (raw == null) {
            return hints;
        }
        String text = raw.trim();
        if (text.isEmpty() || "-1".equals(text)) {
            return hints;
        }
        add(hints, text);
        split(hints, text, '/');
        int gap = text.indexOf("  ");
        if (gap > 0) {
            add(hints, text.substring(0, gap));
            add(hints, text.substring(gap + 2));
        }
        int dot = text.indexOf('·');
        if (dot > 0) {
            add(hints, text.substring(0, dot));
            add(hints, text.substring(dot + 1));
        }
        return hints;
    }

    private static void split(List<String> hints, String text, char mark) {
        if (text.indexOf(mark) < 0) {
            return;
        }
        String[] parts = text.split(String.valueOf(mark));
        for (int i = parts.length - 1; i >= 0; i--) {
            add(hints, parts[i]);
        }
    }

    private static void add(List<String> hints, String value) {
        if (value == null) {
            return;
        }
        String clean = value.trim();
        if (clean.isEmpty() || hints.contains(clean)) {
            return;
        }
        hints.add(clean);
    }
}
