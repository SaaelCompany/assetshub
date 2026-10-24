package com.assetstree.jira.model;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Calendar values stored on a date field. The canonical form is yyyy-MM-dd. */
public final class FieldDates {
    private static final Pattern ISO = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})$");
    private static final Pattern DMY = Pattern.compile("^(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})$");
    private static final Pattern DMY_SHORT = Pattern.compile("^(\\d{1,2})\\.(\\d{1,2})\\.(\\d{2})$");
    private static final int MIN_YEAR = 1900;
    private static final int MAX_YEAR = 2199;

    private FieldDates() {
    }

    /**
     * Empty input stays empty. A real calendar day becomes yyyy-MM-dd.
     * Day-first dates and a two-digit year are accepted. Anything else is null.
     */
    public static String canonical(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return "";
        }
        Matcher iso = ISO.matcher(value);
        if (iso.matches()) {
            return checked(Integer.parseInt(iso.group(1)), Integer.parseInt(iso.group(2)), Integer.parseInt(iso.group(3)));
        }
        Matcher dmy = DMY.matcher(value);
        if (dmy.matches()) {
            return checked(Integer.parseInt(dmy.group(3)), Integer.parseInt(dmy.group(2)), Integer.parseInt(dmy.group(1)));
        }
        Matcher shortYear = DMY_SHORT.matcher(value);
        if (shortYear.matches()) {
            int yy = Integer.parseInt(shortYear.group(3));
            int year = yy <= 69 ? 2000 + yy : 1900 + yy;
            return checked(year, Integer.parseInt(shortYear.group(2)), Integer.parseInt(shortYear.group(1)));
        }
        return null;
    }

    /** A stored day written for a spreadsheet. Day-first is DD.MM.YYYY. */
    public static String display(String raw, boolean dayFirst) {
        String iso = canonical(raw);
        if (iso == null) {
            return raw == null ? "" : raw.trim();
        }
        if (iso.isEmpty() || !dayFirst) {
            return iso;
        }
        return iso.substring(8, 10) + "." + iso.substring(5, 7) + "." + iso.substring(0, 4);
    }

    private static String checked(int year, int month, int day) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            return null;
        }
        try {
            return LocalDate.of(year, month, day).toString();
        } catch (DateTimeException ex) {
            return null;
        }
    }
}
