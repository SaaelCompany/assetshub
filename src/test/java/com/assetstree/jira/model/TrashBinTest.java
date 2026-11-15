package com.assetstree.jira.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TrashBinTest {
    @Test
    public void missingRootMarkMeansTheRowItself() {
        assertEquals(7, TrashBin.rootId(7, null));
        assertEquals(7, TrashBin.rootId(7, Integer.valueOf(0)));
        assertTrue(TrashBin.isTrashRoot(7, Integer.valueOf(7)));
        assertFalse(TrashBin.isTrashRoot(8, Integer.valueOf(7)));
    }

    @Test
    public void branchKeepsTheLiveDescendants() {
        Map<Integer, List<Integer>> children = new HashMap<Integer, List<Integer>>();
        children.put(Integer.valueOf(1), Arrays.asList(Integer.valueOf(2), Integer.valueOf(3)));
        children.put(Integer.valueOf(2), Arrays.asList(Integer.valueOf(4)));
        List<Integer> branch = TrashBin.branch(children, 1);
        assertEquals(Integer.valueOf(1), branch.get(branch.size() - 1));
        assertTrue(branch.contains(Integer.valueOf(2)));
        assertTrue(branch.contains(Integer.valueOf(3)));
        assertTrue(branch.contains(Integer.valueOf(4)));
        assertEquals(4, branch.size());
    }

    @Test
    public void restoreKeepsALiveParentAndDropsAMissingOne() {
        assertEquals(Integer.valueOf(5), TrashBin.restoreParent(Integer.valueOf(5), true));
        assertNull(TrashBin.restoreParent(Integer.valueOf(5), false));
        assertNull(TrashBin.restoreParent(null, true));
        assertNull(TrashBin.restoreParent(Integer.valueOf(0), true));
    }
}
