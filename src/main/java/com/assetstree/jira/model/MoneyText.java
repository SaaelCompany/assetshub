package com.assetstree.jira.model;

/** Rubles typed by a person, stored as kopecks. */
public final class MoneyText {
    private MoneyText() {
    }

    public static Long parse(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim().replace(" ", "").replace("\u00a0", "");
        if (text.isEmpty()) {
            return Long.valueOf(0L);
        }
        text = text.replace(',', '.');
        if (!text.matches("\\d{1,12}(\\.\\d{1,2})?")) {
            return null;
        }
        int dot = text.indexOf('.');
        String rubles = dot < 0 ? text : text.substring(0, dot);
        String fraction = dot < 0 ? "" : text.substring(dot + 1);
        if (fraction.length() == 1) {
            fraction = fraction + "0";
        }
        long minor = Long.parseLong(rubles) * 100L;
        if (!fraction.isEmpty()) {
            minor += Long.parseLong(fraction);
        }
        return Long.valueOf(minor);
    }

    public static String format(long minor) {
        long rubles = minor / 100L;
        long kopecks = Math.abs(minor % 100L);
        if (kopecks == 0L) {
            return Long.toString(rubles);
        }
        return rubles + (kopecks < 10L ? ".0" : ".") + kopecks;
    }
}
