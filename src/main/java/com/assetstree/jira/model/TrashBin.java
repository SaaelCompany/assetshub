package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Which rows a delete, a restore, and a permanent removal touch. */
public final class TrashBin {
    private TrashBin() {
    }

    /** The delete that put this row in the bin. A missing mark means the row itself. */
    public static int rootId(int id, Integer trashRootId) {
        Integer root = TreeLogic.normalizeParent(trashRootId);
        return root == null ? id : root.intValue();
    }

    public static boolean isTrashRoot(int id, Integer trashRootId) {
        return rootId(id, trashRootId) == id;
    }

    /** The live node and the live descendants that leave the tree with it. */
    public static List<Integer> branch(Map<Integer, List<Integer>> liveChildren, int id) {
        List<Integer> ids = new ArrayList<Integer>(TreeLogic.descendants(liveChildren, id));
        ids.add(Integer.valueOf(id));
        return ids;
    }

    /**
     * Where a restored object sits. A parent that is still in the tree keeps the old place.
     * A missing parent or a parent that is itself in the bin returns the project root.
     */
    public static Integer restoreParent(Integer parentId, boolean parentLive) {
        Integer parent = TreeLogic.normalizeParent(parentId);
        if (parent == null || !parentLive) {
            return null;
        }
        return parent;
    }
}
