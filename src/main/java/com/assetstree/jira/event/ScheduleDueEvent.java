package com.assetstree.jira.event;

/** Published once when a calendar day arrives, so one Automation rule can create the request. */
public final class ScheduleDueEvent {
    public static final String NAME = "asset.scheduleDue";

    private final String projectKey;
    private final int assetId;
    private final String objectKey;
    private final String assetName;
    private final String status;
    private final String typeKey;
    private final String plan;
    private final String due;
    private final String note;
    private final String kind;
    private final String kindName;
    private final String assignee;
    private final String assigneeName;
    private final String start;
    private final String end;
    private final String source;
    private final int planId;
    private final String custodian;
    private final String custodianName;

    public ScheduleDueEvent(String projectKey, int assetId, String objectKey, String assetName, String status,
                             String typeKey, String plan, String due, String note, String kind, String kindName,
                             String assignee, String assigneeName, String start, String end, String source,
                             int planId, String custodian, String custodianName) {
        this.projectKey = projectKey == null ? "" : projectKey;
        this.assetId = assetId;
        this.objectKey = objectKey == null ? "" : objectKey;
        this.assetName = assetName == null ? "" : assetName;
        this.status = status == null ? "" : status;
        this.typeKey = typeKey == null ? "" : typeKey;
        this.plan = plan == null ? "" : plan;
        this.due = due == null ? "" : due;
        this.note = note == null ? "" : note;
        this.kind = kind == null ? "" : kind;
        this.kindName = kindName == null ? "" : kindName;
        this.assignee = assignee == null ? "" : assignee;
        this.assigneeName = assigneeName == null ? "" : assigneeName;
        this.start = start == null ? "" : start;
        this.end = end == null ? "" : end;
        this.source = source == null ? "" : source;
        this.planId = planId;
        this.custodian = custodian == null ? "" : custodian;
        this.custodianName = custodianName == null ? "" : custodianName;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public int getAssetId() {
        return assetId;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getAssetName() {
        return assetName;
    }

    public String getStatus() {
        return status;
    }

    public String getTypeKey() {
        return typeKey;
    }

    public String getPlan() {
        return plan;
    }

    public String getDue() {
        return due;
    }

    public String getNote() {
        return note;
    }

    public String getKind() {
        return kind;
    }

    public String getKindName() {
        return kindName;
    }

    public String getAssignee() {
        return assignee;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }

    public String getSource() {
        return source;
    }

    public int getPlanId() {
        return planId;
    }

    public String getCustodian() {
        return custodian;
    }

    public String getCustodianName() {
        return custodianName;
    }

    public String toJson() {
        return "{\"event\":\"" + escape(NAME) + "\",\"project\":\"" + escape(projectKey)
                + "\",\"id\":" + assetId
                + ",\"key\":\"" + escape(objectKey) + "\",\"name\":\"" + escape(assetName)
                + "\",\"status\":\"" + escape(status) + "\",\"type\":\"" + escape(typeKey)
                + "\",\"plan\":\"" + escape(plan) + "\",\"due\":\"" + escape(due)
                + "\",\"note\":\"" + escape(note) + "\",\"kind\":\"" + escape(kind)
                + "\",\"kindName\":\"" + escape(kindName) + "\",\"assignee\":\"" + escape(assignee)
                + "\",\"assigneeName\":\"" + escape(assigneeName) + "\",\"start\":\"" + escape(start)
                + "\",\"end\":\"" + escape(end) + "\",\"source\":\"" + escape(source)
                + "\",\"planId\":" + planId
                + ",\"custodian\":\"" + escape(custodian) + "\",\"custodianName\":\"" + escape(custodianName) + "\"}";
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
            } else if (current == '\r') {
                builder.append("\\r");
            } else if (current >= 32) {
                builder.append(current);
            }
        }
        return builder.toString();
    }
}
