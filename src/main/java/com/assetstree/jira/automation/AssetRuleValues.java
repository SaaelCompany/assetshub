package com.assetstree.jira.automation;

import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

import java.util.Map;

/** Reads the small maps stored on an Automation component and on an equipment event. */
public final class AssetRuleValues {
    public static final String CREATED = "asset.created";
    public static final String UPDATED = "asset.updated";
    public static final String DELETED = "asset.deleted";
    public static final String RESTORED = "asset.restored";

    private AssetRuleValues() {
    }

    public static boolean matches(String filter, String event) {
        if (event == null || event.isEmpty()) {
            return false;
        }
        if (filter == null || filter.trim().isEmpty()) {
            return CREATED.equals(event) || UPDATED.equals(event) || DELETED.equals(event) || RESTORED.equals(event);
        }
        return filter.trim().equals(event);
    }

    public static String field(Object value, String key) {
        if (!(value instanceof Map)) {
            return "";
        }
        Object raw = ((Map<?, ?>) value).get(key);
        return raw == null ? "" : String.valueOf(raw).trim();
    }

    public static String json(String body, String key) {
        if (body == null || body.isEmpty()) {
            return "";
        }
        try {
            JSONObject object = new JSONObject(body);
            return object.has(key) ? object.getString(key) : "";
        } catch (JSONException ex) {
            return "";
        }
    }

    public static int jsonInt(String body, String key) {
        if (body == null || body.isEmpty()) {
            return 0;
        }
        try {
            JSONObject object = new JSONObject(body);
            return object.has(key) ? object.getInt(key) : 0;
        } catch (JSONException ex) {
            return 0;
        }
    }
}
