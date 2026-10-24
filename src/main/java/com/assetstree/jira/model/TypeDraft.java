package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class TypeDraft {
    private String label;
    private String color;
    private String icon;
    private String projectKey;
    private boolean location;
    private Boolean showInTree;
    private Boolean service;
    private String placeCaption;
    private String parentTypeKey;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public boolean isLocation() {
        return location;
    }

    public void setLocation(boolean location) {
        this.location = location;
    }

    /** Null when the request did not mention the flag, so a partial update leaves it alone. */
    public Boolean getShowInTree() {
        return showInTree;
    }

    public void setShowInTree(Boolean showInTree) {
        this.showInTree = showInTree;
    }

    /** Null when the request did not mention maintenance, so a partial update leaves it alone. */
    public Boolean getService() {
        return service;
    }

    public void setService(Boolean service) {
        this.service = service;
    }

    /** Null when the request did not mention the caption, so a partial update leaves it alone. */
    public String getPlaceCaption() {
        return placeCaption;
    }

    public void setPlaceCaption(String placeCaption) {
        this.placeCaption = placeCaption;
    }

    /** Null when the request did not mention the parent, so a partial update leaves it alone. */
    public String getParentTypeKey() {
        return parentTypeKey;
    }

    public void setParentTypeKey(String parentTypeKey) {
        this.parentTypeKey = parentTypeKey;
    }
}
