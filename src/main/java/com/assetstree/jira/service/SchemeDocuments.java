package com.assetstree.jira.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.assetstree.jira.ao.SchemeEntity;
import com.assetstree.jira.dto.SchemeDocumentDto;
import com.assetstree.jira.model.AssetException;
import com.assetstree.jira.model.ProjectKeys;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import net.java.ao.DBParam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class SchemeDocuments {
    public static final int MAX_SCHEMES = 40;
    public static final int MAX_BODY = 1500000;
    public static final int MAX_SHARES = 20;
    public static final int MAX_PLACEMENTS = 2000;

    private SchemeDocuments() {
    }

    public static String nameOf(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            return "Scheme";
        }
        if (name.length() > 80) {
            return name.substring(0, 80);
        }
        return name;
    }

    public static String bodyOf(String raw) {
        if (raw == null) {
            throw new AssetException(400, "asset-tree.error.scheme");
        }
        String body = raw.trim();
        if (body.isEmpty() || body.charAt(0) != '{' || body.length() > MAX_BODY) {
            throw new AssetException(400, "asset-tree.error.scheme");
        }
        return body;
    }

    public static int placeIdOf(String body) {
        if (body == null) {
            return 0;
        }
        int at = body.indexOf("\"placeId\"");
        if (at < 0) {
            return 0;
        }
        int colon = body.indexOf(':', at + 9);
        if (colon < 0) {
            return 0;
        }
        int i = colon + 1;
        while (i < body.length() && (body.charAt(i) == ' ' || body.charAt(i) == '\t')) {
            i++;
        }
        int value = 0;
        boolean digit = false;
        while (i < body.length() && body.charAt(i) >= '0' && body.charAt(i) <= '9') {
            digit = true;
            value = value * 10 + (body.charAt(i) - '0');
            if (value > 100000000) {
                return 0;
            }
            i++;
        }
        return digit ? value : 0;
    }

    public static List<SchemeDocumentDto> list(ActiveObjects ao, String projectKey) {
        List<SchemeDocumentDto> found = new ArrayList<SchemeDocumentDto>();
        SchemeEntity[] rows = ao.find(SchemeEntity.class);
        if (rows != null) {
            for (SchemeEntity row : rows) {
                if (projectKey.equals(row.getProjectKey())) {
                    found.add(summary(row));
                }
            }
        }
        Collections.sort(found, new Comparator<SchemeDocumentDto>() {
            @Override
            public int compare(SchemeDocumentDto left, SchemeDocumentDto right) {
                return left.getId() - right.getId();
            }
        });
        return found;
    }

    public static SchemeDocumentDto one(ActiveObjects ao, String projectKey, int schemeId) {
        return full(require(ao, projectKey, schemeId));
    }

    public static SchemeDocumentDto create(ActiveObjects ao, String projectKey, SchemeDocumentDto draft) {
        String name = nameOf(draft == null ? null : draft.getName());
        String body = bodyOf(draft == null ? null : draft.getBody());
        if (list(ao, projectKey).size() >= MAX_SCHEMES) {
            throw new AssetException(400, "asset-tree.error.scheme");
        }
        long now = System.currentTimeMillis();
        SchemeEntity row = ao.create(SchemeEntity.class, new DBParam("PROJECT_KEY", projectKey), new DBParam("NAME", name));
        row.setBody(body);
        row.setUpdatedAt(now);
        row.save();
        return summary(row);
    }

    public static SchemeDocumentDto save(ActiveObjects ao, String projectKey, int schemeId, SchemeDocumentDto draft) {
        SchemeEntity row = require(ao, projectKey, schemeId);
        row.setName(nameOf(draft == null ? null : draft.getName()));
        row.setBody(bodyOf(draft == null ? null : draft.getBody()));
        row.setUpdatedAt(System.currentTimeMillis());
        row.save();
        return summary(row);
    }

    public static void delete(ActiveObjects ao, String projectKey, int schemeId) {
        ao.delete(require(ao, projectKey, schemeId));
    }

    private static SchemeEntity require(ActiveObjects ao, String projectKey, int schemeId) {
        SchemeEntity[] rows = ao.find(SchemeEntity.class);
        if (rows != null) {
            for (SchemeEntity row : rows) {
                if (row.getID() == schemeId && projectKey.equals(row.getProjectKey())) {
                    return row;
                }
            }
        }
        throw new AssetException(404, "asset-tree.error.notFound");
    }

    private static SchemeDocumentDto summary(SchemeEntity row) {
        SchemeDocumentDto dto = new SchemeDocumentDto();
        dto.setId(row.getID());
        dto.setName(row.getName());
        dto.setUpdatedAt(row.getUpdatedAt());
        dto.setPlaceId(placeIdOf(row.getBody()));
        return dto;
    }

    private static SchemeDocumentDto full(SchemeEntity row) {
        SchemeDocumentDto dto = summary(row);
        dto.setBody(row.getBody());
        return dto;
    }

    public static SchemeEntity find(ActiveObjects ao, int schemeId) {
        if (ao == null || schemeId <= 0) {
            return null;
        }
        return ao.get(SchemeEntity.class, schemeId);
    }

    public static List<String> shareKeys(String owner, List<String> raw) {
        List<String> keys = new ArrayList<String>();
        if (raw == null) {
            return keys;
        }
        for (int i = 0; i < raw.size(); i++) {
            String item = raw.get(i);
            if (item == null) {
                continue;
            }
            String key = item.trim();
            if (key.isEmpty() || key.length() > 80 || ProjectKeys.same(owner, key)) {
                continue;
            }
            boolean seen = false;
            for (int k = 0; k < keys.size(); k++) {
                if (ProjectKeys.same(keys.get(k), key)) {
                    seen = true;
                    break;
                }
            }
            if (!seen) {
                keys.add(key);
            }
            if (keys.size() >= MAX_SHARES) {
                break;
            }
        }
        return keys;
    }

    /** Drops equipment markers that belong to another project. The stored drawing keeps its own markers. */
    public static String stripGuestAssets(String body, String ownerKey) {
        try {
            JSONObject root = document(body);
            JSONArray floors = root.optJSONArray("floors");
            if (floors == null) {
                return body;
            }
            boolean changed = false;
            for (int i = 0; i < floors.length(); i++) {
                JSONObject floor = floors.optJSONObject(i);
                if (floor == null) {
                    continue;
                }
                JSONArray objects = floor.optJSONArray("objects");
                if (objects == null) {
                    continue;
                }
                JSONArray kept = new JSONArray();
                for (int k = 0; k < objects.length(); k++) {
                    JSONObject object = objects.optJSONObject(k);
                    if (object != null && guestAsset(object, ownerKey)) {
                        changed = true;
                        continue;
                    }
                    kept.put(object != null ? object : objects.get(k));
                }
                if (kept.length() != objects.length()) {
                    floor.put("objects", kept);
                }
            }
            return changed ? root.toString() : body;
        } catch (JSONException ex) {
            throw new AssetException(400, "asset-tree.error.scheme");
        }
    }

    /** Equipment markers of one project, each tagged with the floor it sits on. */
    public static String takeProjectAssets(String body, String projectKey) {
        try {
            JSONObject root = document(body);
            JSONArray taken = new JSONArray();
            JSONArray floors = root.optJSONArray("floors");
            if (floors == null) {
                return "[]";
            }
            for (int i = 0; i < floors.length(); i++) {
                JSONObject floor = floors.optJSONObject(i);
                if (floor == null) {
                    continue;
                }
                JSONArray objects = floor.optJSONArray("objects");
                if (objects == null) {
                    continue;
                }
                for (int k = 0; k < objects.length() && taken.length() < MAX_PLACEMENTS; k++) {
                    JSONObject object = objects.optJSONObject(k);
                    if (object == null || !"asset".equals(object.optString("type", ""))) {
                        continue;
                    }
                    JSONObject binding = object.optJSONObject("assetBinding");
                    if (binding == null || !ProjectKeys.same(binding.optString("workspaceId", ""), projectKey)) {
                        continue;
                    }
                    JSONObject row = new JSONObject();
                    row.put("floorId", floor.optString("id", ""));
                    row.put("object", object);
                    taken.put(row);
                }
            }
            return taken.toString();
        } catch (JSONException ex) {
            throw new AssetException(400, "asset-tree.error.scheme");
        }
    }

    public static String mergePlacements(String body, List<String> placements) {
        if (placements == null || placements.isEmpty()) {
            return body;
        }
        try {
            JSONObject root = document(body);
            JSONArray floors = root.optJSONArray("floors");
            if (floors == null || floors.length() == 0) {
                return body;
            }
            boolean changed = false;
            for (int p = 0; p < placements.size(); p++) {
                String raw = placements.get(p);
                if (raw == null || raw.trim().isEmpty() || "[]".equals(raw.trim())) {
                    continue;
                }
                JSONArray rows = new JSONArray(raw);
                for (int i = 0; i < rows.length(); i++) {
                    JSONObject row = rows.optJSONObject(i);
                    if (row == null) {
                        continue;
                    }
                    JSONObject object = row.optJSONObject("object");
                    if (object == null || !"asset".equals(object.optString("type", ""))) {
                        continue;
                    }
                    JSONObject floor = floorById(floors, row.optString("floorId", ""));
                    if (floor == null) {
                        floor = floors.optJSONObject(0);
                    }
                    if (floor == null) {
                        continue;
                    }
                    JSONArray objects = floor.optJSONArray("objects");
                    if (objects == null) {
                        objects = new JSONArray();
                        floor.put("objects", objects);
                    }
                    if (containsId(objects, object.optString("id", ""))) {
                        continue;
                    }
                    objects.put(object);
                    changed = true;
                }
            }
            return changed ? root.toString() : body;
        } catch (JSONException ex) {
            return body;
        }
    }

    private static JSONObject document(String body) throws JSONException {
        if (body == null || body.trim().isEmpty() || body.trim().charAt(0) != '{') {
            throw new JSONException("scheme");
        }
        return new JSONObject(body);
    }

    private static boolean guestAsset(JSONObject object, String ownerKey) throws JSONException {
        if (!"asset".equals(object.optString("type", ""))) {
            return false;
        }
        JSONObject binding = object.optJSONObject("assetBinding");
        if (binding == null) {
            return false;
        }
        String workspace = binding.optString("workspaceId", "");
        return !workspace.isEmpty() && !ProjectKeys.same(workspace, ownerKey);
    }

    private static JSONObject floorById(JSONArray floors, String floorId) throws JSONException {
        for (int i = 0; i < floors.length(); i++) {
            JSONObject floor = floors.optJSONObject(i);
            if (floor != null && floorId != null && floorId.equals(floor.optString("id", ""))) {
                return floor;
            }
        }
        return null;
    }

    private static boolean containsId(JSONArray objects, String id) throws JSONException {
        if (id == null || id.isEmpty()) {
            return false;
        }
        for (int i = 0; i < objects.length(); i++) {
            JSONObject object = objects.optJSONObject(i);
            if (object != null && id.equals(object.optString("id", ""))) {
                return true;
            }
        }
        return false;
    }
}
