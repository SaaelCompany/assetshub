package com.assetstree.jira.model;

/** Screen pages stay short. A file for a medium fleet keeps up to {@link #EXPORT_MAX} rows. */
public final class ReportLines {
    public static final int PAGE = 300;
    public static final int EXPORT_MAX = 50000;

    private ReportLines() {
    }

    public static boolean cutsExport(int size) {
        return size > EXPORT_MAX;
    }
}
