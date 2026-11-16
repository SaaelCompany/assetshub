package com.assetstree.jira.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Scheduled maintenance days inside a report window, and whether the unit kept them. */
public final class PtoWindow {
    public static final String ONTIME = ScheduleDone.ONTIME;
    public static final String LATE = ScheduleDone.LATE;
    public static final String EXPECTED = ScheduleDone.EXPECTED;

    /** One maintenance day and its outcome. */
    public static final class Item {
        private final String day;
        private final String state;
        private final String work;

        public Item(String day, String state, String work) {
            this.day = day == null ? "" : day;
            this.state = state == null ? "" : state;
            this.work = work == null ? "" : work;
        }

        public String getDay() {
            return day;
        }

        public String getState() {
            return state;
        }

        public String getWork() {
            return work;
        }
    }

    private PtoWindow() {
    }

    /**
     * Days of one plan that fall in the window. A finished day keeps its stored outcome.
     * A due day before today with no mark is overdue. A due day from today on is still expected.
     */
    public static List<Item> items(String moves, String lastDone, int count, String unit, String completedOn,
            boolean completed, String work, LocalDate start, LocalDate end, LocalDate today) {
        List<Item> found = new ArrayList<Item>();
        if (start == null || end == null || today == null || start.isAfter(end)) {
            return found;
        }
        Map<String, String> marks = new LinkedHashMap<String, String>();
        List<ScheduleDone.Mark> stored = ScheduleDone.marks(moves, lastDone, unit, completedOn, completed);
        for (int i = 0; i < stored.size(); i++) {
            ScheduleDone.Mark mark = stored.get(i);
            LocalDate day = parse(mark.getDay());
            if (day != null && !day.isBefore(start) && !day.isAfter(end)) {
                marks.put(mark.getDay(), mark.getState());
            }
        }
        LocalDate cursor = ServiceDue.next(lastDone, count, unit);
        int guard = 0;
        while (cursor != null && !cursor.isAfter(end) && guard < 400) {
            if (!cursor.isBefore(start)) {
                String day = cursor.toString();
                String state = marks.remove(day);
                if (state == null) {
                    state = cursor.isBefore(today) ? ScheduleDone.OVERDUE : ScheduleDone.EXPECTED;
                }
                found.add(new Item(day, state, work));
            }
            LocalDate next = ServiceDue.next(cursor.toString(), count, unit);
            if (next == null || !next.isAfter(cursor)) {
                break;
            }
            cursor = next;
            guard++;
        }
        for (Map.Entry<String, String> entry : marks.entrySet()) {
            found.add(new Item(entry.getKey(), entry.getValue(), work));
        }
        Collections.sort(found, new Comparator<Item>() {
            @Override
            public int compare(Item left, Item right) {
                return left.day.compareTo(right.day);
            }
        });
        return found;
    }

    /** {@code late} when any day was late or overdue, {@code ontime} when every finished day was on time, otherwise {@code expected}. */
    public static String rollup(List<Item> items) {
        boolean finished = false;
        boolean waiting = false;
        if (items == null) {
            return "";
        }
        for (int i = 0; i < items.size(); i++) {
            String state = items.get(i).state;
            if (ScheduleDone.LATE.equals(state) || ScheduleDone.OVERDUE.equals(state)) {
                return LATE;
            }
            if (ScheduleDone.ONTIME.equals(state) || ScheduleDone.DONE.equals(state)) {
                finished = true;
            } else if (ScheduleDone.EXPECTED.equals(state)) {
                waiting = true;
            }
        }
        if (finished) {
            return ONTIME;
        }
        return waiting ? EXPECTED : "";
    }

    private static LocalDate parse(String day) {
        String iso = FieldDates.canonical(day);
        if (iso == null || iso.isEmpty()) {
            return null;
        }
        return LocalDate.parse(iso);
    }
}
