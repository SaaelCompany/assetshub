package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class GrantDto {
    private String groupName;
    private String userKey;
    private String username;
    private String displayName;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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
