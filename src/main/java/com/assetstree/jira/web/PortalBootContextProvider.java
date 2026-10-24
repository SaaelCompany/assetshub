package com.assetstree.jira.web;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.ExecutingHttpRequest;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.ContextProvider;
import com.assetstree.jira.PluginInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Supplies the portal id, project key, and asset field ids to the portal panels.
 * The customer portal ignores the custom field edit template, so the panel is
 * one of the places the picker script can be attached.
 */
public class PortalBootContextProvider implements ContextProvider {
    private static final Logger log = LoggerFactory.getLogger(PortalBootContextProvider.class);

    @Override
    public void init(Map<String, String> params) throws PluginParseException {
    }

    @Override
    public Map<String, Object> getContextMap(Map<String, Object> context) {
        if (context == null) {
            context = new HashMap<String, Object>();
        }
        String portalId = "";
        String projectKey = "";
        String fieldIds = "";
        String base = "";
        try {
            HttpServletRequest request = ExecutingHttpRequest.get();
            if (request != null && request.getContextPath() != null) {
                base = request.getContextPath();
            }
            int id = PortalLookup.portalId(request);
            if (id > 0) {
                portalId = String.valueOf(id);
                ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
                String key = PortalLookup.projectKeyForPortal(user, id);
                if (key != null) {
                    projectKey = key;
                }
            }
            fieldIds = PortalLookup.fieldIdList();
        } catch (RuntimeException ex) {
            log.warn("Could not prepare the portal asset field", ex);
        }
        context.put("assetTreePortalId", portalId);
        context.put("assetTreeProjectKey", projectKey);
        context.put("assetTreeFieldIds", fieldIds);
        context.put("assetTreeBase", base);
        context.put("assetTreeVersion", PluginInfo.VERSION);
        return context;
    }
}
