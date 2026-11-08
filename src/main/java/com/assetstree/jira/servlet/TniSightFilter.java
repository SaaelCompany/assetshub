package com.assetstree.jira.servlet;

import com.assetstree.jira.model.PortalAssetHold;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Runs before Service Desk handles the create call, including
 * {@code /rest/servicedesk}, and stores the device report for the new issue.
 */
public class TniSightFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        PortalAssetHold.enter();
        try {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest http = (HttpServletRequest) request;
                TniIntake.keep(http);
                PortalIntake.keep(http);
            }
            chain.doFilter(request, response);
        } finally {
            PortalAssetHold.leave();
        }
    }
}
