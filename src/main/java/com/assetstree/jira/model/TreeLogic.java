package com.assetstree.jira.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TreeLogic {
    public static final int MAX_DEPTH = 40;

    private TreeLogic() {
    }

    public static Integer normalizeParent(Integer parentId) {
        if (parentId == null || parentId.intValue() <= 0) {
            return null;
        }
        return parentId;
    }

    public static boolean sameParent(Integer left, Integer right) {
        Integer a = normalizeParent(left);
        Integer b = normalizeParent(right);
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    public static boolean wouldCycle(Map<Integer, Integer> parentById, int movingId, Integer newParentId) {
        Integer parent = normalizeParent(newParentId);
        if (parent == null) {
            return false;
        }
        int cursor = parent.intValue();
        Set<Integer> seen = new HashSet<Integer>();
        while (true) {
            if (cursor == movingId) {
                return true;
            }
            if (!seen.add(Integer.valueOf(cursor))) {
                return true;
            }
            Integer next = normalizeParent(parentById.get(Integer.valueOf(cursor)));
            if (next == null) {
                return false;
            }
            cursor = next.intValue();
        }
    }

    public static int depth(Map<Integer, Integer> parentById, int id) {
        int level = 0;
        Integer parent = normalizeParent(parentById.get(Integer.valueOf(id)));
        Set<Integer> seen = new HashSet<Integer>();
        while (parent != null) {
            if (!seen.add(parent)) {
                return Integer.MAX_VALUE;
            }
            level++;
            if (level > 1000) {
                return Integer.MAX_VALUE;
            }
            parent = normalizeParent(parentById.get(parent));
        }
        return level;
    }

    public static int subtreeHeight(Map<Integer, List<Integer>> childrenByParent, int id) {
        return height(childrenByParent, id, new HashSet<Integer>());
    }

    /**
     * Equipment under each node. A location contributes only what sits beneath it.
     * A cycle contributes nothing and does not walk forever.
     */
    public static Map<Integer, Integer> equipmentCounts(Map<Integer, List<Integer>> childrenByParent, Set<Integer> locations) {
        Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
        if (childrenByParent == null || childrenByParent.isEmpty()) {
            return counts;
        }
        Set<Integer> nodes = new HashSet<Integer>();
        for (Map.Entry<Integer, List<Integer>> entry : childrenByParent.entrySet()) {
            if (entry.getKey() != null) {
                nodes.add(entry.getKey());
            }
            List<Integer> children = entry.getValue();
            if (children == null) {
                continue;
            }
            for (Integer child : children) {
                if (child != null) {
                    nodes.add(child);
                }
            }
        }
        Set<Integer> visiting = new HashSet<Integer>();
        for (Integer id : nodes) {
            countEquipment(id.intValue(), childrenByParent, locations, counts, visiting);
        }
        return counts;
    }

    public static List<Integer> descendants(Map<Integer, List<Integer>> childrenByParent, int rootId) {
        List<Integer> result = new ArrayList<Integer>();
        ArrayDeque<Integer> stack = new ArrayDeque<Integer>();
        pushChildren(childrenByParent, rootId, stack);
        Set<Integer> seen = new HashSet<Integer>();
        while (!stack.isEmpty()) {
            Integer id = stack.removeLast();
            if (id == null || !seen.add(id)) {
                continue;
            }
            result.add(id);
            pushChildren(childrenByParent, id.intValue(), stack);
        }
        return result;
    }

    public static List<Integer> place(List<Integer> orderedSiblingIds, int movingId, int index) {
        List<Integer> next = new ArrayList<Integer>();
        if (orderedSiblingIds != null) {
            for (Integer id : orderedSiblingIds) {
                if (id == null || id.intValue() == movingId) {
                    continue;
                }
                next.add(id);
            }
        }
        int target = index;
        if (target < 0) {
            target = 0;
        }
        if (target > next.size()) {
            target = next.size();
        }
        next.add(target, Integer.valueOf(movingId));
        return next;
    }

    public static String slug(String label) {
        if (label == null) {
            return "";
        }
        String normalized = label.trim().toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        boolean dash = false;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                builder.append(c);
                dash = false;
            } else if (c == ' ' || c == '-' || c == '_' || c == '.') {
                if (!dash && builder.length() > 0) {
                    builder.append('-');
                    dash = true;
                }
            }
        }
        trimDash(builder);
        if (builder.length() > 40) {
            builder.setLength(40);
            trimDash(builder);
        }
        return builder.toString();
    }

    public static String uniqueKey(String base, Set<String> taken) {
        String seed = base == null ? "" : base;
        if (seed.isEmpty() || seed.charAt(0) < 'a' || seed.charAt(0) > 'z') {
            seed = seed.isEmpty() ? "type" : "type-" + seed;
        }
        if (seed.length() > 40) {
            StringBuilder shortened = new StringBuilder(seed.substring(0, 40));
            trimDash(shortened);
            seed = shortened.toString();
            if (seed.isEmpty()) {
                seed = "type";
            }
        }
        if (!taken.contains(seed)) {
            return seed;
        }
        for (int i = 2; i < 10000; i++) {
            String suffix = "-" + i;
            String head = seed;
            if (head.length() + suffix.length() > 40) {
                head = head.substring(0, Math.max(1, 40 - suffix.length()));
                while (head.endsWith("-")) {
                    head = head.substring(0, head.length() - 1);
                }
                if (head.isEmpty()) {
                    head = "type";
                }
            }
            String candidate = head + suffix;
            if (!taken.contains(candidate)) {
                return candidate;
            }
        }
        return "type-" + Math.abs(seed.hashCode());
    }

    private static void trimDash(StringBuilder builder) {
        while (builder.length() > 0 && builder.charAt(builder.length() - 1) == '-') {
            builder.setLength(builder.length() - 1);
        }
    }

    private static int countEquipment(int id, Map<Integer, List<Integer>> childrenByParent, Set<Integer> locations,
                                      Map<Integer, Integer> counts, Set<Integer> visiting) {
        Integer key = Integer.valueOf(id);
        Integer known = counts.get(key);
        if (known != null) {
            return known.intValue();
        }
        if (!visiting.add(key)) {
            return 0;
        }
        int total = 0;
        List<Integer> children = childrenByParent.get(key);
        if (children != null) {
            for (Integer child : children) {
                if (child == null) {
                    continue;
                }
                total += countEquipment(child.intValue(), childrenByParent, locations, counts, visiting);
            }
        }
        visiting.remove(key);
        if (locations == null || !locations.contains(key)) {
            total += 1;
        }
        counts.put(key, Integer.valueOf(total));
        return total;
    }

    private static int height(Map<Integer, List<Integer>> childrenByParent, int id, Set<Integer> seen) {
        if (!seen.add(Integer.valueOf(id))) {
            return Integer.MAX_VALUE;
        }
        List<Integer> children = childrenByParent.get(Integer.valueOf(id));
        if (children == null || children.isEmpty()) {
            return 0;
        }
        int max = 0;
        for (Integer child : children) {
            if (child == null) {
                continue;
            }
            int childHeight = height(childrenByParent, child.intValue(), seen);
            if (childHeight == Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            max = Math.max(max, 1 + childHeight);
        }
        return max;
    }

    private static void pushChildren(Map<Integer, List<Integer>> childrenByParent, int id, ArrayDeque<Integer> stack) {
        List<Integer> children = childrenByParent.get(Integer.valueOf(id));
        if (children == null) {
            return;
        }
        for (int i = children.size() - 1; i >= 0; i--) {
            stack.addLast(children.get(i));
        }
    }
}
