package com.assetstree.jira.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.assetstree.jira.model.PortalAssetHold;
import com.assetstree.jira.model.PortalAssetMark;
import com.assetstree.jira.service.AssetBridge;
import com.assetstree.jira.service.AssetService;
import com.assetstree.jira.web.PortalLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads the asset chosen in the profile from the call that creates the request.
 * Portal create is often {@code /rest/servicedesk}, which never reaches the
 * customer-portal page filter.
 */
public final class PortalIntake {
    private static final Logger log = LoggerFactory.getLogger(PortalIntake.class);

    private PortalIntake() {
    }

    public static void keep(HttpServletRequest request) {
        if (request == null) {
            return;
        }
        try {
            Integer queryId = PortalAssetMark.fromQuery(request.getQueryString());
            Integer formId = formAsset(request);
            Integer headerId = PortalAssetMark.parseId(request.getHeader(PortalAssetMark.HEADER));
            Integer fieldId = assetField(request);
            Integer cookieId = PortalAssetMark.parseId(cookie(request, PortalAssetMark.COOKIE));
            Integer refererId = PortalAssetMark.fromReferer(request.getHeader("Referer"));
            Integer id = queryId != null ? queryId
                    : formId != null ? formId
                    : headerId != null ? headerId
                    : fieldId != null ? fieldId
                    : cookieId != null ? cookieId
                    : refererId;
            if (id == null) {
                return;
            }
            String uri = request.getRequestURI() == null ? "" : request.getRequestURI();
            boolean portal = uri.indexOf("servicedesk") >= 0;
            boolean marked = queryId != null || formId != null || headerId != null || fieldId != null || refererId != null;
            if (!portal && !marked) {
                return;
            }
            boolean explicit = queryId != null || formId != null || headerId != null || fieldId != null;
            boolean post = "POST".equalsIgnoreCase(request.getMethod());
            ApplicationUser user = loggedIn();
            AssetService service = AssetBridge.service();
            if (!explicit && user != null && service != null && service.portalAssetSettled(user, id.intValue())) {
                return;
            }
            if (post) {
                PortalAssetHold.bindRequest(id.intValue());
            }
            if (user != null && service != null) {
                service.rememberPortalAsset(user, id.intValue());
            }
        } catch (RuntimeException ex) {
            log.debug("Portal asset was not carried", ex);
        }
    }

    private static ApplicationUser loggedIn() {
        try {
            return ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static Integer formAsset(HttpServletRequest request) {
        String type = request.getContentType();
        if (type == null) {
            return null;
        }
        String lower = type.toLowerCase(Locale.US);
        if (!lower.contains("application/x-www-form-urlencoded") && !lower.contains("multipart/")) {
            return null;
        }
        try {
            return PortalAssetMark.parseId(request.getParameter(PortalAssetMark.PARAM));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /** The asset selected in the request field, including a line such as «Ноутбук / Компьютер admin». */
    private static Integer assetField(HttpServletRequest request) {
        if (!formContent(request)) {
            return null;
        }
        try {
            List<String> fields = PortalLookup.assetFieldIds();
            if (fields.isEmpty()) {
                return null;
            }
            String text = "";
            for (int i = 0; i < fields.size(); i++) {
                String value = request.getParameter(fields.get(i));
                Integer id = PortalAssetMark.parseId(value);
                if (id != null) {
                    return id;
                }
                if (value != null && !value.trim().isEmpty() && !"-1".equals(value.trim())) {
                    text = value.trim();
                }
            }
            if (text.isEmpty()) {
                return null;
            }
            AssetService service = AssetBridge.service();
            if (service == null) {
                return null;
            }
            ApplicationUser user = loggedIn();
            String projectKey = "";
            int portalId = PortalLookup.portalId(request);
            if (portalId > 0) {
                String key = PortalLookup.projectKeyForPortal(user, portalId);
                if (key != null) {
                    projectKey = key;
                }
            }
            return service.resolvePortalAsset(projectKey, text, formContext(request, fields));
        } catch (RuntimeException ex) {
            log.debug("The asset chosen on the request form was not read", ex);
            return null;
        }
    }

    private static String formContext(HttpServletRequest request, List<String> assetFields) {
        StringBuilder builder = new StringBuilder();
        Map<String, String[]> params;
        try {
            params = request.getParameterMap();
        } catch (RuntimeException ex) {
            return "";
        }
        if (params == null) {
            return "";
        }
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            if (entry.getKey() == null || assetFields.contains(entry.getKey()) || entry.getValue() == null) {
                continue;
            }
            for (int i = 0; i < entry.getValue().length; i++) {
                String value = entry.getValue()[i];
                if (value == null || value.trim().isEmpty() || value.length() > 500 || builder.length() > 4000) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(value.trim());
            }
        }
        return builder.toString();
    }

    private static boolean formContent(HttpServletRequest request) {
        String type = request.getContentType();
        if (type == null) {
            return false;
        }
        String lower = type.toLowerCase(Locale.US);
        return lower.contains("application/x-www-form-urlencoded") || lower.contains("multipart/");
    }

    private static String cookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || name == null) {
            return null;
        }
        for (int i = 0; i < cookies.length; i++) {
            Cookie cookie = cookies[i];
            if (cookie != null && name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
