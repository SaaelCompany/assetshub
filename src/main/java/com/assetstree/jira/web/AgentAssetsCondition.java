package com.assetstree.jira.web;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractWebCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.user.ApplicationUser;

import java.util.Collection;

/**
 * The agent catalog is for people who can browse a project.
 * Portal customers, who can only create requests, do not see it.
 */
public class AgentAssetsCondition extends AbstractWebCondition {
    @Override
    public boolean shouldDisplay(ApplicationUser user, JiraHelper jiraHelper) {
        if (user == null) {
            return false;
        }
        GlobalPermissionManager globals = ComponentAccessor.getGlobalPermissionManager();
        if (globals.hasPermission(GlobalPermissionKey.ADMINISTER, user)
                || globals.hasPermission(GlobalPermissionKey.SYSTEM_ADMIN, user)) {
            return true;
        }
        PermissionManager permissions = ComponentAccessor.getPermissionManager();
        Project project = jiraHelper == null ? null : jiraHelper.getProject();
        if (project != null) {
            return permissions.hasPermission(ProjectPermissions.BROWSE_PROJECTS, project, user)
                    || permissions.hasPermission(ProjectPermissions.ADMINISTER_PROJECTS, project, user)
                    || GrantSignals.opensMenu(user, project.getKey());
        }
        return hasAny(permissions, ProjectPermissions.BROWSE_PROJECTS, user)
                || hasAny(permissions, ProjectPermissions.ADMINISTER_PROJECTS, user)
                || GrantSignals.opensMenu(user, null);
    }

    private static boolean hasAny(PermissionManager permissions, ProjectPermissionKey key, ApplicationUser user) {
        Collection<Project> projects = permissions.getProjects(key, user);
        return projects != null && !projects.isEmpty();
    }
}
