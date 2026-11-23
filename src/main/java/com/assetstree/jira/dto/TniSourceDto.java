package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/** Directory connection shown to the editor. The password itself is never included. */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class TniSourceDto {
    private String host;
    private int port;
    private Boolean ssl;
    private String base;
    private String account;
    private Boolean passwordSet;
    private String note;
    private long finished;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public Boolean getSsl() { return ssl; }
    public void setSsl(Boolean ssl) { this.ssl = ssl; }
    public String getBase() { return base; }
    public void setBase(String base) { this.base = base; }
    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
    public Boolean getPasswordSet() { return passwordSet; }
    public void setPasswordSet(Boolean passwordSet) { this.passwordSet = passwordSet; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public long getFinished() { return finished; }
    public void setFinished(long finished) { this.finished = finished; }
}
