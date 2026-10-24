package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Optional project modules. Empty means every module is off. Turning one off keeps its data.
 */
public final class ProjectModules {
    public static final String SERVICE = "service";
    public static final String MAP = "map";
    public static final String REPORTS = "reports";
    public static final String LIFE = "life";
    public static final String PORTAL = "portal";
    public static final String CARD = "card";

    private static final List<String> KNOWN = Collections.unmodifiableList(Arrays.asList(
            SERVICE, MAP, REPORTS, LIFE, PORTAL, CARD));

    private ProjectModules() {
    }

    public static List<String> known() {
        return KNOWN;
    }

    public static List<String> clean(List<String> raw) {
        List<String> present = new ArrayList<String>();
        if (raw != null) {
            for (String item : raw) {
                if (item == null) {
                    continue;
                }
                String key = item.trim().toLowerCase(Locale.ROOT);
                if (KNOWN.contains(key) && !present.contains(key)) {
                    present.add(key);
                }
            }
        }
        List<String> kept = new ArrayList<String>();
        for (String key : KNOWN) {
            if (present.contains(key)) {
                kept.add(key);
            }
        }
        return kept;
    }

    public static String join(List<String> raw) {
        StringBuilder stored = new StringBuilder();
        for (String key : clean(raw)) {
            if (stored.length() > 0) {
                stored.append(',');
            }
            stored.append(key);
        }
        return stored.toString();
    }

    public static List<String> split(String stored) {
        if (stored == null || stored.trim().isEmpty()) {
            return new ArrayList<String>();
        }
        return clean(Arrays.asList(stored.split(",")));
    }

    public static boolean has(String stored, String key) {
        return key != null && split(stored).contains(key.trim().toLowerCase(Locale.ROOT));
    }
}
