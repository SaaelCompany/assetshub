package com.assetstree.jira.dto;

import com.assetstree.jira.model.PortalConditionDraft;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class PortalRuleDto {
    private int id;
    private int assetId;
    private String assetName;
    private String assetPath;
    private int position;
    private List<PortalConditionDraft> conditions = new ArrayList<PortalConditionDraft>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAssetId() {
        return assetId;
    }

    public void setAssetId(int assetId) {
        this.assetId = assetId;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getAssetPath() {
        return assetPath;
    }

    public void setAssetPath(String assetPath) {
        this.assetPath = assetPath;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public List<PortalConditionDraft> getConditions() {
        return conditions;
    }

    public void setConditions(List<PortalConditionDraft> conditions) {
        this.conditions = conditions == null ? new ArrayList<PortalConditionDraft>() : conditions;
    }
}
