package com.assetstree.jira.model;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class AssetValidator {
    public static final int MAX_ATTRIBUTES = 50;
    public static final int MAX_NAME = 255;
    public static final int MAX_DESCRIPTION = 10000;
    public static final int MAX_ATTR_NAME = 80;
    public static final int MAX_ATTR_VALUE = 2000;
    public static final int MAX_CAPTION = 80;

    private static final Pattern COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final Pattern TYPE_KEY = Pattern.compile("^[a-z][a-z0-9-]{0,39}$");
    private static final Pattern ISSUE_KEY = Pattern.compile("^[A-Z][A-Z0-9]+-\\d+$");

    private AssetValidator() {
    }

    public static String validateDraft(AssetDraft draft) {
        if (draft == null) {
            return "asset-tree.error.name.required";
        }
        String name = draft.getName() == null ? "" : draft.getName().trim();
        if (name.isEmpty()) {
            return "asset-tree.error.name.required";
        }
        if (name.length() > MAX_NAME) {
            return "asset-tree.error.name.length";
        }
        String description = draft.getDescription() == null ? "" : draft.getDescription();
        if (description.length() > MAX_DESCRIPTION) {
            return "asset-tree.error.description.length";
        }
        if (draft.getStatus() != null) {
            String status = Statuses.canonical(draft.getStatus());
            if (!Statuses.allowed(draft.getStatus()) && !isStatusKey(status)) {
                return "asset-tree.error.status";
            }
        }
        return validateAttributes(draft.getAttributes());
    }

    public static String validateAttributes(List<AttributeDraft> attributes) {
        if (attributes == null) {
            return null;
        }
        if (attributes.size() > MAX_ATTRIBUTES) {
            return "asset-tree.error.attribute.limit";
        }
        Set<String> names = new HashSet<String>();
        int meaningful = 0;
        for (AttributeDraft attribute : attributes) {
            if (attribute == null) {
                continue;
            }
            String name = attribute.getFieldKey() == null ? "" : attribute.getFieldKey().trim();
            String value = attribute.getValue() == null ? "" : attribute.getValue();
            if (name.isEmpty() && value.trim().isEmpty()) {
                continue;
            }
            if (name.isEmpty()) {
                return "asset-tree.error.attribute.name";
            }
            if (name.length() > MAX_ATTR_NAME) {
                return "asset-tree.error.attribute.name.length";
            }
            if (value.length() > MAX_ATTR_VALUE) {
                return "asset-tree.error.attribute.value.length";
            }
            if (!names.add(name.toLowerCase(Locale.ROOT))) {
                return "asset-tree.error.attribute.duplicate";
            }
            meaningful++;
        }
        if (meaningful > MAX_ATTRIBUTES) {
            return "asset-tree.error.attribute.limit";
        }
        return null;
    }

    public static String validateTypeLabel(String label) {
        if (label == null || label.trim().isEmpty()) {
            return "asset-tree.error.type.label";
        }
        if (label.trim().length() > MAX_NAME) {
            return "asset-tree.error.type.label.length";
        }
        return null;
    }

    /** Empty is allowed and means the card uses the type name. */
    public static String validateCaption(String caption) {
        if (caption == null || caption.trim().isEmpty()) {
            return null;
        }
        if (caption.trim().length() > MAX_CAPTION) {
            return "asset-tree.error.caption.length";
        }
        return null;
    }

    public static String validateColor(String color) {
        if (color == null || !COLOR.matcher(color).matches()) {
            return "asset-tree.error.type.color";
        }
        return null;
    }

    public static boolean isTypeKey(String typeKey) {
        return typeKey != null && TYPE_KEY.matcher(typeKey).matches();
    }

    public static boolean isStatusKey(String statusKey) {
        return statusKey != null && TYPE_KEY.matcher(statusKey).matches();
    }

    public static String normalizeIssueKey(String issueKey) {
        if (issueKey == null) {
            return "";
        }
        return issueKey.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isIssueKey(String issueKey) {
        return ISSUE_KEY.matcher(normalizeIssueKey(issueKey)).matches();
    }
}
