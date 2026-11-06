package com.assetstree.jira.model;

/**
 * The address Jira itself saw on the connection that created the request.
 * This is the peer of that connection, not an address discovered on the client.
 */
public final class TniPeer {
    private TniPeer() {
    }

    public static String address(String remote) {
        if (remote == null) {
            return "";
        }
        String text = remote.trim();
        if (text.startsWith("/")) {
            text = text.substring(1).trim();
        }
        int zone = text.indexOf('%');
        if (zone > 0) {
            text = text.substring(0, zone);
        }
        if (text.regionMatches(true, 0, "::ffff:", 0, 7)) {
            text = text.substring(7);
        }
        if (text.startsWith("[") && text.indexOf(']') > 1) {
            text = text.substring(1, text.indexOf(']'));
        }
        return TniAddress.allowed(text) ? text : "";
    }
}
