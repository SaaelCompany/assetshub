package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns the text stored in the asset field into one asset id.
 * The portal sometimes saves a name or an object key instead of the numeric id.
 */
public final class AssetFieldMatch {
    private AssetFieldMatch() {
    }

    public static final class Row {
        private final int id;
        private final String name;
        private final String objectKey;
        private final String projectKey;
        private final boolean location;
        private final String place;

        public Row(int id, String name, String objectKey, String projectKey, boolean location, String place) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.objectKey = objectKey == null ? "" : objectKey;
            this.projectKey = projectKey == null ? "" : projectKey;
            this.location = location;
            this.place = place == null ? "" : place;
        }
    }

    /**
     * The first hint that identifies one asset wins. An ambiguous hint stops
     * the search so a shorter piece of the same text cannot pick a different asset.
     */
    public static Integer choose(List<Row> rows, List<String> hints, String projectKey, String issueText) {
        if (rows == null || rows.isEmpty() || hints == null) {
            return null;
        }
        String hay = issueText == null ? "" : issueText.toLowerCase(Locale.ROOT);
        for (int i = 0; i < hints.size(); i++) {
            Hit hit = chooseHint(rows, hints.get(i), projectKey, hay);
            if (hit == null) {
                continue;
            }
            if (hit.ambiguous) {
                return null;
            }
            return hit.id;
        }
        return null;
    }

    private static Hit chooseHint(List<Row> rows, String hint, String projectKey, String hay) {
        if (hint == null) {
            return null;
        }
        String wanted = hint.trim();
        if (wanted.isEmpty()) {
            return null;
        }
        List<Row> keys = new ArrayList<Row>();
        List<Row> names = new ArrayList<Row>();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (row == null) {
                continue;
            }
            if (same(row.objectKey, wanted)) {
                keys.add(row);
            }
            if (same(row.name, wanted)) {
                names.add(row);
            }
        }
        if (!keys.isEmpty()) {
            Integer id = singleKey(keys, wanted, projectKey);
            return id == null ? Hit.ambiguous() : Hit.found(id);
        }
        if (names.isEmpty()) {
            return null;
        }
        List<Row> local = inProject(names, projectKey);
        List<Row> pool = local.isEmpty() ? names : local;
        Integer named = singleName(pool, hay);
        if (named != null) {
            return Hit.found(named);
        }
        return pool.size() > 1 ? Hit.ambiguous() : null;
    }

    private static Integer singleKey(List<Row> keys, String wanted, String projectKey) {
        if (keys.size() == 1) {
            return Integer.valueOf(keys.get(0).id);
        }
        List<Row> exact = new ArrayList<Row>();
        for (int i = 0; i < keys.size(); i++) {
            if (wanted.equals(keys.get(i).objectKey.trim())) {
                exact.add(keys.get(i));
            }
        }
        List<Row> basis = exact.isEmpty() ? keys : exact;
        if (basis.size() == 1) {
            return Integer.valueOf(basis.get(0).id);
        }
        List<Row> local = inProject(basis, projectKey);
        return local.size() == 1 ? Integer.valueOf(local.get(0).id) : null;
    }

    private static Integer singleName(List<Row> pool, String hay) {
        List<Row> equipment = new ArrayList<Row>();
        List<Row> places = new ArrayList<Row>();
        for (int i = 0; i < pool.size(); i++) {
            Row row = pool.get(i);
            if (row.location) {
                places.add(row);
            } else {
                equipment.add(row);
            }
        }
        List<Row> basis = equipment.isEmpty() ? places : equipment;
        if (basis.size() == 1) {
            return Integer.valueOf(basis.get(0).id);
        }
        if (hay.isEmpty()) {
            return null;
        }
        List<Row> mentioned = new ArrayList<Row>();
        for (int i = 0; i < basis.size(); i++) {
            if (mentioned(hay, basis.get(i))) {
                mentioned.add(basis.get(i));
            }
        }
        return mentioned.size() == 1 ? Integer.valueOf(mentioned.get(0).id) : null;
    }

    private static boolean mentioned(String hay, Row row) {
        String key = row.objectKey.trim().toLowerCase(Locale.ROOT);
        if (key.length() >= 3 && hay.contains(key)) {
            return true;
        }
        String[] parts = row.place.split("/");
        for (int i = 0; i < parts.length; i++) {
            String piece = parts[i].trim().toLowerCase(Locale.ROOT);
            if (piece.length() >= 3 && hay.contains(piece)) {
                return true;
            }
        }
        return false;
    }

    private static List<Row> inProject(List<Row> rows, String projectKey) {
        List<Row> local = new ArrayList<Row>();
        if (projectKey == null || projectKey.trim().isEmpty()) {
            return local;
        }
        for (int i = 0; i < rows.size(); i++) {
            if (same(rows.get(i).projectKey, projectKey)) {
                local.add(rows.get(i));
            }
        }
        return local;
    }

    private static boolean same(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    private static final class Hit {
        private final Integer id;
        private final boolean ambiguous;

        private Hit(Integer id, boolean ambiguous) {
            this.id = id;
            this.ambiguous = ambiguous;
        }

        private static Hit found(Integer id) {
            return new Hit(id, false);
        }

        private static Hit ambiguous() {
            return new Hit(null, true);
        }
    }
}
