package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class ReportBoardDto {
    private int id;
    private String name;
    private List<ReportBoardPanelDto> panels = new ArrayList<ReportBoardPanelDto>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ReportBoardPanelDto> getPanels() {
        return panels;
    }

    public void setPanels(List<ReportBoardPanelDto> panels) {
        this.panels = panels == null ? new ArrayList<ReportBoardPanelDto>() : panels;
    }
}
