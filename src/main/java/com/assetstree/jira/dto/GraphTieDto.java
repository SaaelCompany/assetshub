package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class GraphTieDto {
    private int fromId;
    private int toId;
    private String type = "";
    private String direction = "out";
    private AssetPeerDto from;
    private AssetPeerDto to;

    public int getFromId() {
        return fromId;
    }

    public void setFromId(int fromId) {
        this.fromId = fromId;
    }

    public int getToId() {
        return toId;
    }

    public void setToId(int toId) {
        this.toId = toId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type == null ? "" : type;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction == null || direction.isEmpty() ? "out" : direction;
    }

    public AssetPeerDto getFrom() {
        return from;
    }

    public void setFrom(AssetPeerDto from) {
        this.from = from;
    }

    public AssetPeerDto getTo() {
        return to;
    }

    public void setTo(AssetPeerDto to) {
        this.to = to;
    }
}
