package com.assetstree.jira.event;

import com.atlassian.jira.event.issue.AbstractIssueEventListener;
import com.atlassian.jira.event.issue.IssueEvent;
import com.assetstree.jira.service.AssetBridge;
import com.assetstree.jira.service.AssetService;
import com.assetstree.jira.web.PortalLookup;
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
    public void issueCreated(IssueEvent event) {
        if (event == null || event.getIssue() == null) {
            return;
        }
        AssetService service = AssetBridge.service();
        if (service == null) {
            return;
        }
        try {
            service.attachTni(event.getUser(), event.getIssue());
        } catch (RuntimeException ex) {
            log.warn("TNI did not record a device for {}", event.getIssue().getKey(), ex);
        }
        try {
            service.attachPortalAsset(event.getUser(), event.getIssue());
        } catch (RuntimeException ex) {
            log.warn("Portal asset was not linked to {}", event.getIssue().getKey(), ex);
        }
    }

    /**
     * Jira delivers every issue event here. The base method then calls
     * issueCreated or issueUpdated. Without that dispatch a new portal
     * request never reaches TNI.
     */
    @Override
    public void workflowEvent(IssueEvent event) {
        super.workflowEvent(event);
    }

    @Override
    public void issueUpdated(IssueEvent event) {
        onStatus(event);
        onAssetField(event);
    }

    /** A later edit of the asset field still creates or removes the request link. */
    private void onAssetField(IssueEvent event) {
        if (event == null || event.getIssue() == null || event.getChangeLog() == null) {
            return;
        }
        AssetService service = AssetBridge.service();
        if (service == null) {
            return;
        }
        try {
            List<GenericValue> items = event.getChangeLog().getRelated("ChildChangeItem");
            if (items == null) {
                return;
            }
            List<String> fields = PortalLookup.assetFieldIds();
            for (GenericValue item : items) {
                if (item == null) {
                    continue;
                }
                String fieldId = item.getString("fieldid");
                if (fieldId == null || !fields.contains(fieldId)) {
                    continue;
                }
                String previous = fieldText(item, "oldvalue", "oldstring");
                String next = fieldText(item, "newvalue", "newstring");
                service.syncRequestAsset(event.getUser(), event.getIssue(), previous, next);
                return;
            }
        } catch (GenericEntityException ex) {
            log.warn("Could not read the asset field change", ex);
        } catch (RuntimeException ex) {
            log.warn("Could not link the asset field on {}", event.getIssue().getKey(), ex);
        }
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

    /** The stored id wins over the visible name in the history line. */
    private static String fieldText(GenericValue item, String valueKey, String stringKey) {
        String raw = item.getString(valueKey);
        if (raw != null && raw.trim().matches("\\d+")) {
            return raw.trim();
        }
        String shown = item.getString(stringKey);
        if (shown != null && !shown.trim().isEmpty()) {
            return shown;
        }
        return raw;
    }
}
