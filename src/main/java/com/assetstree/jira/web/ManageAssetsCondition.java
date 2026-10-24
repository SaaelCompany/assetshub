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
import com.assetstree.jira.PluginInfo;

import java.util.Collection;

/**
 * Settings are for people who maintain schemas: Jira administrators,
 * project administrators, holders of Manage assets or Edit assets,
 * and groups granted schema or access rights on a project.
 */
public class ManageAssetsCondition extends AbstractWebCondition {
    private static final ProjectPermissionKey EDIT_ASSETS =
            new ProjectPermissionKey(PluginInfo.KEY + ":edit-assets");
    @Override
    public boolean shouldDisplay(ApplicationUser user, JiraHelper jiraHelper) {
        if (user == null) {
            return false;
        }
        GlobalPermissionManager globals = ComponentAccessor.getGlobalPermissionManager();
        if (globals.hasPermission(GlobalPermissionKey.ADMINISTER, user)
                || globals.hasPermission(GlobalPermissionKey.SYSTEM_ADMIN, user)
                || globals.hasPermission(GlobalPermissionKey.of("MANAGE_ASSETS"), user)
                || globals.hasPermission(GlobalPermissionKey.of(PluginInfo.KEY + ":MANAGE_ASSETS"), user)) {
            return true;
        }
        PermissionManager permissions = ComponentAccessor.getPermissionManager();
        return hasAny(permissions, ProjectPermissions.ADMINISTER_PROJECTS, user)
                || hasAny(permissions, EDIT_ASSETS, user)
                || GrantSignals.managesMenu(user);
    }

    private static boolean hasAny(PermissionManager permissions, ProjectPermissionKey key, ApplicationUser user) {
        Collection<Project> projects = permissions.getProjects(key, user);
        return projects != null && !projects.isEmpty();
    }
}
