package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

/** One field of a portal request type, with the choices a select field offers. */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class RequestFieldDto {
    private String id;
    private String name;
    private boolean required;
    private String kind;
    private List<IssueTypeDto> options = new ArrayList<IssueTypeDto>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public List<IssueTypeDto> getOptions() {
        return options;
    }

    public void setOptions(List<IssueTypeDto> options) {
        this.options = options == null ? new ArrayList<IssueTypeDto>() : options;
    }
}
