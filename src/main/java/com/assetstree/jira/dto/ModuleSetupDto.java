package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/** Per-project options for the optional schedule, map, report and lifecycle modules. */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class ModuleSetupDto {
    private String scheduleKinds;
    private String scheduleUnit;
    private String scheduleLayer;
    private Boolean scheduleNotify;
    private Integer mapWidth;
    private Integer mapHeight;
    private Integer mapGrid;
    private Boolean mapSnap;
    private String reportHidden;
    private String reportCharts;
    private Boolean reportMail;
    private String reportMailEvery;
    private Integer reportMailDay;
    private String reportMailTo;
    private String reportMailSent;
    private String reportMailPart;
    private Boolean lifeApproval;
    private String lifeApprover;
    private String lifeCurrency;
    private Integer lifeMonths;
    private String staffMarks;
    private String staffOrder;
    private String staffPlaceOrder;
    private String scheduleBoards;
    private String scheduleOpen;
    private String furniture;

    public String getScheduleKinds() { return scheduleKinds; }
    public void setScheduleKinds(String scheduleKinds) { this.scheduleKinds = scheduleKinds; }
    public String getScheduleUnit() { return scheduleUnit; }
    public void setScheduleUnit(String scheduleUnit) { this.scheduleUnit = scheduleUnit; }
    public String getScheduleLayer() { return scheduleLayer; }
    public void setScheduleLayer(String scheduleLayer) { this.scheduleLayer = scheduleLayer; }
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
    public String getReportCharts() { return reportCharts; }
    public void setReportCharts(String reportCharts) { this.reportCharts = reportCharts; }
    public Boolean getReportMail() { return reportMail; }
    public void setReportMail(Boolean reportMail) { this.reportMail = reportMail; }
    public String getReportMailEvery() { return reportMailEvery; }
    public void setReportMailEvery(String reportMailEvery) { this.reportMailEvery = reportMailEvery; }
    public Integer getReportMailDay() { return reportMailDay; }
    public void setReportMailDay(Integer reportMailDay) { this.reportMailDay = reportMailDay; }
    public String getReportMailTo() { return reportMailTo; }
    public void setReportMailTo(String reportMailTo) { this.reportMailTo = reportMailTo; }
    public String getReportMailSent() { return reportMailSent; }
    public void setReportMailSent(String reportMailSent) { this.reportMailSent = reportMailSent; }
    public String getReportMailPart() { return reportMailPart; }
    public void setReportMailPart(String reportMailPart) { this.reportMailPart = reportMailPart; }
    public Boolean getLifeApproval() { return lifeApproval; }
    public void setLifeApproval(Boolean lifeApproval) { this.lifeApproval = lifeApproval; }
    public String getLifeApprover() { return lifeApprover; }
    public void setLifeApprover(String lifeApprover) { this.lifeApprover = lifeApprover; }
    public String getLifeCurrency() { return lifeCurrency; }
    public void setLifeCurrency(String lifeCurrency) { this.lifeCurrency = lifeCurrency; }
    public Integer getLifeMonths() { return lifeMonths; }
    public void setLifeMonths(Integer lifeMonths) { this.lifeMonths = lifeMonths; }
    public String getStaffMarks() { return staffMarks; }
    public void setStaffMarks(String staffMarks) { this.staffMarks = staffMarks; }
    public String getStaffOrder() { return staffOrder; }
    public void setStaffOrder(String staffOrder) { this.staffOrder = staffOrder; }
    public String getStaffPlaceOrder() { return staffPlaceOrder; }
    public void setStaffPlaceOrder(String staffPlaceOrder) { this.staffPlaceOrder = staffPlaceOrder; }
    public String getScheduleBoards() { return scheduleBoards; }
    public void setScheduleBoards(String scheduleBoards) { this.scheduleBoards = scheduleBoards; }
    public String getScheduleOpen() { return scheduleOpen; }
    public void setScheduleOpen(String scheduleOpen) { this.scheduleOpen = scheduleOpen; }
    public String getFurniture() { return furniture; }
    public void setFurniture(String furniture) { this.furniture = furniture; }
}
