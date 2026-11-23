package com.assetstree.jira.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Default custodian for a new asset: the nearest place that already has one. */
public final class PlaceMol {
    private PlaceMol() {
    }

    public static final class Node {
        private final int id;
        private final Integer parentId;
        private final boolean location;
        private final String custodianKey;

        public Node(int id, Integer parentId, boolean location, String custodianKey) {
            this.id = id;
            this.parentId = parentId;
            this.location = location;
            this.custodianKey = custodianKey == null ? "" : custodianKey.trim();
        }
    }

    public static String of(List<Node> nodes, Integer startId) {
        if (startId == null || startId.intValue() <= 0 || nodes == null) {
            return "";
        }
        Map<Integer, Node> byId = new HashMap<Integer, Node>();
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            if (node != null && node.id > 0) {
                byId.put(Integer.valueOf(node.id), node);
            }
        }
        Integer current = startId;
        Set<Integer> seen = new HashSet<Integer>();
        while (current != null && current.intValue() > 0 && seen.add(current)) {
            Node node = byId.get(current);
            if (node == null) {
                return "";
            }
            if (node.location && !node.custodianKey.isEmpty()) {
                return node.custodianKey;
            }
            current = node.parentId;
        }
        return "";
    }
}
