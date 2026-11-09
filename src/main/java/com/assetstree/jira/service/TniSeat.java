package com.assetstree.jira.service;

import com.assetstree.jira.ao.AssetEntity;
import com.assetstree.jira.ao.AssetTypeEntity;
import com.assetstree.jira.model.ProjectKeys;
import com.assetstree.jira.model.TniSites;
import com.atlassian.activeobjects.external.ActiveObjects;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Puts an asset on the site selected by its host name. A room inside that site stays put. */
public final class TniSeat {
    private TniSeat() {
    }

    /** Site name when the parent changed, otherwise an empty string. */
    public static String move(ActiveObjects ao, AssetEntity asset, String rules, String host) {
        if (ao == null || asset == null) {
            return "";
        }
        Set<String> locations = locationKeys(ao, asset.getProjectKey());
        return move(ao, asset, rules, host, locations, index(ao, asset.getProjectKey(), locations));
    }

    private static String move(ActiveObjects ao, AssetEntity asset, String rules, String host,
                               Set<String> locations, java.util.Map<String, Integer> places) {
        String place = TniSites.match(rules, host, asset.getName());
        if (place.isEmpty() || locations.contains(asset.getTypeKey())) {
            return "";
        }
        Integer placeId = places.get(place.toLowerCase(Locale.ROOT));
        if (placeId == null || placeId.intValue() == asset.getID() || under(ao, asset, placeId.intValue())) {
            return "";
        }
        asset.setParentId(placeId);
        return place;
    }

    /** Moves every asset in the project whose name selects a site. One read of the tree. */
    public static void apply(ActiveObjects ao, AssetEntity[] rows, String projectKey, String rules) {
        if (ao == null || rows == null || rules == null || rules.trim().isEmpty()) {
            return;
        }
        Set<String> locations = locationKeys(ao, projectKey);
        java.util.Map<String, Integer> places = index(ao, projectKey, locations);
        for (int i = 0; i < rows.length; i++) {
            AssetEntity row = rows[i];
            if (row == null || !ProjectKeys.same(row.getProjectKey(), projectKey)) {
                continue;
            }
            if (!move(ao, row, rules, "", locations, places).isEmpty()) {
                row.save();
            }
        }
    }

    private static Set<String> locationKeys(ActiveObjects ao, String projectKey) {
        Set<String> keys = new HashSet<String>();
        AssetTypeEntity[] types = ao.find(AssetTypeEntity.class);
        if (types == null) {
            return keys;
        }
        for (int i = 0; i < types.length; i++) {
            AssetTypeEntity type = types[i];
            if (type != null && type.isLocation() && ProjectKeys.same(type.getProjectKey(), projectKey)) {
                keys.add(type.getTypeKey());
            }
        }
        return keys;
    }

    private static java.util.Map<String, Integer> index(ActiveObjects ao, String projectKey, Set<String> locations) {
        java.util.Map<String, Integer> places = new java.util.HashMap<String, Integer>();
        AssetEntity[] rows = ao.find(AssetEntity.class);
        if (rows == null) {
            return places;
        }
        for (int i = 0; i < rows.length; i++) {
            AssetEntity row = rows[i];
            if (row == null || !ProjectKeys.same(row.getProjectKey(), projectKey) || !locations.contains(row.getTypeKey())) {
                continue;
            }
            String name = row.getName() == null ? "" : row.getName().trim().toLowerCase(Locale.ROOT);
            String key = row.getObjectKey() == null ? "" : row.getObjectKey().trim().toLowerCase(Locale.ROOT);
            if (!name.isEmpty()) {
                places.put(name, Integer.valueOf(row.getID()));
            }
            if (!key.isEmpty()) {
                places.put(key, Integer.valueOf(row.getID()));
            }
        }
        return places;
    }

    private static boolean under(ActiveObjects ao, AssetEntity asset, int placeId) {
        Integer parent = asset.getParentId();
        int guard = 0;
        while (parent != null && parent.intValue() > 0 && guard < 40) {
            if (parent.intValue() == placeId) {
                return true;
            }
            AssetEntity node = ao.get(AssetEntity.class, parent.intValue());
            if (node == null || !ProjectKeys.same(node.getProjectKey(), asset.getProjectKey())) {
                return false;
            }
            parent = node.getParentId();
            guard++;
        }
        return false;
    }
}
