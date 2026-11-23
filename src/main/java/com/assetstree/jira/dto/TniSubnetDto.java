package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class TniSubnetDto {
    private int id;
    private String name;
    private String target;
    private int hosts;
    private String state;
    private int scanned;
    private int online;
    private int added;
    private String note;
    private String log;
    private long started;
    private long finished;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public int getHosts() { return hosts; }
    public void setHosts(int hosts) { this.hosts = hosts; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public int getScanned() { return scanned; }
    public void setScanned(int scanned) { this.scanned = scanned; }
    public int getOnline() { return online; }
    public void setOnline(int online) { this.online = online; }
    public int getAdded() { return added; }
    public void setAdded(int added) { this.added = added; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getLog() { return log; }
    public void setLog(String log) { this.log = log; }
    public long getStarted() { return started; }
    public void setStarted(long started) { this.started = started; }
    public long getFinished() { return finished; }
    public void setFinished(long finished) { this.finished = finished; }
}
