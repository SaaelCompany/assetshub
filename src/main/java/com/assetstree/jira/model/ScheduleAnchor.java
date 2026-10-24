package com.assetstree.jira.model;

import java.time.LocalDate;

/**
 * The calendar stores the first day the work should appear.
 * Repeating plans keep the previous day in {@code lastDone}, because the next due date is lastDone plus the interval.
 * A one-off stores that day itself.
 */
public final class ScheduleAnchor {
    private ScheduleAnchor() {
    }

    public static String before(String firstDate, int count, String unit) {
        String clean = ServiceDue.unit(unit);
        String iso = FieldDates.canonical(firstDate);
        if (iso == null || iso.isEmpty() || ServiceDue.ONCE.equals(clean) || !ServiceDue.countAllowed(count, clean)) {
            return firstDate == null ? "" : firstDate;
        }
        LocalDate first = LocalDate.parse(iso);
        if (ServiceDue.DAY.equals(clean)) {
            return first.minusDays(count).toString();
        }
        if (ServiceDue.WEEK.equals(clean)) {
            return first.minusWeeks(count).toString();
        }
        if (ServiceDue.YEAR.equals(clean)) {
            return first.minusYears(count).toString();
        }
        return first.minusMonths(count).toString();
    }
}
