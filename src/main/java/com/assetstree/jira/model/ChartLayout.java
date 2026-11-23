package com.assetstree.jira.model;

import java.util.Locale;

/**
 * A column chart stays readable while each name fits on one line.
 * More names switch to horizontal bars so a label is not broken mid-word.
 */
public final class ChartLayout {
    public static final int COLUMN_LIMIT = 8;

    private ChartLayout() {
    }

    public static boolean columns(int rows) {
        return rows > 0 && rows <= COLUMN_LIMIT;
    }

    public static boolean ring(String chart, int rows) {
        if (!columns(rows) || chart == null) {
            return false;
        }
        String value = chart.trim().toLowerCase(Locale.ROOT);
        return "donut".equals(value) || "pie".equals(value);
    }
}
