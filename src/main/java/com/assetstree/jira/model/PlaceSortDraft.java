package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

/** Name order for the equipment of one type on one place. Ids are already sorted. */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class PlaceSortDraft {
    private String typeKey;
    private List<Integer> ids = new ArrayList<Integer>();

    public String getTypeKey() {
        return typeKey;
    }

    public void setTypeKey(String typeKey) {
        this.typeKey = typeKey;
    }

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(List<Integer> ids) {
        this.ids = ids == null ? new ArrayList<Integer>() : ids;
    }
}
