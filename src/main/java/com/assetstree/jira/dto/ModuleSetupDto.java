package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/** Per-project options for the optional schedule, map, report and lifecycle modules. */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class ModuleSetupDto {
    private String scheduleKinds;
    private String scheduleUnit;
    private Boolean scheduleNotify;
    private Integer mapWidth;
    private Integer mapHeight;
    private Integer mapGrid;
    private Boolean mapSnap;
    private String reportHidden;
    private Boolean lifeApproval;
    private String lifeApprover;
    private String lifeCurrency;
    private Integer lifeMonths;

    public String getScheduleKinds() { return scheduleKinds; }
    public void setScheduleKinds(String scheduleKinds) { this.scheduleKinds = scheduleKinds; }
    public String getScheduleUnit() { return scheduleUnit; }
    public void setScheduleUnit(String scheduleUnit) { this.scheduleUnit = scheduleUnit; }
    public Boolean getScheduleNotify() { return scheduleNotify; }
    public void setScheduleNotify(Boolean scheduleNotify) { this.scheduleNotify = scheduleNotify; }
    public Integer getMapWidth() { return mapWidth; }
    public void setMapWidth(Integer mapWidth) { this.mapWidth = mapWidth; }
    public Integer getMapHeight() { return mapHeight; }
    public void setMapHeight(Integer mapHeight) { this.mapHeight = mapHeight; }
    public Integer getMapGrid() { return mapGrid; }
    public void setMapGrid(Integer mapGrid) { this.mapGrid = mapGrid; }
    public Boolean getMapSnap() { return mapSnap; }
    public void setMapSnap(Boolean mapSnap) { this.mapSnap = mapSnap; }
    public String getReportHidden() { return reportHidden; }
    public void setReportHidden(String reportHidden) { this.reportHidden = reportHidden; }
    public Boolean getLifeApproval() { return lifeApproval; }
    public void setLifeApproval(Boolean lifeApproval) { this.lifeApproval = lifeApproval; }
    public String getLifeApprover() { return lifeApprover; }
    public void setLifeApprover(String lifeApprover) { this.lifeApprover = lifeApprover; }
    public String getLifeCurrency() { return lifeCurrency; }
    public void setLifeCurrency(String lifeCurrency) { this.lifeCurrency = lifeCurrency; }
    public Integer getLifeMonths() { return lifeMonths; }
    public void setLifeMonths(Integer lifeMonths) { this.lifeMonths = lifeMonths; }
}
