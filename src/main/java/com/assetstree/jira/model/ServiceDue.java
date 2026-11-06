package com.assetstree.jira.model;

import java.time.LocalDate;
import java.util.Locale;

/** Next calendar day for a repeating service such as maintenance or verification. */
public final class ServiceDue {
    public static final String ONCE = "once";
    public static final String DAY = "day";
    public static final String WEEK = "week";
    public static final String MONTH = "month";
    public static final String YEAR = "year";
    public static final int MAX_DAYS = 3650;
    public static final int MAX_WEEKS = 520;
    public static final int MAX_MONTHS = 120;
    public static final int MAX_YEARS = 30;

    private ServiceDue() {
    }

    public static String unit(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (ONCE.equals(value) || DAY.equals(value) || WEEK.equals(value) || MONTH.equals(value) || YEAR.equals(value)) {
            return value;
        }
        return "";
    }

    public static boolean countAllowed(int count, String unit) {
        if (ONCE.equals(unit)) {
            return count == 1;
        }
        if (DAY.equals(unit)) {
            return count >= 1 && count <= MAX_DAYS;
        }
        if (WEEK.equals(unit)) {
            return count >= 1 && count <= MAX_WEEKS;
        }
        if (MONTH.equals(unit)) {
            return count >= 1 && count <= MAX_MONTHS;
        }
        if (YEAR.equals(unit)) {
            return count >= 1 && count <= MAX_YEARS;
        }
        return false;
    }

    /** The day the service is due again, or the appointment itself when it does not repeat. */
    public static LocalDate next(String lastDone, int count, String unit) {
        String iso = FieldDates.canonical(lastDone);
        if (iso == null || iso.isEmpty() || !countAllowed(count, unit)) {
            return null;
        }
        LocalDate last = LocalDate.parse(iso);
        if (ONCE.equals(unit)) {
            return last;
        }
        if (DAY.equals(unit)) {
            return last.plusDays(count);
        }
        if (WEEK.equals(unit)) {
            return last.plusWeeks(count);
        }
        if (YEAR.equals(unit)) {
            return last.plusYears(count);
        }
        return last.plusMonths(count);
    }

    public static boolean reached(LocalDate next, LocalDate today) {
        return next != null && today != null && !next.isAfter(today);
    }

    /** Keeps a finished occurrence on the calendar as {@code yyyy-mm-dd>done} inside the move list. */
    public static String rememberDone(String moves, String day) {
        String iso = FieldDates.canonical(day);
        if (iso == null || iso.isEmpty()) {
            return moves == null ? "" : moves;
        }
        return keepDone(moves, mark(iso));
    }

    /** A replacement move list keeps the finished days that were already stored. */
    public static String keepDone(String replacement, String previous) {
        StringBuilder out = new StringBuilder();
        appendMoves(out, replacement, false);
        appendMoves(out, previous, true);
        return out.toString();
    }

    private static String mark(String iso) {
        return iso + ">done";
    }

    private static void appendMoves(StringBuilder out, String raw, boolean doneOnly) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        String[] parts = raw.split(",");
        int done = countDone(out.toString());
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i] == null ? "" : parts[i].trim();
            if (part.isEmpty() || containsPart(out, part)) {
                continue;
            }
            boolean finished = part.endsWith(">done") && FieldDates.canonical(part.substring(0, part.length() - 5)) != null;
            if (doneOnly && !finished) {
                continue;
            }
            if (finished) {
                if (done >= 60) {
                    continue;
                }
                done++;
            }
            if (out.length() > 0) {
                out.append(',');
            }
            out.append(part);
        }
    }

    private static int countDone(String raw) {
        if (raw == null || raw.isEmpty()) {
            return 0;
        }
        int count = 0;
        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].endsWith(">done")) {
                count++;
            }
        }
        return count;
    }

    private static boolean containsPart(StringBuilder stored, String part) {
        String text = stored.toString();
        if (text.isEmpty()) {
            return false;
        }
        String[] parts = text.split(",");
        for (int i = 0; i < parts.length; i++) {
            if (part.equals(parts[i])) {
                return true;
            }
        }
        return false;
    }
}
