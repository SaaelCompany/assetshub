package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

/** Equipment card a customer opens from the portal profile. */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class PortalCardDto {
    private int id;
    private String name = "";
    private String objectKey = "";
    private String description = "";
    private String location = "";
    private String status = "";
    private String statusLabel = "";
    private String statusCategory = "";
    private String role = "";
    private String color = "";
    private String icon = "";
    private String viewerKey = "";
    private UserProfileDto custodian;
    private UserProfileDto issued;
    private String createdBy = "";
    private List<UserProfileDto> shares = new ArrayList<UserProfileDto>();
    private List<CommentDto> comments = new ArrayList<CommentDto>();
    private List<FileDto> files = new ArrayList<FileDto>();
    private List<PortalMineDto.Line> lines = new ArrayList<PortalMineDto.Line>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey == null ? "" : objectKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location == null ? "" : location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status == null ? "" : status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel == null ? "" : statusLabel;
    }

    public String getStatusCategory() {
        return statusCategory;
    }

    public void setStatusCategory(String statusCategory) {
        this.statusCategory = statusCategory == null ? "" : statusCategory;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role == null ? "" : role;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color == null ? "" : color;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon == null ? "" : icon;
    }

    public String getViewerKey() {
        return viewerKey;
    }

    public void setViewerKey(String viewerKey) {
        this.viewerKey = viewerKey == null ? "" : viewerKey;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy == null ? "" : createdBy;
    }

    public List<UserProfileDto> getShares() {
        return shares;
    }

    public void setShares(List<UserProfileDto> shares) {
        this.shares = shares == null ? new ArrayList<UserProfileDto>() : shares;
    }

    public List<CommentDto> getComments() {
        return comments;
    }

    public void setComments(List<CommentDto> comments) {
        this.comments = comments == null ? new ArrayList<CommentDto>() : comments;
    }

    public List<FileDto> getFiles() {
        return files;
    }

    public void setFiles(List<FileDto> files) {
        this.files = files == null ? new ArrayList<FileDto>() : files;
    }

    public List<PortalMineDto.Line> getLines() {
        return lines;
    }

    public void setLines(List<PortalMineDto.Line> lines) {
        this.lines = lines == null ? new ArrayList<PortalMineDto.Line>() : lines;
    }
}
