package com.assetstree.jira.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Counts a person's working days, working hours and leave days inside one month.
 * Leave covers every calendar day of a range. A work day is a pattern day that is not on leave.
 * Hours are the work days times the hours of one day. Overlapping leave is counted once.
 * Weekday bits: bit 0 is Monday, bit 6 is Sunday.
 */
public final class StaffCalendar {
    private StaffCalendar() {
    }

    /** @return int[]{workDays, workHours, leaveDays} */
    public static int[] tally(int weekdays, int hoursPerDay, List<LocalDate[]> leaves, int year, int month) {
        LocalDate cursor = LocalDate.of(year, month, 1);
        LocalDate end = cursor.plusMonths(1);
        int hours = hoursPerDay < 0 ? 0 : Math.min(hoursPerDay, 24);
        int workDays = 0;
        int leaveDays = 0;
        while (cursor.isBefore(end)) {
            if (onLeave(cursor, leaves)) {
                leaveDays++;
            } else if (works(weekdays, cursor)) {
                workDays++;
            }
            cursor = cursor.plusDays(1);
        }
        return new int[] { workDays, workDays * hours, leaveDays };
    }

    public static boolean works(int weekdays, LocalDate date) {
        int bit = 1 << (date.getDayOfWeek().getValue() - 1);
        return (weekdays & bit) != 0;
    }

    private static boolean onLeave(LocalDate day, List<LocalDate[]> leaves) {
        if (leaves == null) {
            return false;
        }
        for (int i = 0; i < leaves.size(); i++) {
            LocalDate[] range = leaves.get(i);
            if (range == null || range.length < 2 || range[0] == null || range[1] == null) {
                continue;
            }
            if (!day.isBefore(range[0]) && !day.isAfter(range[1])) {
                return true;
            }
        }
        return false;
    }
}
