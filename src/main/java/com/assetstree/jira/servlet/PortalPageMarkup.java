package com.assetstree.jira.servlet;

import com.assetstree.jira.PluginInfo;

import java.nio.charset.StandardCharsets;

/**
 * Inserts the portal picker bootstrap just before the closing body tag.
 * Pages without that tag, including JSON responses, are left unchanged.
 */
public final class PortalPageMarkup {
    private static final byte[] BODY_END = "</body>".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] MARKER = "asset-tree-portal-boot".getBytes(StandardCharsets.US_ASCII);

    private PortalPageMarkup() {
    }

    public static byte[] insertBoot(byte[] body, byte[] snippet) {
        if (body == null || snippet == null || snippet.length == 0) {
            return body;
        }
        if (indexOf(body, MARKER) >= 0) {
            return body;
        }
        int at = lastIndexOf(body, BODY_END);
        if (at < 0) {
            return body;
        }
        byte[] out = new byte[body.length + snippet.length];
        System.arraycopy(body, 0, out, 0, at);
        System.arraycopy(snippet, 0, out, at, snippet.length);
        System.arraycopy(body, at, out, at + snippet.length, body.length - at);
        return out;
    }

    public static byte[] snippet(String contextPath, int portalId, String projectKey, String fieldIds) {
        String base = contextPath == null ? "" : contextPath;
        String html = "<div id=\"asset-tree-portal-boot\" hidden data-portal=\""
                + escape(portalId > 0 ? String.valueOf(portalId) : "")
                + "\" data-project=\"" + escape(projectKey)
                + "\" data-fields=\"" + escape(fieldIds)
                + "\"></div><link rel=\"stylesheet\" href=\"" + escape(base)
                + "/download/resources/" + PluginInfo.KEY
                + ":asset-tree-field/asset-field.css?v=" + PluginInfo.VERSION
                + "\"><script src=\"" + escape(base)
                + "/download/resources/" + PluginInfo.KEY
                + ":asset-tree-field/asset-field.js?v=" + PluginInfo.VERSION
                + "\"></script>";
        return html.getBytes(StandardCharsets.UTF_8);
    }

    static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static int indexOf(byte[] source, byte[] needle) {
        if (needle.length == 0 || source.length < needle.length) {
            return -1;
        }
        int last = source.length - needle.length;
        for (int i = 0; i <= last; i++) {
            if (matches(source, i, needle)) {
                return i;
            }
        }
        return -1;
    }

    private static int lastIndexOf(byte[] source, byte[] needle) {
        if (needle.length == 0 || source.length < needle.length) {
            return -1;
        }
        for (int i = source.length - needle.length; i >= 0; i--) {
            if (matches(source, i, needle)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean matches(byte[] source, int offset, byte[] needle) {
        for (int i = 0; i < needle.length; i++) {
            if (source[offset + i] != needle[i]) {
                return false;
            }
        }
        return true;
    }
}
