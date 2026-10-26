package com.assetstree.jira.web;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.assetstree.jira.ao.ProjectGrantEntity;
import com.assetstree.jira.model.GrantCaps;
import com.assetstree.jira.service.JiraServices;
import net.java.ao.Query;

/**
 * Menu visibility for grants stored on a project.
 * A row names a group or one user. A failure here hides the extra item and leaves the Jira permission checks in place.
 */
final class GrantSignals {
    private GrantSignals() {
    }

    static boolean opensMenu(ApplicationUser user, String projectKey) {
        return hasGrant(user, projectKey, false);
    }

    static boolean managesMenu(ApplicationUser user) {
        return hasGrant(user, null, true);
    }

    private static boolean hasGrant(ApplicationUser user, String projectKey, boolean manageOnly) {
        if (user == null) {
            return false;
        }
        try {
            ActiveObjects ao = JiraServices.activeObjects();
            GroupManager groups = ComponentAccessor.getGroupManager();
            if (ao == null || groups == null) {
                return false;
            }
            ProjectGrantEntity[] rows = projectKey == null || projectKey.isEmpty()
                    ? ao.find(ProjectGrantEntity.class)
                    : ao.find(ProjectGrantEntity.class, Query.select().where("PROJECT_KEY = ?", projectKey));
            for (ProjectGrantEntity row : rows) {
                if (row == null) {
                    continue;
                }
                String caps = row.getCaps();
                if (caps == null || caps.trim().isEmpty()) {
                    caps = GrantCaps.fromLevel(row.getLevel());
                }
                if (manageOnly && !GrantCaps.has(caps, GrantCaps.TYPES) && !GrantCaps.has(caps, GrantCaps.ASSETS)) {
                    continue;
                }
                if (caps == null || caps.isEmpty()) {
                    continue;
                }
                if (grantApplies(user, row, groups)) {
                    return true;
                }
            }
        } catch (RuntimeException ex) {
            return false;
        }
        return false;
    }

    private static boolean grantApplies(ApplicationUser user, ProjectGrantEntity row, GroupManager groups) {
        String userKey = row.getUserKey();
        if (userKey != null && !userKey.trim().isEmpty()) {
            return user.getKey() != null && user.getKey().equals(userKey.trim());
        }
        String groupName = row.getGroupName();
        if (groupName == null || groupName.trim().isEmpty() || groups == null) {
            return false;
        }
        try {
            return groups.isUserInGroup(user, groupName.trim());
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
