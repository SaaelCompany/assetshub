package com.assetstree.jira.web;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.ExecutingHttpRequest;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.ContextProvider;
import com.assetstree.jira.dto.AssetDto;
import com.assetstree.jira.service.AssetBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Puts the issue and its linked assets into the issue-view panel.
 * The panel body is drawn from this list, so the assets are visible even when
 * the issue page inserts the panel after the page scripts have already started.
 */
public class IssuePanelContextProvider implements ContextProvider {
    private static final Logger log = LoggerFactory.getLogger(IssuePanelContextProvider.class);

    @Override
    public void init(Map<String, String> params) throws PluginParseException {
    }

    @Override
    public Map<String, Object> getContextMap(Map<String, Object> context) {
        if (context == null) {
            return new java.util.HashMap<String, Object>();
        }
        Issue issue = resolveIssue(context);
        if (issue == null || issue.getId() == null) {
            return context;
        }
        context.put("issue", issue);
        context.put("assetTreeIssueId", issue.getId());
        context.put("assetTreeIssueKey", issue.getKey());
        if (issue.getProjectObject() != null) {
            context.put("assetTreeProjectKey", issue.getProjectObject().getKey());
        }
        context.put("assetTreeBase", contextPath());
        try {
            ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
            List<AssetDto> assets = AssetBridge.service().assetsForIssue(user, issue.getId().longValue());
            context.put("assetTreeAssets", assets == null ? Collections.<AssetDto>emptyList() : assets);
            context.put("assetTreeReady", Boolean.TRUE);
        } catch (RuntimeException ex) {
            log.warn("Could not load linked assets for {}", issue.getKey(), ex);
        }
        return context;
    }

    private Issue resolveIssue(Map<String, Object> context) {
        if (context == null) {
            return null;
        }
        Issue direct = asIssue(context.get("issue"));
        if (direct != null) {
            return direct;
        }
        Object helper = context.get("helper");
        if (!(helper instanceof JiraHelper)) {
            return null;
        }
        JiraHelper jiraHelper = (JiraHelper) helper;
        if (jiraHelper.getContextParams() != null) {
            Issue fromHelper = asIssue(jiraHelper.getContextParams().get("issue"));
            if (fromHelper != null) {
                return fromHelper;
            }
        }
        return issueFromRequest(jiraHelper.getRequest());
    }

    private Issue issueFromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        IssueManager issues = ComponentAccessor.getIssueManager();
        String id = first(request, "id", "issueId");
        if (id.matches("\\d+")) {
            Issue byId = issues.getIssueObject(Long.valueOf(id));
            if (byId != null) {
                return byId;
            }
        }
        String key = first(request, "issueKey");
        if (!key.isEmpty()) {
            return issues.getIssueByCurrentKey(key);
        }
        return null;
    }

    private String contextPath() {
        try {
            HttpServletRequest request = ExecutingHttpRequest.get();
            if (request != null && request.getContextPath() != null) {
                return request.getContextPath();
            }
        } catch (RuntimeException ex) {
            log.debug("No request context path for the asset panel", ex);
        }
        return "";
    }

    private Issue asIssue(Object value) {
        return value instanceof Issue ? (Issue) value : null;
    }

    private String first(HttpServletRequest request, String... names) {
        for (String name : names) {
            String value = request.getParameter(name);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }
}
