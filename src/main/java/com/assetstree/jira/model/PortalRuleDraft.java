package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class PortalRuleDraft {
    private int assetId;
    private List<PortalConditionDraft> conditions = new ArrayList<PortalConditionDraft>();

    public int getAssetId() {
        return assetId;
    }

    public void setAssetId(int assetId) {
        this.assetId = assetId;
    }

    public List<PortalConditionDraft> getConditions() {
        return conditions;
    }

    public void setConditions(List<PortalConditionDraft> conditions) {
        this.conditions = conditions == null ? new ArrayList<PortalConditionDraft>() : conditions;
    }
}
