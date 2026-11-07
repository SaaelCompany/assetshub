package com.assetstree.jira.dto;

/** Reachability choice for one asset. The check stays off until somebody turns it on. */
public class TniWatchDto {
    private Boolean armed;
    private String address;
    private String suggestion;
    private Integer up;
    private long checkedAt;

    public Boolean getArmed() { return armed; }
    public void setArmed(Boolean armed) { this.armed = armed; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public Integer getUp() { return up; }
    public void setUp(Integer up) { this.up = up; }
    public long getCheckedAt() { return checkedAt; }
    public void setCheckedAt(long checkedAt) { this.checkedAt = checkedAt; }
}
