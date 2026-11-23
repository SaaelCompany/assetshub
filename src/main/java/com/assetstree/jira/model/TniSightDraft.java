package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.List;

/** Browser report posted when a customer opens the portal create form. */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class TniSightDraft {
    private String projectKey;
    private Integer portalId;
    private String ua;
    private String platform;
    private String screen;
    private String gpu;
    private Integer cores;
    private Double memory;
    private Integer touch;
    private Boolean battery;
    private List<String> addresses;
    private String seen;
    private String host;

    public String getProjectKey() { return projectKey; }
    public void setProjectKey(String projectKey) { this.projectKey = projectKey; }
    public Integer getPortalId() { return portalId; }
    public void setPortalId(Integer portalId) { this.portalId = portalId; }
    public String getUa() { return ua; }
    public void setUa(String ua) { this.ua = ua; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getScreen() { return screen; }
    public void setScreen(String screen) { this.screen = screen; }
    public String getGpu() { return gpu; }
    public void setGpu(String gpu) { this.gpu = gpu; }
    public Integer getCores() { return cores; }
    public void setCores(Integer cores) { this.cores = cores; }
    public Double getMemory() { return memory; }
    public void setMemory(Double memory) { this.memory = memory; }
    public Integer getTouch() { return touch; }
    public void setTouch(Integer touch) { this.touch = touch; }
    public Boolean getBattery() { return battery; }
    public void setBattery(Boolean battery) { this.battery = battery; }
    public List<String> getAddresses() { return addresses; }
    public void setAddresses(List<String> addresses) { this.addresses = addresses; }
    public String getSeen() { return seen; }
    public void setSeen(String seen) { this.seen = seen; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
}
