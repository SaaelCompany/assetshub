package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Time an asset spent in the downtime statuses, clipped to a report window. */
public final class RepairTime {
    public static final long DAY = 24L * 60L * 60L * 1000L;

    /** One status write. {@code next} is the status after the write. */
    public static final class Change {
        private final long at;
        private final String next;

        public Change(long at, String next) {
            this.at = at;
            this.next = Statuses.canonical(next);
        }

        public long getAt() {
            return at;
        }

        public String getNext() {
            return next;
        }
    }

    /** A stay in a downtime status. {@code start} and {@code end} are the real edges. The clipped pair is inside the window. */
    public static final class Span {
        private final long start;
        private final long end;
        private final long clippedStart;
        private final long clippedEnd;

        Span(long start, long end, long clippedStart, long clippedEnd) {
            this.start = start;
            this.end = end;
            this.clippedStart = clippedStart;
            this.clippedEnd = clippedEnd;
        }

        public long getStart() {
            return start;
        }

        public long getEnd() {
            return end;
        }

        public long getClippedStart() {
            return clippedStart;
        }

        public long getClippedEnd() {
            return clippedEnd;
        }

        public long overlap() {
            return clippedEnd - clippedStart;
        }
    }

    private RepairTime() {
    }

    /**
     * Downtime stays in the built-in repair status.
     * A history that is already down when the window opens counts from the window start.
     * An asset with no history counts the whole window only when {@code repairNow} is true.
     * An open stay ends at {@code windowEnd}.
     */
    public static List<Span> spans(List<Change> changes, boolean repairNow, long windowStart, long windowEnd) {
        return spans(changes, repairNow, windowStart, windowEnd, Collections.singleton(Statuses.REPAIR));
    }

    /**
     * Downtime stays. A move from one status in {@code downtime} to another stays one open stay.
     * An empty set counts nothing. A null set counts the built-in repair status.
     */
    public static List<Span> spans(List<Change> changes, boolean repairNow, long windowStart, long windowEnd,
            Set<String> downtime) {
        List<Span> found = new ArrayList<Span>();
        if (windowEnd <= windowStart) {
            return found;
        }
        Set<String> down = new HashSet<String>();
        if (downtime == null) {
            down.add(Statuses.REPAIR);
        } else {
            for (String key : downtime) {
                if (key != null && !key.trim().isEmpty()) {
                    down.add(Statuses.canonical(key));
                }
            }
        }
        List<Change> ordered = new ArrayList<Change>();
        if (changes != null) {
            ordered.addAll(changes);
        }
        Collections.sort(ordered, new Comparator<Change>() {
            @Override
            public int compare(Change left, Change right) {
                return left.at < right.at ? -1 : left.at > right.at ? 1 : 0;
            }
        });
        if (ordered.isEmpty()) {
            if (repairNow && !down.isEmpty()) {
                add(found, windowStart, windowEnd, windowStart, windowEnd);
            }
            return found;
        }
        long open = -1L;
        for (int i = 0; i < ordered.size(); i++) {
            Change change = ordered.get(i);
            boolean repair = down.contains(change.next);
            if (repair && open < 0L) {
                open = change.at;
            } else if (!repair && open >= 0L) {
                add(found, open, change.at, windowStart, windowEnd);
                open = -1L;
            }
        }
        if (open >= 0L) {
            add(found, open, windowEnd, windowStart, windowEnd);
        }
        return found;
    }

    public static long overlap(List<Span> spans) {
        long total = 0L;
        if (spans == null) {
            return 0L;
        }
        for (int i = 0; i < spans.size(); i++) {
            total += spans.get(i).overlap();
        }
        return total;
    }

    /** Whole days, rounded up. A stay shorter than a day still counts as one day. */
    public static int days(long millis) {
        if (millis <= 0L) {
            return 0;
        }
        long count = (millis + DAY - 1L) / DAY;
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }

    private static void add(List<Span> found, long start, long end, long windowStart, long windowEnd) {
        if (end <= start) {
            return;
        }
        long clippedStart = Math.max(start, windowStart);
        long clippedEnd = Math.min(end, windowEnd);
        if (clippedEnd <= clippedStart) {
            return;
        }
        found.add(new Span(start, end, clippedStart, clippedEnd));
    }
}
