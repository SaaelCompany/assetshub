package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Column keys of the summary and search tables. The name column is always first and is not stored.
 */
public final class ViewColumns {
    public static final int MAX_COLUMNS = 24;
    public static final int MAX_STORED = 450;
    public static final List<String> BASE = Collections.unmodifiableList(Arrays.asList(
            "key", "type", "status", "custodian", "location", "description", "created", "updated"));
    private static final Pattern ATTR = Pattern.compile("^attr:[a-z][a-z0-9-]{0,39}(:[a-z][a-z0-9-]{0,39})?$");

    private ViewColumns() {
    }

    public static List<String> clean(List<String> raw) {
        Set<String> picked = new LinkedHashSet<String>();
        if (raw != null) {
            for (String item : raw) {
                String key = item == null ? "" : item.trim();
                if (key.isEmpty() || "name".equals(key)) {
                    continue;
                }
                if (!BASE.contains(key) && !ATTR.matcher(key).matches()) {
                    throw new AssetException(400, "asset-tree.error.view.columns");
                }
                picked.add(key);
            }
        }
        if (picked.size() > MAX_COLUMNS || join(new ArrayList<String>(picked)).length() > MAX_STORED) {
            throw new AssetException(400, "asset-tree.error.view.columns");
        }
        return new ArrayList<String>(picked);
    }

    public static String join(List<String> columns) {
        StringBuilder out = new StringBuilder();
        for (String key : columns) {
            if (out.length() > 0) {
                out.append(',');
            }
            out.append(key);
        }
        return out.toString();
    }

    public static List<String> split(String stored) {
        List<String> result = new ArrayList<String>();
        if (stored == null || stored.trim().isEmpty()) {
            return result;
        }
        for (String part : stored.split(",")) {
            String key = part.trim();
            if (!key.isEmpty() && !result.contains(key) && (BASE.contains(key) || ATTR.matcher(key).matches())) {
                result.add(key);
            }
        }
        return result;
    }
}
