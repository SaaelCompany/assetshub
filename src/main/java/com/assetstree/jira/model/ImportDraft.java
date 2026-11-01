package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class ImportDraft {
    private String csv;
    private String content;
    private String name;
    private Integer offset;
    private String uploadId;
    private String idempotencyKey;
    private Boolean chunked;

    public String getCsv() {
        return csv;
    }

    public void setCsv(String csv) {
        this.csv = csv;
    }

    /** Base64 of an .xlsx book. When present, this is the file to import. */
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    /** When true, one request writes only a slice and returns an upload id. */
    public Boolean getChunked() {
        return chunked;
    }

    public void setChunked(Boolean chunked) {
        this.chunked = chunked;
    }
}
