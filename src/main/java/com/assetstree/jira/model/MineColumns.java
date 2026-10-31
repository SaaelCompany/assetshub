package com.assetstree.jira.model;

import com.assetstree.jira.dto.MineViewDto;

import java.util.ArrayList;
import java.util.List;

/** Personal «Мои активы» columns stored on the Jira user, one column key per line. */
public final class MineColumns {
    public static final String KEY = "asset-tree.mineColumns";
    public static final int MAX = 40;
    public static final int MAX_KEY = 80;
    public static final int MAX_TEXT = 4000;

    private MineColumns() {
    }

    public static String encode(List<String> columns, String sortKey, String sortDir) {
        StringBuilder out = new StringBuilder();
        out.append(cleanKey(sortKey)).append('\n');
        out.append("desc".equals(sortDir) ? "desc" : "asc").append('\n');
        int count = 0;
        if (columns != null) {
            for (String column : columns) {
                if (count >= MAX) {
                    break;
                }
                String key = cleanKey(column);
                if (key.isEmpty()) {
                    continue;
                }
                out.append(key).append('\n');
                count++;
            }
        }
        String text = out.toString();
        return text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text;
    }

    /** Null when nothing was stored. An empty column list is a saved choice. */
    public static MineViewDto decode(String raw) {
        if (raw == null) {
            return null;
        }
        String[] lines = raw.split("\n", -1);
        MineViewDto view = new MineViewDto();
        view.setSortKey(lines.length > 0 ? cleanKey(lines[0]) : "");
        view.setSortDir(lines.length > 1 && "desc".equals(lines[1].trim()) ? "desc" : "asc");
        List<String> columns = new ArrayList<String>();
        for (int i = 2; i < lines.length && columns.size() < MAX; i++) {
            String key = cleanKey(lines[i]);
            if (!key.isEmpty() && !columns.contains(key)) {
                columns.add(key);
            }
        }
        view.setColumns(columns);
        return view;
    }

    public static String cleanKey(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.isEmpty() || value.length() > MAX_KEY) {
            return "";
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 32) {
                return "";
            }
        }
        return value;
    }
}
