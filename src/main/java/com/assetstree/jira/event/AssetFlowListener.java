package com.assetstree.jira.event;

import com.atlassian.jira.event.issue.AbstractIssueEventListener;
import com.atlassian.jira.event.issue.IssueEvent;
import com.assetstree.jira.service.AssetBridge;
import com.assetstree.jira.service.AssetService;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * When a request moves from one status to another, equipment already linked
 * to it follows the project diagram. The first status on a new request is
 * not a change: that path is the create rule.
 */
public class AssetFlowListener extends AbstractIssueEventListener {
    private static final Logger log = LoggerFactory.getLogger(AssetFlowListener.class);

    @Override
    public void workflowEvent(IssueEvent event) {
        onStatus(event);
    }

    @Override
    public void issueUpdated(IssueEvent event) {
        onStatus(event);
    }

    private void onStatus(IssueEvent event) {
        if (event == null || event.getIssue() == null || !statusChanged(event)) {
            return;
        }
        AssetService service = AssetBridge.service();
        if (service == null) {
            return;
        }
        try {
            service.syncRequestStatus(event.getUser(), event.getIssue());
        } catch (RuntimeException ex) {
            log.warn("Equipment status was not updated for {}", event.getIssue().getKey(), ex);
        }
    }

    private boolean statusChanged(IssueEvent event) {
        GenericValue changeLog = event.getChangeLog();
        if (changeLog == null) {
            return false;
        }
        try {
            List<GenericValue> items = changeLog.getRelated("ChildChangeItem");
            if (items == null) {
                return false;
            }
            for (GenericValue item : items) {
                if (item == null || !"status".equals(item.getString("field"))) {
                    continue;
                }
                if (blank(item.getString("oldvalue")) && blank(item.getString("oldstring"))) {
                    continue;
                }
                return true;
            }
        } catch (GenericEntityException ex) {
            log.warn("Could not read the issue changelog", ex);
        }
        return false;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
