package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class SchemaDto {
    private int id;
    private String name;
    private List<String> typeKeys = new ArrayList<String>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getTypeKeys() { return typeKeys; }
    public void setTypeKeys(List<String> typeKeys) {
        this.typeKeys = typeKeys == null ? new ArrayList<String>() : typeKeys;
    }
}
