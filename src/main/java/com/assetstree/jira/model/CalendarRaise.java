package com.assetstree.jira.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Which calendar days the single Automation rule should see.
 * A day is handed over once. A finished day stays on the calendar and is not handed over again.
 */
public final class CalendarRaise {
    /** How many missed days one run hands to the rule. The next run continues after them. */
    public static final int BATCH = 20;

    private CalendarRaise() {
    }

    /** One occurrence ready for the rule: the series day, the day it is shown on, and that day's clock. */
    public static final class Hand {
        private final String source;
        private final String date;
        private final String start;
        private final String end;

        public Hand(String source, String date, String start, String end) {
            this.source = source == null ? "" : source;
            this.date = date == null ? "" : date;
            this.start = start == null ? "" : start;
            this.end = end == null ? "" : end;
        }

        public String getSource() {
            return source;
        }

        public String getDate() {
            return date;
        }

        public String getStart() {
            return start;
        }

        public String getEnd() {
            return end;
        }
    }

    /**
     * Occurrences from the last handed-over day through today. Today's start time must have arrived.
     * Earlier days ignore the clock, so a stopped server still catches the days it missed.
     * A plan that has never been handed over looks at yesterday and today only.
     * One call returns at most {@link #BATCH} days, oldest first.
     * {@code clock} is {@code HH:mm} in the Jira zone. An empty clock means the day's start has passed.
     */
    public static List<Hand> due(String lastDone, int count, String unit, String moves, String seriesStart,
            String seriesEnd, String completedOn, boolean completed, String raised, LocalDate today, String clock) {
        List<Hand> found = new ArrayList<Hand>();
        if (today == null) {
            return found;
        }
        if (completed && ServiceDue.ONCE.equals(ServiceDue.unit(unit))) {
            return found;
        }
        Set<String> finished = finishedDays(moves, lastDone, unit, completedOn, completed);
        Set<String> sent = tokens(raised, null);
        LocalDate start = catchUpFrom(sent, today);
        for (LocalDate day = start; !day.isAfter(today) && found.size() < BATCH; day = day.plusDays(1)) {
            collect(found, lastDone, count, unit, moves, seriesStart, seriesEnd, finished, sent, day,
                    day.equals(today) ? clock : "");
        }
        return found;
    }

    /** The last handed-over day, or yesterday when nothing has been handed over yet. Not earlier than 400 days. */
    private static LocalDate catchUpFrom(Set<String> sent, LocalDate today) {
        String newest = "";
        if (sent != null) {
            for (String token : sent) {
                if (token != null && token.compareTo(newest) > 0) {
                    newest = token;
                }
            }
        }
        if (newest.isEmpty()) {
            return today.minusDays(1);
        }
        LocalDate last;
        try {
            last = LocalDate.parse(newest);
        } catch (RuntimeException ex) {
            return today.minusDays(1);
        }
        LocalDate horizon = today.minusDays(400);
        return last.isBefore(horizon) ? horizon : last;
    }

    /** Remembers a handed-over series day and drops days more than 400 days behind {@code today}. */
    public static String remember(String raised, String source, LocalDate today) {
        String day = FieldDates.canonical(source);
        if (day == null || day.isEmpty()) {
            return raised == null ? "" : raised;
        }
        String cutoff = today == null ? "" : today.minusDays(400).toString();
        Set<String> kept = tokens(raised, cutoff);
        kept.add(day);
        StringBuilder out = new StringBuilder();
        for (String token : kept) {
            if (out.length() > 0) {
                out.append(',');
            }
            out.append(token);
        }
        return out.toString();
    }

    private static void collect(List<Hand> found, String lastDone, int count, String unit, String moves,
            String seriesStart, String seriesEnd, Set<String> finished, Set<String> sent, LocalDate day, String clock) {
        List<ScheduleOccurrences.Slot> slots = ScheduleOccurrences.onDate(lastDone, count, unit, moves, seriesStart, seriesEnd, day);
        for (int i = 0; i < slots.size(); i++) {
            ScheduleOccurrences.Slot slot = slots.get(i);
            if (slot.getDay() == null) {
                continue;
            }
            String source = slot.getSource();
            String shown = slot.getDay().toString();
            if (finished.contains(source) || finished.contains(shown) || sent.contains(source)) {
                continue;
            }
            if (!clockReached(slot.getStart(), clock)) {
                continue;
            }
            if (found.size() >= BATCH) {
                return;
            }
            found.add(new Hand(source, shown, slot.getStart(), slot.getEnd()));
            sent.add(source);
        }
    }

    static boolean clockReached(String start, String clock) {
        String from = clockText(start);
        if (from.isEmpty()) {
            return true;
        }
        String now = clockText(clock);
        if (now.isEmpty()) {
            return true;
        }
        return now.compareTo(from) >= 0;
    }

    private static String clockText(String value) {
        if (value == null) {
            return "";
        }
        String text = value.trim();
        if (text.length() == 5 && text.charAt(2) == ':') {
            return text;
        }
        return "";
    }

    private static Set<String> finishedDays(String moves, String lastDone, String unit, String completedOn, boolean completed) {
        Set<String> days = new LinkedHashSet<String>();
        List<String> marks = ScheduleDone.days(moves, lastDone, unit, completedOn, completed);
        for (int i = 0; i < marks.size(); i++) {
            String day = marks.get(i);
            if (day != null && !day.isEmpty()) {
                days.add(day);
            }
        }
        return days;
    }

    private static Set<String> tokens(String raised, String cutoff) {
        Set<String> kept = new LinkedHashSet<String>();
        if (raised == null || raised.isEmpty()) {
            return kept;
        }
        String[] parts = raised.split(",");
        for (int i = 0; i < parts.length; i++) {
            String token = FieldDates.canonical(parts[i]);
            if (token == null || token.isEmpty()) {
                continue;
            }
            if (cutoff != null && !cutoff.isEmpty() && token.compareTo(cutoff) < 0) {
                continue;
            }
            kept.add(token);
        }
        return kept;
    }
}
