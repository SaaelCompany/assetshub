package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class FieldOrderDto {
    private List<String> fieldKeys = new ArrayList<String>();

    public List<String> getFieldKeys() {
        return fieldKeys;
    }

    public void setFieldKeys(List<String> fieldKeys) {
        this.fieldKeys = fieldKeys == null ? new ArrayList<String>() : fieldKeys;
    }
}
