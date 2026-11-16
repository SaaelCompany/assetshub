package com.assetstree.jira.model;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/** Inclusive calendar range a fault report counts. An empty bound uses the month that contains today. */
public final class ReportWindow {
    public static final int MAX_DAYS = 3660;
    private final LocalDate start;
    private final LocalDate end;
    private final long startMillis;
    private final long endMillis;

    private ReportWindow(LocalDate start, LocalDate end, long startMillis, long endMillis) {
        this.start = start;
        this.end = end;
        this.startMillis = startMillis;
        this.endMillis = endMillis;
    }

    public static ReportWindow of(String startRaw, String endRaw, LocalDate today, ZoneId zone) {
        LocalDate day = today == null ? LocalDate.now(zone == null ? ZoneId.systemDefault() : zone) : today;
        ZoneId clock = zone == null ? ZoneId.systemDefault() : zone;
        LocalDate end = date(endRaw, day);
        LocalDate start = date(startRaw, end.withDayOfMonth(1));
        if (start.isAfter(end)) {
            LocalDate swap = start;
            start = end;
            end = swap;
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_DAYS) {
            start = end.minusDays(MAX_DAYS);
        }
        long from = start.atStartOfDay(clock).toInstant().toEpochMilli();
        long to = end.plusDays(1).atStartOfDay(clock).toInstant().toEpochMilli();
        return new ReportWindow(start, end, from, to);
    }

    public LocalDate getStart() {
        return start;
    }

    public LocalDate getEnd() {
        return end;
    }

    /** First millisecond of the start day. */
    public long getStartMillis() {
        return startMillis;
    }

    /** First millisecond after the end day. */
    public long getEndMillis() {
        return endMillis;
    }

    public boolean contains(LocalDate day) {
        return day != null && !day.isBefore(start) && !day.isAfter(end);
    }

    public boolean containsMillis(long at) {
        return at >= startMillis && at < endMillis;
    }

    private static LocalDate date(String raw, LocalDate fallback) {
        String iso = FieldDates.canonical(raw);
        if (iso == null || iso.isEmpty()) {
            return fallback;
        }
        return LocalDate.parse(iso);
    }
}
