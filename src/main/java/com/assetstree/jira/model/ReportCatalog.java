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
    public static final String SERVICE = "service";
    public static final String WARRANTY = "warranty";
    public static final String COST = "cost";
    public static final String RECEIPTS = "receipts";
    public static final String AGE = "age";
    public static final String WRITEOFF = "writeoff";
    public static final String WORK = "work";

    private static final List<String> STANDARD = Collections.unmodifiableList(Arrays.asList(
            STATUS, TYPE, PLACE, HOLDER, RECEIPTS, COST, WARRANTY, AGE, WRITEOFF, WORK));
    private static final List<String> GROUPS = Collections.unmodifiableList(Arrays.asList(
            STATUS, TYPE, PLACE, HOLDER));

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

    /** Donut for a status split, columns for everything else, unless a saved report chose otherwise. */
    public static String chart(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if ("donut".equals(value) || "bar".equals(value)) {
            return value;
        }
        return "";
    }
}
