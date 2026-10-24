package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class MetaDto {
    private boolean canEdit;
    private boolean canConfigure;
    private boolean canGrant;
    private String version;
    private String locale;
    private String displayName;
    private String userKey;
    private String baseUrl;
    private Map<String, String> i18n = new LinkedHashMap<String, String>();
    private List<ProjectDto> projects = new ArrayList<ProjectDto>();

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public boolean isCanConfigure() {
        return canConfigure;
    }

    public void setCanConfigure(boolean canConfigure) {
        this.canConfigure = canConfigure;
    }

    public boolean isCanGrant() {
        return canGrant;
    }

    public void setCanGrant(boolean canGrant) {
        this.canGrant = canGrant;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUserKey() {
        return userKey;
    }

    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Map<String, String> getI18n() {
        return i18n;
    }

    public void setI18n(Map<String, String> i18n) {
        this.i18n = i18n;
    }

    public List<ProjectDto> getProjects() {
        return projects;
    }

    public void setProjects(List<ProjectDto> projects) {
        this.projects = projects;
    }
}
