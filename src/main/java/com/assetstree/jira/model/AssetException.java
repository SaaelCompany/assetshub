package com.assetstree.jira.model;

public final class AssetException extends RuntimeException {
    private final int status;
    private final String messageKey;
    private final Object[] args;

    public AssetException(int status, String messageKey, Object... args) {
        super(messageKey);
        this.status = status;
        this.messageKey = messageKey;
        this.args = args == null ? new Object[0] : args;
    }

    public int getStatus() {
        return status;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }
}
