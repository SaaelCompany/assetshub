package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class FlowBookDto {
    private List<FlowProcessDto> processes = new ArrayList<FlowProcessDto>();

    public List<FlowProcessDto> getProcesses() {
        return processes;
    }

    public void setProcesses(List<FlowProcessDto> processes) {
        this.processes = processes == null ? new ArrayList<FlowProcessDto>() : processes;
    }
}
