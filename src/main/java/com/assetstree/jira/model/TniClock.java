package com.assetstree.jira.model;

/**
 * After the storage period, a device is checked a few times across the probe window.
 * One reply starts the storage period again. Silence for every attempt retires it.
 */
public final class TniClock {
    public static final long DAY = 86400000L;
    public static final long MIN_GAP = 15L * 60L * 1000L;
    public static final long SIGHT_MS = 3L * 60L * 60L * 1000L;

    public enum Action {
        WAIT, PING, DROP
    }

    public static final class Mark {
        public final Action action;
        public final long probeFrom;

        public Mark(Action action, long probeFrom) {
            this.action = action;
            this.probeFrom = probeFrom;
        }
    }

    private TniClock() {
    }

    public static Mark look(long seenAt, long probeFrom, int fails, long lastProbe,
                            int keepDays, int probeDays, int tries, boolean hasAddress, long now) {
        if (seenAt <= 0 || now < seenAt + (long) keepDays * DAY) {
            return new Mark(Action.WAIT, probeFrom);
        }
        if (!hasAddress) {
            return new Mark(Action.WAIT, probeFrom);
        }
        int need = Math.max(tries, 1);
        if (fails >= need && probeFrom > 0) {
            return new Mark(Action.DROP, probeFrom);
        }
        long spacing = Math.max(MIN_GAP, ((long) Math.max(probeDays, 1) * DAY) / need);
        if (lastProbe > 0 && now < lastProbe + spacing) {
            return new Mark(Action.WAIT, probeFrom);
        }
        if (fails >= need) {
            return new Mark(Action.DROP, probeFrom);
        }
        return new Mark(Action.PING, probeFrom > 0 ? probeFrom : now);
    }
}
