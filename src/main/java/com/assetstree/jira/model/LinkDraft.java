package com.assetstree.jira.model;

import com.assetstree.jira.dto.LinkAttrDto;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class LinkDraft {
    private Integer assetId;
    private String type;
    private List<LinkAttrDto> attributes;
    /** When null or true, a blank attribute keeps the stored value. */
    private Boolean skipBlank;

    public Integer getAssetId() { return assetId; }
    public void setAssetId(Integer assetId) { this.assetId = assetId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public List<LinkAttrDto> getAttributes() { return attributes; }
    public void setAttributes(List<LinkAttrDto> attributes) { this.attributes = attributes; }
    public Boolean getSkipBlank() { return skipBlank; }
    public void setSkipBlank(Boolean skipBlank) { this.skipBlank = skipBlank; }
}
