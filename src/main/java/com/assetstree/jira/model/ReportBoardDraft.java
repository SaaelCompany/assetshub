package com.assetstree.jira.model;

import com.assetstree.jira.dto.ReportBoardPanelDto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class ReportBoardDraft {
    private String name;
    private List<ReportBoardPanelDto> panels = new ArrayList<ReportBoardPanelDto>();

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
