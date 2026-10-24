package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class AssetDto {
    private int id;
    private String objectKey;
    private String name;
    private String description;
    private String typeKey;
    private String typeLabel;
    private String color;
    private String icon;
    private String status;
    private Integer parentId;
    private int sortOrder;
    private String created;
    private String updated;
    private String createdBy;
    private String updatedBy;
    private String projectKey;
    private String projectName;
    private String location;
    private boolean editable;
    private boolean cardImage;
    private UserProfileDto custodian;
    private String holderRole;
    private List<AttributeDto> attributes = new ArrayList<AttributeDto>();
    private List<IssueRefDto> issues;
    private List<CommentDto> comments;
    private List<FileDto> files;
    private List<ActivityDto> activities;
    private List<ServicePlanDto> plans;
    private String serviceDue;
    private List<String> offeredTypes = new ArrayList<String>();

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

    public String getTypeLabel() {
        return typeLabel;
    }

    public void setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
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

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isCardImage() {
        return cardImage;
    }

    public void setCardImage(boolean cardImage) {
        this.cardImage = cardImage;
    }

    public UserProfileDto getCustodian() {
        return custodian;
    }

    public void setCustodian(UserProfileDto custodian) {
        this.custodian = custodian;
    }

    public String getHolderRole() {
        return holderRole;
    }

    public void setHolderRole(String holderRole) {
        this.holderRole = holderRole;
    }

    public List<AttributeDto> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<AttributeDto> attributes) {
        this.attributes = attributes;
    }

    public List<IssueRefDto> getIssues() {
        return issues;
    }

    public void setIssues(List<IssueRefDto> issues) {
        this.issues = issues;
    }

    public List<CommentDto> getComments() {
        return comments;
    }

    public void setComments(List<CommentDto> comments) {
        this.comments = comments;
    }

    public List<FileDto> getFiles() {
        return files;
    }

    public void setFiles(List<FileDto> files) {
        this.files = files;
    }

    public List<ActivityDto> getActivities() {
        return activities;
    }

    public void setActivities(List<ActivityDto> activities) {
        this.activities = activities;
    }

    public List<ServicePlanDto> getPlans() {
        return plans;
    }

    public void setPlans(List<ServicePlanDto> plans) {
        this.plans = plans;
    }

    public String getServiceDue() {
        return serviceDue;
    }

    public void setServiceDue(String serviceDue) {
        this.serviceDue = serviceDue;
    }

    public List<String> getOfferedTypes() {
        return offeredTypes;
    }

    public void setOfferedTypes(List<String> offeredTypes) {
        this.offeredTypes = offeredTypes == null ? new ArrayList<String>() : offeredTypes;
    }
}
