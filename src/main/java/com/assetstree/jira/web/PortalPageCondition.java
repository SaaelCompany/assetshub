package com.assetstree.jira.web;

import com.atlassian.jira.web.ExecutingHttpRequest;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Portal panels belong on the customer portal. A missing request still shows
 * them, because those locations exist only there.
 */
public class PortalPageCondition implements Condition {
    @Override
    public void init(Map<String, String> params) throws PluginParseException {
    }

    @Override
    public boolean shouldDisplay(Map<String, Object> context) {
        HttpServletRequest request = ExecutingHttpRequest.get();
        if (request == null && context != null && context.get("request") instanceof HttpServletRequest) {
            request = (HttpServletRequest) context.get("request");
        }
        if (request == null || request.getRequestURI() == null) {
            return true;
        }
        return request.getRequestURI().contains("/servicedesk/customer/");
    }
}
