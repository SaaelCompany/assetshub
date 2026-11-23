package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

/** Several equipment rows dropped together in the hierarchy. */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class GroupMoveDraft {
    private List<Integer> ids = new ArrayList<Integer>();
    private Integer parentId;
    private Integer index;
    /** Empty keeps each object's type. A type key moves the whole group onto that type. */
    private String typeKey;

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(List<Integer> ids) {
        this.ids = ids == null ? new ArrayList<Integer>() : ids;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getTypeKey() {
        return typeKey;
    }

    public void setTypeKey(String typeKey) {
        this.typeKey = typeKey;
    }
}
