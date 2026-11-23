package com.assetstree.jira.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /** One day a work report should count, with the clock shown on the calendar. */
    public static final class Shown {
        private final LocalDate day;
        private final String start;
        private final String end;

        public Shown(LocalDate day, String start, String end) {
            this.day = day;
            this.start = start == null ? "" : start;
            this.end = end == null ? "" : end;
        }

        public LocalDate getDay() {
            return day;
        }

        public String getStart() {
            return start;
        }

        public String getEnd() {
            return end;
        }
    }

    /**
     * The latest day {@link #reportDays} would count. A repeating plan keeps
     * the latest open day on or before today. Otherwise it is the soonest
     * later day.
     */
    public static LocalDate nextShown(String lastDone, int count, String unit, String moves, LocalDate today, boolean completed) {
        List<Shown> days = reportDays(lastDone, count, unit, moves, "", "", today, completed);
        if (days.isEmpty()) {
            return null;
        }
        return days.get(days.size() - 1).getDay();
    }

    /**
     * Every open day a work report should count. A repeating plan lists each
     * displayed day on or before today, so several missed months are separate
     * lines. When none remain, the list holds the soonest later day. A removed
     * day is skipped. A finished or skipped one-off, and a series with no day
     * left, is empty. The clock is the series time, or the time of a drag.
     */
    public static List<Shown> reportDays(String lastDone, int count, String unit, String moves, String seriesStart,
            String seriesEnd, LocalDate today, boolean completed) {
        List<Shown> found = new ArrayList<Shown>();
        String clean = ServiceDue.unit(unit);
        LocalDate first = ServiceDue.next(lastDone, count, clean);
        if (first == null || today == null) {
            return found;
        }
        Map<String, String> moved = moveTargets(moves);
        if (ServiceDue.ONCE.equals(clean)) {
            if (completed) {
                return found;
            }
            String target = moved.get(first.toString());
            if (target != null && "-".equals(dayOf(target))) {
                return found;
            }
            LocalDate day = target == null ? first : parseDay(dayOf(target));
            if (day == null) {
                return found;
            }
            found.add(shown(day, target, seriesStart, seriesEnd));
            return found;
        }
        List<Shown> open = new ArrayList<Shown>();
        Set<String> seen = new HashSet<String>();
        LocalDate cursor = first;
        int guard = 0;
        while (cursor != null && guard < 20000) {
            if (afterAnchor(cursor, lastDone)) {
                String target = moved.get(cursor.toString());
                LocalDate shown = displayOf(cursor, moved);
                if (shown != null && !shown.isAfter(today)) {
                    addShown(open, seen, shown, target, seriesStart, seriesEnd);
                }
            }
            if (!cursor.isBefore(today)) {
                break;
            }
            LocalDate next = ServiceDue.next(cursor.toString(), count, clean);
            if (next == null || !next.isAfter(cursor)) {
                break;
            }
            cursor = next;
            guard++;
        }
        String anchor = lastDone == null ? "" : lastDone.trim();
        for (Map.Entry<String, String> entry : moved.entrySet()) {
            String source = entry.getKey();
            String target = entry.getValue();
            if (target == null || "-".equals(dayOf(target))) {
                continue;
            }
            if (!anchor.isEmpty() && source.compareTo(anchor) <= 0) {
                continue;
            }
            LocalDate shown = parseDay(dayOf(target));
            if (shown != null && !shown.isAfter(today)) {
                addShown(open, seen, shown, target, seriesStart, seriesEnd);
            }
        }
        if (!open.isEmpty()) {
            Collections.sort(open, new Comparator<Shown>() {
                @Override
                public int compare(Shown left, Shown right) {
                    return left.getDay().compareTo(right.getDay());
                }
            });
            return open;
        }
        LocalDate upcoming = first.isBefore(today) ? jumpTo(first, count, clean, today) : first;
        Shown later = soonestLater(upcoming, count, clean, moved, lastDone, today, seriesStart, seriesEnd);
        if (later != null) {
            found.add(later);
        }
        return found;
    }

    private static void addShown(List<Shown> open, Set<String> seen, LocalDate day, String target, String seriesStart, String seriesEnd) {
        if (day == null || !seen.add(day.toString())) {
            return;
        }
        open.add(shown(day, target, seriesStart, seriesEnd));
    }

    private static Shown shown(LocalDate day, String target, String seriesStart, String seriesEnd) {
        String[] clock = clockOf(target, seriesStart, seriesEnd);
        return new Shown(day, clock[0], clock[1]);
    }

    private static Shown soonestLater(LocalDate upcoming, int count, String unit, Map<String, String> moved,
            String lastDone, LocalDate today, String seriesStart, String seriesEnd) {
        LocalDate best = null;
        String bestTarget = null;
        boolean fromMove = false;
        String anchor = lastDone == null ? "" : lastDone.trim();
        for (Map.Entry<String, String> entry : moved.entrySet()) {
            String source = entry.getKey();
            String target = entry.getValue();
            if (target == null || "-".equals(dayOf(target))) {
                continue;
            }
            if (!anchor.isEmpty() && source.compareTo(anchor) <= 0) {
                continue;
            }
            LocalDate shown = parseDay(dayOf(target));
            if (shown != null && !shown.isBefore(today) && (best == null || shown.isBefore(best))) {
                best = shown;
                bestTarget = target;
                fromMove = true;
            }
        }
        LocalDate cursor = upcoming;
        int guard = 0;
        while (cursor != null && guard < 500) {
            if (!moved.containsKey(cursor.toString())) {
                if (best == null || cursor.isBefore(best)) {
                    best = cursor;
                    bestTarget = null;
                    fromMove = false;
                }
                break;
            }
            cursor = ServiceDue.next(cursor.toString(), count, unit);
            guard++;
        }
        if (best == null) {
            return null;
        }
        return shown(best, fromMove ? bestTarget : null, seriesStart, seriesEnd);
    }

    private static LocalDate displayOf(LocalDate seriesDay, Map<String, String> moved) {
        if (seriesDay == null) {
            return null;
        }
        if (!moved.containsKey(seriesDay.toString())) {
            return seriesDay;
        }
        String target = moved.get(seriesDay.toString());
        if ("-".equals(dayOf(target))) {
            return null;
        }
        return parseDay(dayOf(target));
    }

    /** Series clock, or the clock stored on a drag. {@code @} with {@code -} clears the time. */
    private static String[] clockOf(String target, String seriesStart, String seriesEnd) {
        String start = seriesStart == null ? "" : seriesStart.trim();
        String end = seriesEnd == null ? "" : seriesEnd.trim();
        if (target == null) {
            return new String[] { start, end };
        }
        int at = target.indexOf('@');
        if (at < 0) {
            return new String[] { start, end };
        }
        String span = target.substring(at + 1).trim();
        if (span.isEmpty() || "-".equals(span)) {
            return new String[] { "", "" };
        }
        int dash = span.indexOf('-');
        if (dash < 0) {
            return new String[] { span, "" };
        }
        return new String[] { span.substring(0, dash).trim(), span.substring(dash + 1).trim() };
    }

    private static String dayOf(String target) {
        if (target == null) {
            return "";
        }
        int at = target.indexOf('@');
        return at < 0 ? target : target.substring(0, at).trim();
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
            String day = dayOf(raw);
            if ("-".equals(day) || (day.length() == 10 && day.charAt(4) == '-' && day.charAt(7) == '-')) {
                map.put(source, raw);
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
