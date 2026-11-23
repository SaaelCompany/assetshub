package com.assetstree.jira.model;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * The asset a customer picked before the portal created a request.
 * The request type does not carry an asset field, so the id travels
 * on the create call itself.
 */
public final class PortalAssetMark {
    public static final String COOKIE = "assethub-portal-asset";
    public static final String HEADER = "X-AssetHub-Asset";
    public static final String PARAM = "assethubAsset";
    public static final String PROPERTY = "assethub.portal.asset";
    public static final String DONE = "assethub.portal.done";
    public static final long KEPT_MS = 2L * 60L * 60L * 1000L;
    private static final long CLOCK_SLACK_MS = 2L * 60L * 1000L;

    private PortalAssetMark() {
    }

    public static Integer parseId(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return null;
        }
        int cut = text.indexOf(';');
        if (cut >= 0) {
            text = text.substring(0, cut).trim();
        }
        if (text.indexOf('%') >= 0) {
            try {
                text = URLDecoder.decode(text, "UTF-8").trim();
            } catch (IllegalArgumentException ex) {
                return null;
            } catch (UnsupportedEncodingException ex) {
                return null;
            }
        }
        if (text.isEmpty() || text.length() > 9) {
            return null;
        }
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch < '0' || ch > '9') {
                return null;
            }
        }
        try {
            int id = Integer.parseInt(text);
            return id > 0 ? Integer.valueOf(id) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static Integer fromQuery(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        String[] parts = query.split("&");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.startsWith("asset=")) {
                return parseId(part.substring("asset=".length()));
            }
        }
        return null;
    }

    public static Integer fromReferer(String referer) {
        if (referer == null) {
            return null;
        }
        int queryAt = referer.indexOf('?');
        if (queryAt < 0 || queryAt >= referer.length() - 1) {
            return null;
        }
        String query = referer.substring(queryAt + 1);
        int hash = query.indexOf('#');
        if (hash >= 0) {
            query = query.substring(0, hash);
        }
        return fromQuery(query);
    }

    public static String pendingValue(int assetId, long now) {
        return assetId + ":" + now;
    }

    public static Integer pendingId(String raw, long now) {
        if (raw == null) {
            return null;
        }
        int colon = raw.indexOf(':');
        if (colon <= 0 || colon >= raw.length() - 1) {
            return null;
        }
        Integer id = parseId(raw.substring(0, colon));
        if (id == null) {
            return null;
        }
        long at;
        try {
            at = Long.parseLong(raw.substring(colon + 1).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
        long age = now - at;
        if (age > KEPT_MS || age < -CLOCK_SLACK_MS) {
            return null;
        }
        return id;
    }

    /** A brand-new request may not have a created timestamp yet. */
    public static boolean fresh(Long createdMillis, long now) {
        if (createdMillis == null) {
            return true;
        }
        long age = now - createdMillis.longValue();
        return age >= -CLOCK_SLACK_MS && age <= KEPT_MS;
    }
}
