package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class AssetListDto {
    private List<AssetDto> assets = new ArrayList<AssetDto>();
    private List<AssetTypeDto> types = new ArrayList<AssetTypeDto>();
    private List<StatusDto> statuses = new ArrayList<StatusDto>();

    public List<AssetDto> getAssets() {
        return assets;
    }

    public void setAssets(List<AssetDto> assets) {
        this.assets = assets;
    }

    public List<AssetTypeDto> getTypes() {
        return types;
    }

    public void setTypes(List<AssetTypeDto> types) {
        this.types = types;
    }

    public List<StatusDto> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<StatusDto> statuses) {
        this.statuses = statuses;
    }
}
