package com.assetstree.jira.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** The three kinds of work a maintenance calendar can schedule. */
public final class WorkKind {
    public static final String TO = "to";
    public static final String KTS = "kts";
    public static final String VERIFY = "verify";

    private static final List<String> ALL = Collections.unmodifiableList(Arrays.asList(TO, KTS, VERIFY));

    private WorkKind() {
    }

    public static List<String> all() {
        return ALL;
    }

    /** Blank becomes maintenance. An unknown value is rejected as empty. */
    public static String clean(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return TO;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return ALL.contains(value) ? value : "";
    }
}
