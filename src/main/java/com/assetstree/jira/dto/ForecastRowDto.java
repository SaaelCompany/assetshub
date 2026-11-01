package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class ForecastRowDto {
    private int assetId;
    private String objectKey;
    private String name;
    private String replaceOn;
    private int monthsLeft;
    private String book;

    public int getAssetId() { return assetId; }
    public void setAssetId(int assetId) { this.assetId = assetId; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getReplaceOn() { return replaceOn; }
    public void setReplaceOn(String replaceOn) { this.replaceOn = replaceOn; }
    public int getMonthsLeft() { return monthsLeft; }
    public void setMonthsLeft(int monthsLeft) { this.monthsLeft = monthsLeft; }
    public String getBook() { return book; }
    public void setBook(String book) { this.book = book; }
}
