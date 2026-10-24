package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class ReportDto {
    private String projectKey;
    private String projectName;
    private int total;
    private int equipment;
    private int unassigned;
    private List<ReportBucketDto> byStatus = new ArrayList<ReportBucketDto>();
    private List<ReportBucketDto> byType = new ArrayList<ReportBucketDto>();
    private List<ReportPlaceDto> places = new ArrayList<ReportPlaceDto>();
    private List<ReportHolderDto> holders = new ArrayList<ReportHolderDto>();

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getEquipment() {
        return equipment;
    }

    public void setEquipment(int equipment) {
        this.equipment = equipment;
    }

    public int getUnassigned() {
        return unassigned;
    }

    public void setUnassigned(int unassigned) {
        this.unassigned = unassigned;
    }

    public List<ReportBucketDto> getByStatus() {
        return byStatus;
    }

    public void setByStatus(List<ReportBucketDto> byStatus) {
        this.byStatus = byStatus;
    }

    public List<ReportBucketDto> getByType() {
        return byType;
    }

    public void setByType(List<ReportBucketDto> byType) {
        this.byType = byType;
    }

    public List<ReportPlaceDto> getPlaces() {
        return places;
    }

    public void setPlaces(List<ReportPlaceDto> places) {
        this.places = places;
    }

    public List<ReportHolderDto> getHolders() {
        return holders;
    }

    public void setHolders(List<ReportHolderDto> holders) {
        this.holders = holders;
    }
}
