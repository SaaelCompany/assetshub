package com.assetstree.jira.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Completed schedule days a report can count, and how those rows are grouped. */
public final class ScheduleDone {
    public static final String ONTIME = "ontime";
    public static final String LATE = "late";
    public static final String DONE = "done";
    public static final String OVERDUE = "overdue";
    public static final String EXPECTED = "expected";
    private static final List<String> AXES = Collections.unmodifiableList(Arrays.asList(
            "kind", "month", "place", "type", "who", "asset", "state"));

    private ScheduleDone() {
    }

    /** One finished day and whether it was on time, late, or only recorded as done. */
    public static final class Mark {
        private final String day;
        private final String state;

        public Mark(String day, String state) {
            this.day = day;
            this.state = state;
        }

        public String getDay() {
            return day;
        }

        public String getState() {
            return state;
        }
    }

    /**
     * Days the calendar keeps as finished. A repeating plan stores each marked
     * day, and also the latest anchor when someone has pressed done. A finished
     * one-off contributes its scheduled day.
     */
    public static List<String> days(String moves, String lastDone, String unit, String completedOn, boolean completed) {
        List<String> found = new ArrayList<String>();
        for (Mark mark : marks(moves, lastDone, unit, completedOn, completed)) {
            found.add(mark.getDay());
        }
        return found;
    }

    /** Finished days with {@code ontime}, {@code late} or {@code done}. */
    public static List<Mark> marks(String moves, String lastDone, String unit, String completedOn, boolean completed) {
        Map<String, String> stamps = new LinkedHashMap<String, String>();
        if (moves != null) {
            String[] parts = moves.split(",");
            for (int i = 0; i < parts.length; i++) {
                String piece = parts[i] == null ? "" : parts[i].trim();
                String day = ServiceDue.doneDay(piece);
                if (day.isEmpty() || stamps.containsKey(day)) {
                    continue;
                }
                stamps.put(day, stampOf(piece));
            }
        }
        String clean = ServiceDue.unit(unit);
        String marked = FieldDates.canonical(completedOn);
        if (!ServiceDue.ONCE.equals(clean)) {
            String anchor = FieldDates.canonical(lastDone);
            if (marked != null && !marked.isEmpty() && anchor != null && !anchor.isEmpty()
                    && (!stamps.containsKey(anchor) || stamps.get(anchor).isEmpty())) {
                stamps.put(anchor, marked);
            }
        } else if (completed) {
            String anchor = FieldDates.canonical(lastDone);
            if (anchor == null || anchor.isEmpty()) {
                anchor = marked;
            }
            if (anchor != null && !anchor.isEmpty() && (!stamps.containsKey(anchor) || stamps.get(anchor).isEmpty())) {
                stamps.put(anchor, marked == null ? "" : marked);
            }
        }
        List<Mark> found = new ArrayList<Mark>();
        for (Map.Entry<String, String> entry : stamps.entrySet()) {
            found.add(new Mark(entry.getKey(), stateOf(entry.getKey(), entry.getValue())));
        }
        Collections.sort(found, new java.util.Comparator<Mark>() {
            @Override
            public int compare(Mark left, Mark right) {
                return left.getDay().compareTo(right.getDay());
            }
        });
        return found;
    }

    /** The first series day after today. Empty for a one-off and when none remains. */
    public static String expectedAfter(String lastDone, int count, String unit, LocalDate today) {
        String clean = ServiceDue.unit(unit);
        if (ServiceDue.ONCE.equals(clean) || today == null) {
            return "";
        }
        LocalDate cursor = ServiceDue.next(lastDone, count, clean);
        int guard = 0;
        while (cursor != null && guard < 500) {
            if (cursor.isAfter(today)) {
                return cursor.toString();
            }
            LocalDate next = ServiceDue.next(cursor.toString(), count, clean);
            if (next == null || !next.isAfter(cursor)) {
                return "";
            }
            cursor = next;
            guard++;
        }
        return "";
    }

    private static String stampOf(String part) {
        int at = part.indexOf('>');
        if (at < 0) {
            return "";
        }
        String target = part.substring(at + 1).trim();
        if (!target.startsWith("done@")) {
            return "";
        }
        String stamp = FieldDates.canonical(target.substring(5));
        return stamp == null ? "" : stamp;
    }

    private static String stateOf(String day, String stamp) {
        if (stamp == null || stamp.isEmpty()) {
            return DONE;
        }
        return stamp.compareTo(day) > 0 ? LATE : ONTIME;
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
