package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class BulkResultDto {
    private int done;
    private List<BulkIssueDto> errors = new ArrayList<BulkIssueDto>();

    public int getDone() {
        return done;
    }

    public void setDone(int done) {
        this.done = done;
    }

    public List<BulkIssueDto> getErrors() {
        return errors;
    }

    public void setErrors(List<BulkIssueDto> errors) {
        this.errors = errors == null ? new ArrayList<BulkIssueDto>() : errors;
    }
}
