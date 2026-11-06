package com.assetstree.jira.dto;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class AssetLinkDto {
    private int id;
    private int fromId;
    private int toId;
    private String type;
    private String direction;
    private String inbound;
    private String objectKey;
    private String name;
    private String projectKey;
    private boolean open;
    private List<LinkAttrDto> attributes = new ArrayList<LinkAttrDto>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getFromId() { return fromId; }
    public void setFromId(int fromId) { this.fromId = fromId; }
    public int getToId() { return toId; }
    public void setToId(int toId) { this.toId = toId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction == null ? "" : direction; }
    public String getInbound() { return inbound; }
    public void setInbound(String inbound) { this.inbound = inbound == null ? "" : inbound; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProjectKey() { return projectKey; }
    public void setProjectKey(String projectKey) { this.projectKey = projectKey; }
    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }
    public List<LinkAttrDto> getAttributes() { return attributes; }
    public void setAttributes(List<LinkAttrDto> attributes) {
        this.attributes = attributes == null ? new ArrayList<LinkAttrDto>() : attributes;
    }
}
