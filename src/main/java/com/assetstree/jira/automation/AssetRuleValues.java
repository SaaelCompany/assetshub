package com.assetstree.jira.automation;

import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.codebarrel.automation.api.thirdparty.context.ComponentInputs;

import java.util.Map;

/** Reads the small maps stored on an Automation component and on an equipment event. */
public final class AssetRuleValues {
    public static final String CREATED = "asset.created";
    public static final String UPDATED = "asset.updated";
    public static final String DELETED = "asset.deleted";
    public static final String RESTORED = "asset.restored";
    public static final String STATUS = "asset.status";
    public static final String CALENDAR = "asset.calendar";
    public static final String WRITEOFF = "asset.writeoff";
    public static final String SCHEDULE = "asset.schedule";

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

    /** An empty status filter matches. A filled one matches only a real change into that status. */
    public static boolean statusMatches(String filter, String current, String previous) {
        if (filter == null || filter.trim().isEmpty()) {
            return true;
        }
        String wanted = filter.trim();
        String now = current == null ? "" : current.trim();
        if (!wanted.equals(now)) {
            return false;
        }
        String before = previous == null ? "" : previous.trim();
        return before.isEmpty() || !before.equals(wanted);
    }

    /** Write-off and schedule rows keep their current status, the same way a due date does. */
    public static boolean watchesCurrent(String kind) {
        return CALENDAR.equals(kind) || WRITEOFF.equals(kind) || SCHEDULE.equals(kind);
    }

    /** An empty detail filter matches. A filled one matches pending, approved, created, done, and the other words. */
    public static boolean detailMatches(String filter, String detail) {
        if (filter == null || filter.trim().isEmpty()) {
            return true;
        }
        String now = detail == null ? "" : detail.trim();
        return filter.trim().equals(now);
    }

    /** An empty filter matches. A filled one matches the current status, even when it did not just change. */
    public static boolean currentStatus(String filter, String current) {
        if (filter == null || filter.trim().isEmpty()) {
            return true;
        }
        String now = current == null ? "" : current.trim();
        return filter.trim().equals(now);
    }

    public static int assetId(ComponentInputs inputs) {
        if (inputs == null) {
            return 0;
        }
        if (inputs.getInputs() != null) {
            Object raw = inputs.getInputs().get("assetId");
            if (raw instanceof Number && ((Number) raw).intValue() > 0) {
                return ((Number) raw).intValue();
            }
        }
        String body = inputs.getSerializedEvent();
        if (!json(body, "event").startsWith("asset.")) {
            return 0;
        }
        return jsonInt(body, "id");
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
