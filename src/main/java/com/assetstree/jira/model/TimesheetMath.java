package com.assetstree.jira.model;

/**
 * How one timesheet mark turns a person's hours into a day total.
 * Factor 0 leaves the day out, 1 keeps the hours, 2 doubles them.
 * A night mark without an explicit factor doubles. Zero hours stay zero.
 */
public final class TimesheetMath {
    private TimesheetMath() {
    }

    public static int factor(String id, int hours, Integer explicit) {
        if (explicit != null && (explicit.intValue() == 0 || explicit.intValue() == 1 || explicit.intValue() == 2)) {
            return explicit.intValue();
        }
        if (hours <= 0) {
            return 0;
        }
        if ("night".equals(id)) {
            return 2;
        }
        return 1;
    }

    public static int hours(int personHours, int factor) {
        if (personHours <= 0 || factor <= 0) {
            return 0;
        }
        int scale = factor >= 2 ? 2 : 1;
        long total = (long) personHours * scale;
        if (total > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) total;
    }
}
