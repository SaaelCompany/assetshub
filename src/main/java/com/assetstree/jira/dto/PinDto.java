package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class PinDto {
    private int id;
    private int assetId;
    private String assetName;
    private String objectKey;
    private int x;
    private int y;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getAssetId() { return assetId; }
    public void setAssetId(int assetId) { this.assetId = assetId; }
    public String getAssetName() { return assetName; }
    public void setAssetName(String assetName) { this.assetName = assetName; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
}
