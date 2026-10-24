package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class FlowProcessDto {
    private String id = "";
    private String name = "";
    private List<FlowNodeDto> nodes = new ArrayList<FlowNodeDto>();
    private List<FlowLinkDto> links = new ArrayList<FlowLinkDto>();
    private String removedStatus = "";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id == null ? "" : id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public List<FlowNodeDto> getNodes() {
        return nodes;
    }

    public void setNodes(List<FlowNodeDto> nodes) {
        this.nodes = nodes == null ? new ArrayList<FlowNodeDto>() : nodes;
    }

    public List<FlowLinkDto> getLinks() {
        return links;
    }

    public void setLinks(List<FlowLinkDto> links) {
        this.links = links == null ? new ArrayList<FlowLinkDto>() : links;
    }

    public String getRemovedStatus() {
        return removedStatus;
    }

    public void setRemovedStatus(String removedStatus) {
        this.removedStatus = removedStatus == null ? "" : removedStatus;
    }
}
