package com.assetstree.jira.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;

/** The real moment a status changed, which can be earlier than the request. */
public final class StatusMoment {
    static final long SKEW_MS = 120000L;
    static final long OLDEST_MS = 946684800000L;

    private static final DateTimeFormatter[] LOCAL = new DateTimeFormatter[] {
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    };
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private StatusMoment() {
    }

    /** Null means the current moment. A future instant and anything before 2000 are rejected. */
    public static Date at(Long millis, Date now) {
        Date clock = now == null ? new Date() : now;
        if (millis == null) {
            return clock;
        }
        long value = millis.longValue();
        if (value > clock.getTime() + SKEW_MS || value < OLDEST_MS) {
            throw new AssetException(400, "asset-tree.error.statusWhen");
        }
        return new Date(value);
    }

    /** A date field, a timestamp, or a written date. Null when the value is empty or not a date. */
    public static Date fromValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return new Date(((Date) value).getTime());
        }
        if (value instanceof Calendar) {
            return new Date(((Calendar) value).getTimeInMillis());
        }
        if (value instanceof Iterable) {
            for (Object item : (Iterable<?>) value) {
                Date found = fromValue(item);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return new Date(Instant.parse(text).toEpochMilli());
        } catch (DateTimeParseException ignored) {
            // A local date has no zone, so it is read in the server zone.
        }
        int dot = text.lastIndexOf('.');
        if (dot > 10 && text.indexOf('-') == 4) {
            Date head = localDate(text.substring(0, dot));
            if (head != null) {
                return head;
            }
        }
        return localDate(text);
    }

    private static Date localDate(String text) {
        ZoneId zone = ZoneId.systemDefault();
        for (int i = 0; i < LOCAL.length; i++) {
            try {
                LocalDateTime local = LocalDateTime.parse(text, LOCAL[i]);
                return new Date(local.atZone(zone).toInstant().toEpochMilli());
            } catch (DateTimeParseException ignored) {
                // try the next pattern
            }
        }
        try {
            LocalDate day = LocalDate.parse(text, DAY);
            return new Date(day.atStartOfDay(zone).toInstant().toEpochMilli());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
