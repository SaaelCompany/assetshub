package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/** Direction for a name sort. Positive is A to Z, negative is Z to A. */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class NameSortDraft {
    private Integer direction;

    public Integer getDirection() {
        return direction;
    }

    public void setDirection(Integer direction) {
        this.direction = direction;
    }
}
