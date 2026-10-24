package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class ReportHolderDto {
    private UserProfileDto user;
    private int count;

    public UserProfileDto getUser() {
        return user;
    }

    public void setUser(UserProfileDto user) {
        this.user = user;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
