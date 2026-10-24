package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class ServicePlanDraft {
    private String name;
    private String lastDone;
    private Integer everyCount;
    private String everyUnit;
    private String statusKey;
    /** Absent on a rename of the other fields so a partial save can leave the letter alone. */
    private Boolean notify;
    /** True records the service as done today and starts the next interval. */
    private Boolean done;
    private String kind;
    private String assigneeKey;
    private String note;
    private String startTime;
    private String endTime;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastDone() {
        return lastDone;
    }

    public void setLastDone(String lastDone) {
        this.lastDone = lastDone;
    }

    public Integer getEveryCount() {
        return everyCount;
    }

    public void setEveryCount(Integer everyCount) {
        this.everyCount = everyCount;
    }

    public String getEveryUnit() {
        return everyUnit;
    }

    public void setEveryUnit(String everyUnit) {
        this.everyUnit = everyUnit;
    }

    public String getStatusKey() {
        return statusKey;
    }

    public void setStatusKey(String statusKey) {
        this.statusKey = statusKey;
    }

    public Boolean getNotify() {
        return notify;
    }

    public void setNotify(Boolean notify) {
        this.notify = notify;
    }

    public Boolean getDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getAssigneeKey() {
        return assigneeKey;
    }

    public void setAssigneeKey(String assigneeKey) {
        this.assigneeKey = assigneeKey;
    }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
