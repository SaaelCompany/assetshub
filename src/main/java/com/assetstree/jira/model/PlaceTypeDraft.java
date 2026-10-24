package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class PlaceTypeDraft {
    private String typeKey;
    private List<String> typeKeys;

    public String getTypeKey() {
        return typeKey;
    }

    public void setTypeKey(String typeKey) {
        this.typeKey = typeKey;
    }

    public List<String> getTypeKeys() {
        return typeKeys;
    }

    public void setTypeKeys(List<String> typeKeys) {
        this.typeKeys = typeKeys;
    }
}
