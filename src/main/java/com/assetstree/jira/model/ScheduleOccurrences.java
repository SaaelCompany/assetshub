package com.assetstree.jira.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Dates of one plan that fall inside a calendar month. */
public final class ScheduleOccurrences {
    private ScheduleOccurrences() {
    }

    public static List<LocalDate> inMonth(String lastDone, int count, String unit, int year, int month) {
        String clean = ServiceDue.unit(unit);
        if (month < 1 || month > 12) {
            return Collections.emptyList();
        }
        if (ServiceDue.ONCE.equals(clean)) {
            String iso = FieldDates.canonical(lastDone);
            if (iso == null || iso.isEmpty()) {
                return Collections.emptyList();
            }
            LocalDate day = LocalDate.parse(iso);
            if (day.getYear() == year && day.getMonthValue() == month) {
                return Collections.singletonList(day);
            }
            return Collections.emptyList();
        }
        LocalDate first = ServiceDue.next(lastDone, count, clean);
        if (first == null) {
            return Collections.emptyList();
        }
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        LocalDate cursor = jumpTo(first, count, clean, start);
        List<LocalDate> found = new ArrayList<LocalDate>();
        int guard = 0;
        while (cursor != null && !cursor.isAfter(end) && guard < 40) {
            if (!cursor.isBefore(start)) {
                found.add(cursor);
            }
            cursor = ServiceDue.next(cursor.toString(), count, clean);
            guard++;
        }
        return found;
    }

    private static LocalDate jumpTo(LocalDate first, int count, String unit, LocalDate start) {
        if (!first.isBefore(start)) {
            return first;
        }
        if (ServiceDue.DAY.equals(unit) || ServiceDue.WEEK.equals(unit)) {
            int span = ServiceDue.WEEK.equals(unit) ? count * 7 : count;
            long steps = ChronoUnit.DAYS.between(first, start) / span;
            LocalDate jumped = first.plusDays(steps * span);
            if (jumped.isBefore(start)) {
                jumped = jumped.plusDays(span);
            }
            return jumped;
        }
        if (ServiceDue.YEAR.equals(unit)) {
            long steps = ChronoUnit.YEARS.between(first, start) / count;
            LocalDate jumped = first.plusYears(steps * count);
            int guard = 0;
            while (jumped.isBefore(start) && guard < 24) {
                jumped = jumped.plusYears(count);
                guard++;
            }
            return jumped;
        }
        long steps = ChronoUnit.MONTHS.between(first.withDayOfMonth(1), start.withDayOfMonth(1)) / count;
        LocalDate jumped = first.plusMonths(steps * count);
        int guard = 0;
        while (jumped.isBefore(start) && guard < 24) {
            jumped = jumped.plusMonths(count);
            guard++;
        }
        return jumped;
    }
}
