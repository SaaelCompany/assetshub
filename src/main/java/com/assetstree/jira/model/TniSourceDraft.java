package com.assetstree.jira.model;

public class TniSourceDraft {
    private String host;
    private Integer port;
    private Boolean ssl;
    private String base;
    private String account;
    private String password;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
    public Boolean getSsl() { return ssl; }
    public void setSsl(Boolean ssl) { this.ssl = ssl; }
    public String getBase() { return base; }
    public void setBase(String base) { this.base = base; }
    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
