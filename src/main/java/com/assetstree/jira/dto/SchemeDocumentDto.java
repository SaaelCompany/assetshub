package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class SchemeDocumentDto {
    private int id;
    private String name;
    private String body;
    private long updatedAt;
    private int placeId;
    private String projectKey;
    private boolean shared;
    private boolean canEditShared;
    private List<String> shares = new ArrayList<String>();
    private List<String> shareEdits = new ArrayList<String>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public int getPlaceId() { return placeId; }
    public void setPlaceId(int placeId) { this.placeId = placeId; }
    public String getProjectKey() { return projectKey; }
    public void setProjectKey(String projectKey) { this.projectKey = projectKey; }
    public boolean isShared() { return shared; }
    public void setShared(boolean shared) { this.shared = shared; }
    public List<String> getShares() { return shares; }
    public void setShares(List<String> shares) {
        this.shares = shares == null ? new ArrayList<String>() : shares;
    }
    public boolean isCanEditShared() { return canEditShared; }
    public void setCanEditShared(boolean canEditShared) { this.canEditShared = canEditShared; }
    public List<String> getShareEdits() { return shareEdits; }
    public void setShareEdits(List<String> shareEdits) {
        this.shareEdits = shareEdits == null ? new ArrayList<String>() : shareEdits;
    }
}
