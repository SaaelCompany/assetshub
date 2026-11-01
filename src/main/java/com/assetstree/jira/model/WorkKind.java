package com.assetstree.jira.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** A work kind stored on a plan. Built-in ids cover the usual cases; a project may add its own. */
public final class WorkKind {
    public static final String TO = "to";
    public static final String KTS = "kts";
    public static final String VERIFY = "verify";
    public static final String STAFF = "staff";
    public static final String EVENT = "event";
    public static final String UPDATE = "update";

    private static final Pattern SLUG = Pattern.compile("[a-z][a-z0-9]{0,15}");
    private static final List<String> ALL = Collections.unmodifiableList(
            Arrays.asList(TO, KTS, VERIFY, STAFF, EVENT, UPDATE));

    private WorkKind() {
    }

    public static List<String> all() {
        return ALL;
    }

    /** A stored id: a short lowercase slug, or empty when the text is not one. */
    public static String id(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return SLUG.matcher(value).matches() ? value : "";
    }

    /** Blank becomes maintenance. Anything that is not a slug is rejected as empty. */
    public static String clean(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return TO;
        }
        return id(raw);
    }
}
