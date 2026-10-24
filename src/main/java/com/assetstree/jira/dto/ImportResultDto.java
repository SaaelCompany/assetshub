package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class ImportResultDto {
    private int created;
    private int updated;
    private List<ImportIssueDto> errors = new ArrayList<ImportIssueDto>();

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public int getUpdated() {
        return updated;
    }

    public void setUpdated(int updated) {
        this.updated = updated;
    }

    public List<ImportIssueDto> getErrors() {
        return errors;
    }

    public void setErrors(List<ImportIssueDto> errors) {
        this.errors = errors == null ? new ArrayList<ImportIssueDto>() : errors;
    }
}
