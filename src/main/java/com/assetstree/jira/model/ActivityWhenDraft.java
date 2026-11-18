package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/** The actual moment stored on one status history row. */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class ActivityWhenDraft {
    private Long created;

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }
}
