package com.assetstree.jira.model;

/**
 * Whether a project status is a counter on the summary.
 * Mode 0 is the column default on rows created before the flag existed,
 * so the three built-in counters stay visible until someone chooses.
 */
public final class StatusSummary {
    public static final int UNSET = 0;
    public static final int SHOW = 1;
    public static final int HIDE = 2;

    private StatusSummary() {
    }

    public static boolean shows(int mode, String statusKey) {
        if (mode == SHOW) {
            return true;
        }
        if (mode == HIDE) {
            return false;
        }
        return Statuses.REPAIR.equals(statusKey)
                || Statuses.MAINTENANCE.equals(statusKey)
                || Statuses.WRITTEN_OFF.equals(statusKey);
    }

    public static int seedMode(String statusKey) {
        return shows(UNSET, statusKey) ? SHOW : UNSET;
    }

    /** A new status is shown unless the form explicitly leaves the box clear. */
    public static int createMode(Boolean inSummary) {
        if (inSummary == null || inSummary.booleanValue()) {
            return SHOW;
        }
        return HIDE;
    }

    /** A missing value keeps the stored mode, so a rename does not clear the flag. */
    public static int updateMode(Boolean inSummary, int current) {
        if (inSummary == null) {
            return current;
        }
        return inSummary.booleanValue() ? SHOW : HIDE;
    }
}
