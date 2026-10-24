package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class AssetTypeDto {
    private String typeKey;
    private String projectKey;
    private String label;
    private String color;
    private String icon;
    private boolean systemType;
    private boolean location;
    private boolean showInTree;
    private boolean service;
    private String placeCaption;
    private String parentTypeKey;
    private int assetCount;
    private List<FieldDto> fields = new ArrayList<FieldDto>();

    public String getTypeKey() {
        return typeKey;
    }

    public void setTypeKey(String typeKey) {
        this.typeKey = typeKey;
    }

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

    public boolean isSystemType() {
        return systemType;
    }

    public void setSystemType(boolean systemType) {
        this.systemType = systemType;
    }

    public int getAssetCount() {
        return assetCount;
    }

    public void setAssetCount(int assetCount) {
        this.assetCount = assetCount;
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

    public boolean isShowInTree() {
        return showInTree;
    }

    public void setShowInTree(boolean showInTree) {
        this.showInTree = showInTree;
    }

    public boolean isService() {
        return service;
    }

    public void setService(boolean service) {
        this.service = service;
    }

    public String getPlaceCaption() {
        return placeCaption;
    }

    public void setPlaceCaption(String placeCaption) {
        this.placeCaption = placeCaption;
    }

    public String getParentTypeKey() {
        return parentTypeKey;
    }

    public void setParentTypeKey(String parentTypeKey) {
        this.parentTypeKey = parentTypeKey;
    }

    public List<FieldDto> getFields() {
        return fields;
    }

    public void setFields(List<FieldDto> fields) {
        this.fields = fields;
    }
}
