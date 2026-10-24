package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class AssetDraft {
    private String name;
    private String description;
    private String typeKey;
    private String status;
    private Integer parentId;
    private String projectKey;
    private String custodianKey;
    private List<AttributeDraft> attributes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTypeKey() {
        return typeKey;
    }

    public void setTypeKey(String typeKey) {
        this.typeKey = typeKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public String getCustodianKey() {
        return custodianKey;
    }

    public void setCustodianKey(String custodianKey) {
        this.custodianKey = custodianKey;
    }

    public List<AttributeDraft> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<AttributeDraft> attributes) {
        this.attributes = attributes;
    }
}
