package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class CardFoldDto {
    private String folds = "";

    public String getFolds() {
        return folds;
    }

    public void setFolds(String folds) {
        this.folds = folds == null ? "" : folds;
    }
}
