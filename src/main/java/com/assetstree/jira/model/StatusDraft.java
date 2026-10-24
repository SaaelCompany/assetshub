package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class StatusDraft {
    private String label;
    private String category;
    /** Absent when a rename or a color change must leave the summary flag alone. */
    private Boolean inSummary;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getInSummary() {
        return inSummary;
    }

    public void setInSummary(Boolean inSummary) {
        this.inSummary = inSummary;
    }
}
