package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class GrantDraft {
    private String groupName;
    private String userKey;
    private String level;
    private String caps;
    private String typeScope;
    private String fieldScope;
    private String schemaName;

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getUserKey() {
        return userKey;
    }

    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getCaps() {
        return caps;
    }

    public void setCaps(String caps) {
        this.caps = caps;
    }

    public String getTypeScope() { return typeScope; }
    public void setTypeScope(String typeScope) { this.typeScope = typeScope; }
    public String getFieldScope() { return fieldScope; }
    public void setFieldScope(String fieldScope) { this.fieldScope = fieldScope; }
    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
}
