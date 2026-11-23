package com.assetstree.jira.model;

/** One computer read from Active Directory or from an AD/SCCM export. */
public final class TniComputer {
    private final String source;
    private final String identity;
    private final String name;
    private final String detail;
    private final boolean disabled;
    private final String host;

    public TniComputer(String source, String identity, String name, String detail, boolean disabled) {
        this(source, identity, name, detail, disabled, "");
    }

    public TniComputer(String source, String identity, String name, String detail, boolean disabled, String host) {
        this.source = source;
        this.identity = identity;
        this.name = name;
        this.detail = detail;
        this.disabled = disabled;
        this.host = host == null ? "" : host;
    }

    public String getSource() {
        return source;
    }

    public String getIdentity() {
        return identity;
    }

    public String getName() {
        return name;
    }

    public String getDetail() {
        return detail;
    }

    public boolean isDisabled() {
        return disabled;
    }

    /** DNS name used by the availability check. The object name stays the short computer name. */
    public String getHost() {
        return host;
    }

    public String externalKey() {
        String key = source + ":" + identity;
        return key.length() > 120 ? key.substring(0, 120) : key;
    }
}
