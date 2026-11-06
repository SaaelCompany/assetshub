package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

/** Fields a customer filled in for one request type. */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class PortalRequestDraft {
    private String requestTypeId = "";
    private List<Value> values = new ArrayList<Value>();

    public String getRequestTypeId() {
        return requestTypeId;
    }

    public void setRequestTypeId(String requestTypeId) {
        this.requestTypeId = requestTypeId == null ? "" : requestTypeId;
    }

    public List<Value> getValues() {
        return values;
    }

    public void setValues(List<Value> values) {
        this.values = values == null ? new ArrayList<Value>() : values;
    }

    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class Value {
        private String id = "";
        private String value = "";

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id == null ? "" : id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value == null ? "" : value;
        }
    }
}
