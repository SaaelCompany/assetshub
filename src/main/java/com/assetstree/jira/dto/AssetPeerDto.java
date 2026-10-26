package com.assetstree.jira.dto;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class AssetPeerDto {
    private int id;
    private String objectKey;
    private String name;
    private String projectKey;
    private String projectName;
    private String typeLabel;
    private String location;
    private boolean open;
    private UserProfileDto custodian;
    private UserProfileDto issued;
    private List<AttributeDto> attributes = new ArrayList<AttributeDto>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public void setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public UserProfileDto getCustodian() {
        return custodian;
    }

    public void setCustodian(UserProfileDto custodian) {
        this.custodian = custodian;
    }

    public UserProfileDto getIssued() {
        return issued;
    }

    public void setIssued(UserProfileDto issued) {
        this.issued = issued;
    }

    public List<AttributeDto> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<AttributeDto> attributes) {
        this.attributes = attributes == null ? new ArrayList<AttributeDto>() : attributes;
    }
}
