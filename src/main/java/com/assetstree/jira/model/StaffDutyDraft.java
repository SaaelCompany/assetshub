package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class StaffDutyDraft {
    private String userKey;
    private Integer weekdays;
    private Integer hours;
    private String startOn;
    private String endOn;
    private String marks;

    public String getUserKey() { return userKey; }
    public void setUserKey(String userKey) { this.userKey = userKey; }
    public Integer getWeekdays() { return weekdays; }
    public void setWeekdays(Integer weekdays) { this.weekdays = weekdays; }
    public Integer getHours() { return hours; }
    public void setHours(Integer hours) { this.hours = hours; }
    public String getStartOn() { return startOn; }
    public void setStartOn(String startOn) { this.startOn = startOn; }
    public String getEndOn() { return endOn; }
    public void setEndOn(String endOn) { this.endOn = endOn; }
    public String getMarks() { return marks; }
    public void setMarks(String marks) { this.marks = marks; }
}
