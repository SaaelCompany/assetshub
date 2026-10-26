package com.assetstree.jira.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * The day a report line should show. A repeating plan keeps the latest
     * open day on or before today, so a missed day that is still on the
     * calendar stays in the line. A removed day is skipped. Otherwise the
     * line uses the soonest later day, including one that was dragged. A
     * finished or skipped one-off, and a series with no day left, has none.
     */
    public static LocalDate nextShown(String lastDone, int count, String unit, String moves, LocalDate today, boolean completed) {
        String clean = ServiceDue.unit(unit);
        LocalDate first = ServiceDue.next(lastDone, count, clean);
        if (first == null || today == null) {
            return null;
        }
        Map<String, String> moved = moveTargets(moves);
        if (ServiceDue.ONCE.equals(clean)) {
            if (completed) {
                return null;
            }
            String target = moved.get(first.toString());
            if ("-".equals(target)) {
                return null;
            }
            LocalDate movedDay = parseDay(target);
            if (movedDay != null) {
                return movedDay;
            }
            return first;
        }
        LocalDate upcoming = first.isBefore(today) ? jumpTo(first, count, clean, today) : first;
        LocalDate overdue = overdueDay(first, upcoming, count, clean, moved, lastDone, today);
        if (overdue != null) {
            return overdue;
        }
        return soonestLater(upcoming, count, clean, moved, lastDone, today);
    }

    /** Latest open day on or before today, when today itself is not that day. */
    private static LocalDate overdueDay(LocalDate first, LocalDate upcoming, int count, String unit,
            Map<String, String> moved, String lastDone, LocalDate today) {
        LocalDate shownUpcoming = displayOf(upcoming, moved);
        if (shownUpcoming != null && shownUpcoming.isEqual(today)) {
            return null;
        }
        LocalDate best = null;
        if (upcoming != null && first != null && upcoming.isAfter(first)) {
            LocalDate prior = stepBack(first, upcoming, count, unit);
            if (prior != null && afterAnchor(prior, lastDone)) {
                LocalDate shown = displayOf(prior, moved);
                if (shown != null && !shown.isAfter(today)) {
                    best = shown;
                }
            }
        }
        String anchor = lastDone == null ? "" : lastDone.trim();
        for (Map.Entry<String, String> entry : moved.entrySet()) {
            String source = entry.getKey();
            String target = entry.getValue();
            if (target == null || "-".equals(target)) {
                continue;
            }
            if (!anchor.isEmpty() && source.compareTo(anchor) <= 0) {
                continue;
            }
            LocalDate shown = parseDay(target);
            if (shown != null && !shown.isAfter(today) && (best == null || shown.isAfter(best))) {
                best = shown;
            }
        }
        return best;
    }

    private static LocalDate soonestLater(LocalDate upcoming, int count, String unit, Map<String, String> moved,
            String lastDone, LocalDate today) {
        LocalDate best = null;
        String anchor = lastDone == null ? "" : lastDone.trim();
        for (Map.Entry<String, String> entry : moved.entrySet()) {
            String source = entry.getKey();
            String target = entry.getValue();
            if (target == null || "-".equals(target)) {
                continue;
            }
            if (!anchor.isEmpty() && source.compareTo(anchor) <= 0) {
                continue;
            }
            LocalDate shown = parseDay(target);
            if (shown != null && !shown.isBefore(today) && (best == null || shown.isBefore(best))) {
                best = shown;
            }
        }
        LocalDate cursor = upcoming;
        int guard = 0;
        while (cursor != null && guard < 500) {
            if (!moved.containsKey(cursor.toString())) {
                if (best == null || cursor.isBefore(best)) {
                    best = cursor;
                }
                break;
            }
            cursor = ServiceDue.next(cursor.toString(), count, unit);
            guard++;
        }
        return best;
    }

    private static LocalDate displayOf(LocalDate seriesDay, Map<String, String> moved) {
        if (seriesDay == null) {
            return null;
        }
        if (!moved.containsKey(seriesDay.toString())) {
            return seriesDay;
        }
        String target = moved.get(seriesDay.toString());
        if ("-".equals(target)) {
            return null;
        }
        return parseDay(target);
    }

    private static boolean afterAnchor(LocalDate day, String lastDone) {
        if (lastDone == null || lastDone.trim().isEmpty()) {
            return true;
        }
        LocalDate anchor = parseDay(lastDone.trim());
        if (anchor == null) {
            return true;
        }
        return day.isAfter(anchor);
    }

    /** The series day immediately before {@code upcoming}. */
    private static LocalDate stepBack(LocalDate first, LocalDate upcoming, int count, String unit) {
        if (first == null || upcoming == null || !upcoming.isAfter(first)) {
            return null;
        }
        if (ServiceDue.DAY.equals(unit) || ServiceDue.WEEK.equals(unit)) {
            int span = ServiceDue.WEEK.equals(unit) ? count * 7 : count;
            if (span < 1) {
                return null;
            }
            LocalDate prior = upcoming.minusDays(span);
            return prior.isBefore(first) ? null : prior;
        }
        LocalDate cursor = first;
        LocalDate prior = null;
        int guard = 0;
        while (cursor != null && cursor.isBefore(upcoming) && guard < 600) {
            prior = cursor;
            cursor = ServiceDue.next(cursor.toString(), count, unit);
            guard++;
        }
        return prior;
    }

    /** Source day to the day it is shown on. {@code -} means that day was removed. */
    private static Map<String, String> moveTargets(String moves) {
        Map<String, String> map = new HashMap<String, String>();
        if (moves == null || moves.isEmpty()) {
            return map;
        }
        String[] parts = moves.split(",");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            int at = part.indexOf('>');
            if (at < 1) {
                continue;
            }
            String source = part.substring(0, at).trim();
            if (source.length() != 10 || source.charAt(4) != '-' || source.charAt(7) != '-') {
                continue;
            }
            String raw = part.substring(at + 1).trim();
            int timed = raw.indexOf('@');
            String day = timed < 0 ? raw : raw.substring(0, timed).trim();
            if ("-".equals(day) || (day.length() == 10 && day.charAt(4) == '-' && day.charAt(7) == '-')) {
                map.put(source, day);
            }
        }
        return map;
    }

    private static LocalDate parseDay(String value) {
        if (value == null || "-".equals(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
