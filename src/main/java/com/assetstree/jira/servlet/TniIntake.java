package com.assetstree.jira.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.assetstree.jira.model.TniCookie;
import com.assetstree.jira.model.TniHold;
import com.assetstree.jira.model.TniSightDraft;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Keeps the browser report that arrived on the request which is about to
 * create the portal issue. The issue event can run after this filter returns.
 */
public final class TniIntake {
    private TniIntake() {
    }

    public static void keep(HttpServletRequest request) {
        if (request == null || !"POST".equalsIgnoreCase(request.getMethod())) {
            return;
        }
        String raw = request.getHeader(TniCookie.HEADER);
        if (raw == null || raw.trim().isEmpty()) {
            raw = cookie(request, TniCookie.NAME);
        }
        TniSightDraft draft = TniCookie.read(raw);
        if (draft == null) {
            return;
        }
        try {
            ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
            if (user != null && user.getKey() != null) {
                TniHold.keep(user.getKey(), draft);
            }
        } catch (RuntimeException ex) {
            /* The issue listener still reads the same header from the request. */
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
