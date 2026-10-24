package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class BulkDraft {
    private String target;
    private String action;
    private List<Integer> ids = new ArrayList<Integer>();
    private List<String> keys = new ArrayList<String>();
    private String status;
    private String custodianKey;
    private Integer parentId;
    private boolean toRoot;
    private boolean cascade;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(List<Integer> ids) {
        this.ids = ids == null ? new ArrayList<Integer>() : ids;
    }

    public List<String> getKeys() {
        return keys;
    }

    public void setKeys(List<String> keys) {
        this.keys = keys == null ? new ArrayList<String>() : keys;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCustodianKey() {
        return custodianKey;
    }

    public void setCustodianKey(String custodianKey) {
        this.custodianKey = custodianKey;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public boolean isToRoot() {
        return toRoot;
    }

    public void setToRoot(boolean toRoot) {
        this.toRoot = toRoot;
    }

    public boolean isCascade() {
        return cascade;
    }

    public void setCascade(boolean cascade) {
        this.cascade = cascade;
    }
}
