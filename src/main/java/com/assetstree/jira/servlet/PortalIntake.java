package com.assetstree.jira.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.assetstree.jira.model.PortalAssetHold;
import com.assetstree.jira.model.PortalAssetMark;
import com.assetstree.jira.service.AssetBridge;
import com.assetstree.jira.service.AssetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

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
            Integer cookieId = PortalAssetMark.parseId(cookie(request, PortalAssetMark.COOKIE));
            Integer refererId = PortalAssetMark.fromReferer(request.getHeader("Referer"));
            Integer id = queryId != null ? queryId
                    : formId != null ? formId
                    : headerId != null ? headerId
                    : cookieId != null ? cookieId
                    : refererId;
            if (id == null) {
                return;
            }
            String uri = request.getRequestURI() == null ? "" : request.getRequestURI();
            boolean portal = uri.indexOf("servicedesk") >= 0;
            boolean marked = queryId != null || formId != null || headerId != null || refererId != null;
            if (!portal && !marked) {
                return;
            }
            boolean explicit = queryId != null;
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
