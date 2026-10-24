package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class BulkIssueDto {
    private String label;
    private String message;

    public BulkIssueDto() {
    }

    public BulkIssueDto(String label, String message) {
        this.label = label == null ? "" : label;
        this.message = message == null ? "" : message;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
