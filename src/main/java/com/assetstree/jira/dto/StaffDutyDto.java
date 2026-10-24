package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class StaffDutyDto {
    private int id;
    private String userKey;
    private String userName;
    private String kind;
    private int weekdays;
    private int hours;
    private String startOn;
    private String endOn;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUserKey() { return userKey; }
    public void setUserKey(String userKey) { this.userKey = userKey; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public int getWeekdays() { return weekdays; }
    public void setWeekdays(int weekdays) { this.weekdays = weekdays; }
    public int getHours() { return hours; }
    public void setHours(int hours) { this.hours = hours; }
    public String getStartOn() { return startOn; }
    public void setStartOn(String startOn) { this.startOn = startOn; }
    public String getEndOn() { return endOn; }
    public void setEndOn(String endOn) { this.endOn = endOn; }
}
