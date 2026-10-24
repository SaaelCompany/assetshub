package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decides which selected nodes a bulk move or delete should touch.
 * A node under another selected node travels with that ancestor.
 */
public final class BulkSelection {
    public static final int MAX_ITEMS = 200;

    private BulkSelection() {
    }

    public static List<Integer> keepRoots(List<Integer> ids, Map<Integer, Integer> parents) {
        Set<Integer> selected = new HashSet<Integer>();
        if (ids != null) {
            for (Integer id : ids) {
                if (id != null) {
                    selected.add(id);
                }
            }
        }
        List<Integer> roots = new ArrayList<Integer>();
        if (ids == null) {
            return roots;
        }
        for (Integer id : ids) {
            if (id == null || underSelected(id.intValue(), selected, parents)) {
                continue;
            }
            roots.add(id);
        }
        return roots;
    }

    public static List<Integer> deepestFirst(List<Integer> ids, Map<Integer, Integer> parents) {
        List<Integer> copy = new ArrayList<Integer>();
        if (ids != null) {
            for (Integer id : ids) {
                if (id != null) {
                    copy.add(id);
                }
            }
        }
        final Map<Integer, Integer> tree = parents;
        Collections.sort(copy, new Comparator<Integer>() {
            @Override
            public int compare(Integer left, Integer right) {
                return depth(right.intValue(), tree) - depth(left.intValue(), tree);
            }
        });
        return copy;
    }

    public static boolean isUnder(int id, int ancestor, Map<Integer, Integer> parents) {
        Integer parent = parentOf(id, parents);
        Set<Integer> seen = new HashSet<Integer>();
        while (parent != null) {
            if (parent.intValue() == ancestor) {
                return true;
            }
            if (!seen.add(parent)) {
                return false;
            }
            parent = parentOf(parent.intValue(), parents);
        }
        return false;
    }

    private static boolean underSelected(int id, Set<Integer> selected, Map<Integer, Integer> parents) {
        Integer parent = parentOf(id, parents);
        Set<Integer> seen = new HashSet<Integer>();
        while (parent != null) {
            if (selected.contains(parent)) {
                return true;
            }
            if (!seen.add(parent)) {
                return false;
            }
            parent = parentOf(parent.intValue(), parents);
        }
        return false;
    }

    private static Integer parentOf(int id, Map<Integer, Integer> parents) {
        if (parents == null) {
            return null;
        }
        return TreeLogic.normalizeParent(parents.get(Integer.valueOf(id)));
    }

    private static int depth(int id, Map<Integer, Integer> parents) {
        int level = 0;
        Integer parent = parentOf(id, parents);
        Set<Integer> seen = new HashSet<Integer>();
        while (parent != null && seen.add(parent)) {
            level++;
            parent = parentOf(parent.intValue(), parents);
        }
        return level;
    }
}
