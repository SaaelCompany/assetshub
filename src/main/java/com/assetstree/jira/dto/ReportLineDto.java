package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class ReportLineDto {
    private int assetId;
    private String objectKey;
    private String name;
    private String detail;
    private String bucket;
    private String from;
    private String to;
    /** Equipment name. Empty on reports that are not about schedule work. */
    private String asset;
    /** Kind of work, such as maintenance or verification. */
    private String kind;
    /** The scheduled day, {@code yyyy-mm-dd}. */
    private String when;
    /** {@code ontime}, {@code late}, {@code done}, {@code overdue} or {@code expected}. */
    private String state;
    /** The next day this work is expected, when one remains. */
    private String next;
    /** Clock of the occurrence, such as {@code 09:00–10:00}. */
    private String clock;

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
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
    public String getAsset() { return asset; }
    public void setAsset(String asset) { this.asset = asset; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getWhen() { return when; }
    public void setWhen(String when) { this.when = when; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getNext() { return next; }
    public void setNext(String next) { this.next = next; }
    public String getClock() { return clock; }
    public void setClock(String clock) { this.clock = clock; }
}
