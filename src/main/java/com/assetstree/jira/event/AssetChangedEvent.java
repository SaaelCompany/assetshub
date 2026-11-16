package com.assetstree.jira.event;

/** Published when equipment is created, changed, moved to the trash, restored, or its status changes. */
public final class AssetChangedEvent {
    private final String projectKey;
    private final String kind;
    private final int assetId;
    private final String objectKey;
    private final String name;
    private final String status;
    private final String typeKey;
    private final String previousStatus;

    public AssetChangedEvent(String projectKey, String kind, int assetId, String objectKey, String name,
                              String status, String typeKey, String previousStatus) {
        this.projectKey = projectKey == null ? "" : projectKey;
        this.kind = kind == null ? "" : kind;
        this.assetId = assetId;
        this.objectKey = objectKey == null ? "" : objectKey;
        this.name = name == null ? "" : name;
        this.status = status == null ? "" : status;
        this.typeKey = typeKey == null ? "" : typeKey;
        this.previousStatus = previousStatus == null ? "" : previousStatus;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public String getKind() {
        return kind;
    }

    public int getAssetId() {
        return assetId;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getTypeKey() {
        return typeKey;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String toJson() {
        return "{\"event\":\"" + escape(kind) + "\",\"id\":" + assetId
                + ",\"key\":\"" + escape(objectKey) + "\",\"name\":\"" + escape(name)
                + "\",\"project\":\"" + escape(projectKey) + "\",\"status\":\"" + escape(status)
                + "\",\"type\":\"" + escape(typeKey) + "\",\"previous\":\"" + escape(previousStatus) + "\"}";
    }

    private static String escape(String value) {
        StringBuilder builder = new StringBuilder();
        String raw = value == null ? "" : value;
        for (int i = 0; i < raw.length(); i++) {
            char current = raw.charAt(i);
            if (current == '\\' || current == '"') {
                builder.append('\\').append(current);
            } else if (current == '\n') {
                builder.append("\\n");
            } else if (current >= 32) {
                builder.append(current);
            }
        }
        return builder.toString();
    }
}
