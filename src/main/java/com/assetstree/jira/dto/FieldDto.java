package com.assetstree.jira.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class FieldDto {
    private String fieldKey;
    private String label;
    private String kind;
    private boolean required;
    private boolean showOnIssue;
    private int position;
    private List<String> options = new ArrayList<String>();

    public String getFieldKey() {
        return fieldKey;
    }

    public void setFieldKey(String fieldKey) {
        this.fieldKey = fieldKey;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isShowOnIssue() {
        return showOnIssue;
    }

    public void setShowOnIssue(boolean showOnIssue) {
        this.showOnIssue = showOnIssue;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options == null ? new ArrayList<String>() : options;
    }
}
