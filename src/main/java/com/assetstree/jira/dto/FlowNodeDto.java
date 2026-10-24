package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class FlowNodeDto {
    private String id;
    private String kind;
    private int x;
    private int y;
    private String issueTypeId;
    private String issueTypeName;
    private String issueStatusId;
    private String issueStatusName;
    private String statusKey;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String getIssueTypeId() {
        return issueTypeId;
    }

    public void setIssueTypeId(String issueTypeId) {
        this.issueTypeId = issueTypeId;
    }

    public String getIssueTypeName() {
        return issueTypeName;
    }

    public void setIssueTypeName(String issueTypeName) {
        this.issueTypeName = issueTypeName;
    }

    public String getIssueStatusId() {
        return issueStatusId;
    }

    public void setIssueStatusId(String issueStatusId) {
        this.issueStatusId = issueStatusId;
    }

    public String getIssueStatusName() {
        return issueStatusName;
    }

    public void setIssueStatusName(String issueStatusName) {
        this.issueStatusName = issueStatusName;
    }

    public String getStatusKey() {
        return statusKey;
    }

    public void setStatusKey(String statusKey) {
        this.statusKey = statusKey;
    }
}
