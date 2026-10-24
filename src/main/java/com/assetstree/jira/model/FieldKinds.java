package com.assetstree.jira.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class FieldKinds {
    public static final String TEXT = "text";
    public static final String NUMBER = "number";
    public static final String USER = "user";
    public static final String TEXTAREA = "textarea";
    public static final String DATE = "date";
    public static final String SELECT = "select";
    public static final String SELECTS = "selects";
    public static final String CHECKS = "checks";
    public static final String RADIO = "radio";
    public static final String LABELS = "labels";
    public static final String URL = "url";
    public static final String VERSION = "version";

    public static final Set<String> ALL = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            TEXT, NUMBER, USER, TEXTAREA, DATE, SELECT, SELECTS, CHECKS, RADIO, LABELS, URL, VERSION
    )));

    private FieldKinds() {
    }

    public static boolean isKind(String kind) {
        return kind != null && ALL.contains(kind);
    }
}
