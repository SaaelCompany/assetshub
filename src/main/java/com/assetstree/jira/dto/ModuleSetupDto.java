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
    private String glyphs;
    private Boolean portalMine;
    private String portalColumns;
    private Integer tniKeep;
    private Integer tniProbeDays;
    private Integer tniProbeTries;
    private String tniAction;
    private String tniComputer;
    private String tniLaptop;
    private String tniPhone;
    private String tniTablet;

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
    public String getGlyphs() { return glyphs; }
    public void setGlyphs(String glyphs) { this.glyphs = glyphs; }
    public Boolean getPortalMine() { return portalMine; }
    public void setPortalMine(Boolean portalMine) { this.portalMine = portalMine; }
    public String getPortalColumns() { return portalColumns; }
    public void setPortalColumns(String portalColumns) { this.portalColumns = portalColumns; }
    public Integer getTniKeep() { return tniKeep; }
    public void setTniKeep(Integer tniKeep) { this.tniKeep = tniKeep; }
    public Integer getTniProbeDays() { return tniProbeDays; }
    public void setTniProbeDays(Integer tniProbeDays) { this.tniProbeDays = tniProbeDays; }
    public Integer getTniProbeTries() { return tniProbeTries; }
    public void setTniProbeTries(Integer tniProbeTries) { this.tniProbeTries = tniProbeTries; }
    public String getTniAction() { return tniAction; }
    public void setTniAction(String tniAction) { this.tniAction = tniAction; }
    public String getTniComputer() { return tniComputer; }
    public void setTniComputer(String tniComputer) { this.tniComputer = tniComputer; }
    public String getTniLaptop() { return tniLaptop; }
    public void setTniLaptop(String tniLaptop) { this.tniLaptop = tniLaptop; }
    public String getTniPhone() { return tniPhone; }
    public void setTniPhone(String tniPhone) { this.tniPhone = tniPhone; }
    public String getTniTablet() { return tniTablet; }
    public void setTniTablet(String tniTablet) { this.tniTablet = tniTablet; }
}
