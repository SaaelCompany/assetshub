package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class AttributeDto {
    private String name;
    private String fieldKey;
    private String kind;
    private boolean required;
    private boolean showOnIssue;
    private String value;
    private String displayValue;

    public AttributeDto() {
    }

    public AttributeDto(String name, String value) {
        this.name = name;
        this.fieldKey = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public void setFieldKey(String fieldKey) {
        this.fieldKey = fieldKey;
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
    }
}
