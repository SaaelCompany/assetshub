package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.LinkedHashMap;
import java.util.Map;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class JsonItem {
    private String key;
    private String externalId;
    private String name;
    private String type;
    private String status;
    private String place;
    private String custodian;
    private String issued;
    private String description;
    private Map<String, String> fields = new LinkedHashMap<String, String>();

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }
    public String getCustodian() { return custodian; }
    public void setCustodian(String custodian) { this.custodian = custodian; }
    public String getIssued() { return issued; }
    public void setIssued(String issued) { this.issued = issued; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Map<String, String> getFields() { return fields; }
    public void setFields(Map<String, String> fields) {
        this.fields = fields == null ? new LinkedHashMap<String, String>() : fields;
    }
}
