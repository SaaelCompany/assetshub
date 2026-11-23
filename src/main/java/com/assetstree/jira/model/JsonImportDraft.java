package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

/** JSON body for CMDB and discovery import. */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class JsonImportDraft {
    private String idempotencyKey;
    private Boolean skipBlank;
    private List<JsonItem> items = new ArrayList<JsonItem>();

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    /** When true, or when omitted, an empty value does not clear the stored one. */
    public Boolean getSkipBlank() {
        return skipBlank;
    }

    public void setSkipBlank(Boolean skipBlank) {
        this.skipBlank = skipBlank;
    }

    public List<JsonItem> getItems() {
        return items;
    }

    public void setItems(List<JsonItem> items) {
        this.items = items == null ? new ArrayList<JsonItem>() : items;
    }
}
