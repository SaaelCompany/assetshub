package com.assetstree.jira.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Built-in asset reports and the groupings a saved report may use. */
public final class ReportCatalog {
    public static final String STATUS = "status";
    public static final String TYPE = "type";
    public static final String PLACE = "place";
    public static final String HOLDER = "holder";
    public static final String ISSUED = "issued";
    public static final String SERVICE = "service";
    public static final String WARRANTY = "warranty";
    public static final String COST = "cost";
    public static final String RECEIPTS = "receipts";
    public static final String AGE = "age";
    public static final String WRITEOFF = "writeoff";
    public static final String WORK = "work";
    public static final String FIELD = "field";

    private static final List<String> STANDARD = Collections.unmodifiableList(Arrays.asList(
            STATUS, TYPE, PLACE, HOLDER, ISSUED, RECEIPTS, COST, WARRANTY, AGE, WRITEOFF, WORK));
    private static final List<String> GROUPS = Collections.unmodifiableList(Arrays.asList(
            STATUS, TYPE, PLACE, HOLDER, ISSUED, "month", "age", WARRANTY, WRITEOFF, "kind", FIELD));
    private static final List<String> CHARTS = Collections.unmodifiableList(Arrays.asList(
            "donut", "pie", "bar", "bars", "line", "area", "histogram", "scatter", "radar", "gantt", "venn"));

    private ReportCatalog() {
    }

    public static List<String> standard() {
        return STANDARD;
    }

    public static boolean isStandard(String key) {
        return key != null && STANDARD.contains(key.trim().toLowerCase(Locale.ROOT));
    }

    public static String group(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return GROUPS.contains(value) ? value : "";
    }

    public static String savedKey(int id) {
        return "s" + id;
    }

    public static int savedId(String key) {
        if (key == null || key.length() < 2 || key.charAt(0) != 's') {
            return 0;
        }
        try {
            return Integer.parseInt(key.substring(1));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /** A chart the report screen can draw. Anything else is rejected. */
    public static String chart(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return CHARTS.contains(value) ? value : "";
    }

    /** Type-field key used when a saved report groups or filters by a field. */
    public static String fieldKey(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty() || value.length() > 40 || value.charAt(0) < 'a' || value.charAt(0) > 'z') {
            return "";
        }
        for (int i = 1; i < value.length(); i++) {
            char ch = value.charAt(i);
            boolean ok = (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '-';
            if (!ok) {
                return "";
            }
        }
        return value;
    }
}
