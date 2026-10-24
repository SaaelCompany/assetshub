package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class ScheduleDraft {
    private List<Integer> assetIds = new ArrayList<Integer>();
    private String name;
    private String kind;
    private String assigneeKey;
    private String lastDone;
    private Integer everyCount;
    private String everyUnit;
    private String statusKey;
    private Boolean notify;
    private String note;
    private String startTime;
    private String endTime;

    public List<Integer> getAssetIds() { return assetIds; }
    public void setAssetIds(List<Integer> assetIds) { this.assetIds = assetIds; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getAssigneeKey() { return assigneeKey; }
    public void setAssigneeKey(String assigneeKey) { this.assigneeKey = assigneeKey; }
    public String getLastDone() { return lastDone; }
    public void setLastDone(String lastDone) { this.lastDone = lastDone; }
    public Integer getEveryCount() { return everyCount; }
    public void setEveryCount(Integer everyCount) { this.everyCount = everyCount; }
    public String getEveryUnit() { return everyUnit; }
    public void setEveryUnit(String everyUnit) { this.everyUnit = everyUnit; }
    public String getStatusKey() { return statusKey; }
    public void setStatusKey(String statusKey) { this.statusKey = statusKey; }
    public Boolean getNotify() { return notify; }
    public void setNotify(Boolean notify) { this.notify = notify; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
