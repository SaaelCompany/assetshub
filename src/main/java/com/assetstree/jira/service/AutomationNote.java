package com.assetstree.jira.service;

/** The sentence written on a linked request when equipment changes. */
public final class AutomationNote {
    private AutomationNote() {
    }

    public static String text(String pattern, String objectKey, String name, String verb) {
        String body = pattern == null ? "" : pattern;
        body = body.replace("{0}", objectKey == null ? "" : objectKey);
        body = body.replace("{1}", name == null ? "" : name);
        body = body.replace("{2}", verb == null ? "" : verb);
        return body;
    }
}
