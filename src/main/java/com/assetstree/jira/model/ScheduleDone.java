package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Completed schedule days a report can count, and how those rows are grouped. */
public final class ScheduleDone {
    private static final List<String> AXES = Collections.unmodifiableList(Arrays.asList(
            "kind", "month", "place", "type", "who"));

    private ScheduleDone() {
    }

    /**
     * Days the calendar keeps as finished. A repeating plan stores each marked
     * day, and also the latest anchor when someone has pressed done. A finished
     * one-off contributes its scheduled day.
     */
    public static List<String> days(String moves, String lastDone, String unit, String completedOn, boolean completed) {
        List<String> found = new ArrayList<String>();
        if (moves != null) {
            String[] parts = moves.split(",");
            for (int i = 0; i < parts.length; i++) {
                String piece = parts[i] == null ? "" : parts[i].trim();
                if (!piece.endsWith(">done")) {
                    continue;
                }
                String day = FieldDates.canonical(piece.substring(0, piece.length() - 5));
                if (day != null && !day.isEmpty() && !found.contains(day)) {
                    found.add(day);
                }
            }
        }
        String clean = ServiceDue.unit(unit);
        if (!ServiceDue.ONCE.equals(clean)) {
            String marked = FieldDates.canonical(completedOn);
            String anchor = FieldDates.canonical(lastDone);
            if (marked != null && !marked.isEmpty() && anchor != null && !anchor.isEmpty() && !found.contains(anchor)) {
                found.add(anchor);
            }
        } else if (completed) {
            String anchor = FieldDates.canonical(lastDone);
            if (anchor == null || anchor.isEmpty()) {
                anchor = FieldDates.canonical(completedOn);
            }
            if (anchor != null && !anchor.isEmpty() && !found.contains(anchor)) {
                found.add(anchor);
            }
        }
        Collections.sort(found);
        return found;
    }

    /** Grouping axes of a completed-work report. A bare {@code done} groups by kind. */
    public static List<String> axes(String group) {
        List<String> found = new ArrayList<String>();
        if (group == null || !ReportCatalog.doneReport(group)) {
            return found;
        }
        if ("done".equals(group)) {
            found.add("kind");
            return found;
        }
        String[] parts = group.substring("done+".length()).split("\\+");
        for (int i = 0; i < parts.length; i++) {
            if (AXES.contains(parts[i]) && !found.contains(parts[i])) {
                found.add(parts[i]);
            }
        }
        if (found.isEmpty()) {
            found.add("kind");
        }
        return found;
    }
}
