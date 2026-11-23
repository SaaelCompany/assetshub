package com.assetstree.jira.model;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Picks the site a portal device belongs on.
 * An explicit asset or portal rule wins. Otherwise the request's answers
 * are matched to site names: one cabinet stays on that cabinet, several
 * cabinets under one branch stay on the shared branch.
 */
public final class TniPlace {
    private TniPlace() {
    }

    public static final class Spot {
        private final int id;
        private final Integer parentId;
        private final String name;
        private final boolean location;

        public Spot(int id, Integer parentId, String name, boolean location) {
            this.id = id;
            this.parentId = parentId;
            this.name = name == null ? "" : name;
            this.location = location;
        }
    }

    public static Integer choose(List<Spot> spots, List<PortalRules.Answer> answers, Integer pointed) {
        Map<Integer, Spot> byId = new java.util.LinkedHashMap<Integer, Spot>();
        if (spots != null) {
            for (Spot spot : spots) {
                if (spot != null && spot.id > 0) {
                    byId.put(Integer.valueOf(spot.id), spot);
                }
            }
        }
        Integer hinted = locationOf(byId, pointed);
        if (hinted != null) {
            return hinted;
        }
        List<Integer> named = new java.util.ArrayList<Integer>();
        for (Spot spot : byId.values()) {
            if (spot.location && named(spot.name, answers)) {
                named.add(Integer.valueOf(spot.id));
            }
        }
        return commonLocation(byId, named);
    }

    private static Integer locationOf(Map<Integer, Spot> byId, Integer id) {
        Integer cursor = id;
        int guard = 0;
        while (cursor != null && cursor.intValue() > 0 && guard < 40) {
            Spot spot = byId.get(cursor);
            if (spot == null) {
                return null;
            }
            if (spot.location) {
                return cursor;
            }
            cursor = spot.parentId;
            guard++;
        }
        return null;
    }

    private static boolean named(String spotName, List<PortalRules.Answer> answers) {
        String wanted = PortalRules.tidy(spotName);
        if (wanted.isEmpty() || answers == null) {
            return false;
        }
        for (PortalRules.Answer answer : answers) {
            if (answer == null) {
                continue;
            }
            String text = PortalRules.tidy(answer.getText());
            if (text.isEmpty() || blankOption(text)) {
                continue;
            }
            if (wanted.equals(text)) {
                return true;
            }
        }
        return false;
    }

    private static boolean blankOption(String tidy) {
        return "не выбрано".equals(tidy) || "none".equals(tidy) || "n/a".equals(tidy)
                || "select".equals(tidy) || "please select".equals(tidy) || "-".equals(tidy);
    }

    private static Integer commonLocation(Map<Integer, Spot> byId, List<Integer> matches) {
        if (matches == null || matches.isEmpty()) {
            return null;
        }
        Set<Integer> shared = null;
        for (Integer id : matches) {
            Set<Integer> chain = new LinkedHashSet<Integer>();
            Integer cursor = id;
            int guard = 0;
            while (cursor != null && guard < 40) {
                Spot spot = byId.get(cursor);
                if (spot == null || !spot.location) {
                    break;
                }
                chain.add(cursor);
                cursor = spot.parentId;
                guard++;
            }
            if (shared == null) {
                shared = chain;
            } else {
                shared.retainAll(chain);
            }
        }
        if (shared == null || shared.isEmpty()) {
            return null;
        }
        Integer best = null;
        int depth = -1;
        for (Integer id : shared) {
            int steps = 0;
            Integer cursor = id;
            int guard = 0;
            while (cursor != null && guard < 40) {
                steps++;
                Spot spot = byId.get(cursor);
                cursor = spot == null ? null : spot.parentId;
                guard++;
            }
            if (steps > depth) {
                depth = steps;
                best = id;
            }
        }
        return best;
    }
}
