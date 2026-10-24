package com.assetstree.jira.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Statuses {
    public static final String IN_STOCK = "in_stock";
    public static final String IN_USE = "in_use";
    public static final String REPAIR = "repair";
    public static final String RESERVE = "reserve";
    public static final String MAINTENANCE = "maintenance";
    public static final String WRITTEN_OFF = "written_off";

    public static final String ACTIVE = "active";
    public static final String INACTIVE = "inactive";
    public static final String RETIRED = "retired";

    public static final List<String> ORDER = Collections.unmodifiableList(Arrays.asList(
            IN_STOCK, IN_USE, REPAIR, RESERVE, MAINTENANCE, WRITTEN_OFF
    ));

    public static final Set<String> ALL = Collections.unmodifiableSet(new HashSet<String>(ORDER));

    private Statuses() {
    }

    public static String canonical(String status) {
        if (status == null || status.trim().isEmpty()) {
            return IN_USE;
        }
        if (ACTIVE.equals(status)) {
            return IN_USE;
        }
        if (INACTIVE.equals(status)) {
            return RESERVE;
        }
        if (RETIRED.equals(status)) {
            return WRITTEN_OFF;
        }
        return status;
    }

    public static boolean allowed(String status) {
        return status != null && ALL.contains(canonical(status));
    }
}
