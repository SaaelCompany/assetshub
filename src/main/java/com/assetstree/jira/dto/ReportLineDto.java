package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class ReportLineDto {
    private int assetId;
    private String objectKey;
    private String name;
    private String detail;

    public ReportLineDto() {
    }

    public ReportLineDto(int assetId, String objectKey, String name, String detail) {
        this.assetId = assetId;
        this.objectKey = objectKey;
        this.name = name;
        this.detail = detail;
    }

    public int getAssetId() { return assetId; }
    public void setAssetId(int assetId) { this.assetId = assetId; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
