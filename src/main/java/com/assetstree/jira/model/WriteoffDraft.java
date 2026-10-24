package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class WriteoffDraft {
    private String reason;
    private String approverKey;
    private Boolean approve;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getApproverKey() { return approverKey; }
    public void setApproverKey(String approverKey) { this.approverKey = approverKey; }
    public Boolean getApprove() { return approve; }
    public void setApprove(Boolean approve) { this.approve = approve; }
}
