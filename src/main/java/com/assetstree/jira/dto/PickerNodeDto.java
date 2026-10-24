package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class PickerNodeDto {
    private int id;
    private Integer parentId;
    private String name;
    private String objectKey;
    private String typeLabel;
    private String status;
    private boolean group;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public void setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean getGroup() {
        return group;
    }

    public void setGroup(boolean group) {
        this.group = group;
    }
}
