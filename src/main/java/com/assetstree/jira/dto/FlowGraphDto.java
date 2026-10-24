package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class FlowGraphDto {
    private List<FlowNodeDto> nodes = new ArrayList<FlowNodeDto>();
    private List<FlowLinkDto> links = new ArrayList<FlowLinkDto>();

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
}
