package com.assetstree.jira.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Straight-line depreciation in minor currency units. */
public final class Depreciation {
    private Depreciation() {
    }

    public static long monthlyMinor(long priceMinor, long residualMinor, int lifeMonths) {
        if (lifeMonths <= 0 || priceMinor <= residualMinor) {
            return 0L;
        }
        return (priceMinor - residualMinor) / lifeMonths;
    }

    public static int elapsedMonths(LocalDate purchased, LocalDate today) {
        if (purchased == null || today == null || today.isBefore(purchased)) {
            return 0;
        }
        long months = ChronoUnit.MONTHS.between(purchased, today);
        if (months > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) months;
    }

    public static long bookMinor(long priceMinor, long residualMinor, int lifeMonths, int elapsed) {
        if (priceMinor <= 0L) {
            return 0L;
        }
        long lost = monthlyMinor(priceMinor, residualMinor, lifeMonths) * Math.max(0, elapsed);
        long book = priceMinor - lost;
        long floor = Math.max(0L, residualMinor);
        return book < floor ? floor : book;
    }

    public static boolean warrantyActive(LocalDate until, LocalDate today) {
        return until != null && today != null && !today.isAfter(until);
    }
}
