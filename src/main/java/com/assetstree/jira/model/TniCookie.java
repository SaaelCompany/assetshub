package com.assetstree.jira.model;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/**
 * The device report travels with the request that creates the portal issue.
 * A separate call to this plugin is often refused for a customer, so the
 * browser attaches the same report as a header and a cookie.
 */
public final class TniCookie {
    public static final String NAME = "asset-tree-tni";
    public static final String HEADER = "X-Asset-Tree-Tni";

    private TniCookie() {
    }

    public static TniSightDraft read(String encoded) {
        if (encoded == null || encoded.trim().isEmpty()) {
            return null;
        }
        String json = decode(encoded);
        if (json.indexOf('{') < 0) {
            return null;
        }
        TniSightDraft draft = new TniSightDraft();
        draft.setProjectKey(text(json, "projectKey"));
        draft.setPortalId(integer(json, "portalId"));
        draft.setUa(text(json, "ua"));
        draft.setPlatform(text(json, "platform"));
        draft.setScreen(text(json, "screen"));
        draft.setGpu(text(json, "gpu"));
        draft.setCores(integer(json, "cores"));
        draft.setMemory(decimal(json, "memory"));
        draft.setTouch(integer(json, "touch"));
        draft.setBattery(bool(json, "battery"));
        draft.setAddresses(texts(json, "addresses"));
        if (blank(draft.getUa()) && blank(draft.getPlatform()) && blank(draft.getScreen())) {
            return null;
        }
        return draft;
    }

    private static String decode(String encoded) {
        try {
            return URLDecoder.decode(encoded, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            return encoded;
        } catch (IllegalArgumentException ex) {
            return encoded;
        }
    }

    private static String text(String json, String name) {
        int at = value(json, name);
        if (at < 0 || at >= json.length() || json.charAt(at) != '"') {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = at + 1; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '"') {
                return out.toString();
            }
            if (ch == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (next == 'u' && i + 5 < json.length()) {
                    try {
                        out.append((char) Integer.parseInt(json.substring(i + 2, i + 6), 16));
                        i += 5;
                        continue;
                    } catch (NumberFormatException ex) {
                        out.append(next);
                        i++;
                        continue;
                    }
                }
                out.append(next == 'n' ? '\n' : next);
                i++;
                continue;
            }
            out.append(ch);
        }
        return "";
    }

    private static List<String> texts(String json, String name) {
        List<String> values = new ArrayList<String>();
        int at = value(json, name);
        if (at < 0 || at >= json.length() || json.charAt(at) != '[') {
            return values;
        }
        int end = json.indexOf(']', at);
        if (end < 0) {
            return values;
        }
        int cursor = at + 1;
        while (cursor < end) {
            while (cursor < end && json.charAt(cursor) != '"') {
                cursor++;
            }
            if (cursor >= end) {
                break;
            }
            String item = text("{\"v\":" + json.substring(cursor, end) + "}", "v");
            if (!item.isEmpty()) {
                values.add(item);
            }
            cursor += Math.max(1, item.length()) + 2;
        }
        return values;
    }

    private static Integer integer(String json, String name) {
        String raw = number(json, name);
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Double decimal(String json, String name) {
        String raw = number(json, name);
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return Double.valueOf(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Boolean bool(String json, String name) {
        int at = value(json, name);
        if (at < 0) {
            return null;
        }
        if (json.startsWith("true", at)) {
            return Boolean.TRUE;
        }
        if (json.startsWith("false", at)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static String number(String json, String name) {
        int at = value(json, name);
        if (at < 0) {
            return "";
        }
        int end = at;
        while (end < json.length()) {
            char ch = json.charAt(end);
            if ((ch < '0' || ch > '9') && ch != '.' && ch != '-') {
                break;
            }
            end++;
        }
        return json.substring(at, end);
    }

    private static int value(String json, String name) {
        String needle = "\"" + name + "\"";
        int from = 0;
        while (from < json.length()) {
            int at = json.indexOf(needle, from);
            if (at < 0) {
                return -1;
            }
            int colon = at + needle.length();
            while (colon < json.length() && json.charAt(colon) <= ' ') {
                colon++;
            }
            if (colon < json.length() && json.charAt(colon) == ':') {
                colon++;
                while (colon < json.length() && json.charAt(colon) <= ' ') {
                    colon++;
                }
                return colon;
            }
            from = at + needle.length();
        }
        return -1;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
