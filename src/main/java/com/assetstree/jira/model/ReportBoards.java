package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** A named board is an ordered list of report panels. Each panel names a report, a chart and a width. */
public final class ReportBoards {
    public static final int MAX_BOARDS = 12;
    public static final int MAX_PANELS = 12;
    public static final String FULL = "full";
    public static final String HALF = "half";

    private static final List<String> CHARTS = Collections.unmodifiableList(Arrays.asList(
            "histogram", "bars", "line", "area", "scatter", "donut", "pie", "radar", "gantt", "venn"));

    private ReportBoards() {
    }

    public static final class Panel {
        private final String report;
        private final String chart;
        private final String width;

        public Panel(String report, String chart, String width) {
            this.report = report == null ? "" : report;
            this.chart = chart == null ? "" : chart;
            this.width = width == null ? HALF : width;
        }

        public String getReport() {
            return report;
        }

        public String getChart() {
            return chart;
        }

        public String getWidth() {
            return width;
        }
    }

    /** A board title, or empty when the text cannot be stored. */
    public static String name(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim().replace('\n', ' ').replace('\r', ' ');
        if (value.isEmpty() || value.length() > 80) {
            return "";
        }
        return value;
    }

    /** Keeps panels the report screen can draw, at most {@link #MAX_PANELS}. */
    public static List<Panel> read(String stored) {
        List<Panel> panels = new ArrayList<Panel>();
        if (stored == null || stored.isEmpty()) {
            return panels;
        }
        String[] parts = stored.split(",");
        for (int i = 0; i < parts.length; i++) {
            if (panels.size() >= MAX_PANELS) {
                break;
            }
            Panel panel = one(parts[i]);
            if (panel != null) {
                panels.add(panel);
            }
        }
        return panels;
    }

    public static String write(List<Panel> panels) {
        StringBuilder out = new StringBuilder();
        if (panels == null) {
            return "";
        }
        int kept = 0;
        for (int i = 0; i < panels.size(); i++) {
            Panel panel = panels.get(i);
            if (panel == null) {
                continue;
            }
            Panel clean = one(panel.getReport() + "|" + panel.getChart() + "|" + panel.getWidth());
            if (clean == null) {
                continue;
            }
            if (kept >= MAX_PANELS) {
                break;
            }
            if (out.length() > 0) {
                out.append(',');
            }
            out.append(clean.getReport()).append('|').append(clean.getChart()).append('|').append(clean.getWidth());
            kept++;
        }
        return out.toString();
    }

    private static Panel one(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        int first = text.indexOf('|');
        int second = first < 0 ? -1 : text.indexOf('|', first + 1);
        if (first <= 0 || second <= first) {
            return null;
        }
        String report = report(text.substring(0, first));
        String chart = chart(text.substring(first + 1, second));
        String width = width(text.substring(second + 1));
        if (report.isEmpty() || chart.isEmpty()) {
            return null;
        }
        return new Panel(report, chart, width);
    }

    private static String report(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (ReportCatalog.isStandard(value) || ReportCatalog.savedId(value) > 0) {
            return value;
        }
        return "";
    }

    private static String chart(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if ("bar".equals(value) || "columns".equals(value)) {
            return "histogram";
        }
        return CHARTS.contains(value) ? value : "";
    }

    private static String width(String raw) {
        if (raw != null && FULL.equals(raw.trim().toLowerCase(Locale.ROOT))) {
            return FULL;
        }
        return HALF;
    }
}
