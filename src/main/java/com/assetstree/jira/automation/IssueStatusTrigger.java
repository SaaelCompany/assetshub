package com.assetstree.jira.automation;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.status.Status;
import com.codebarrel.automation.api.thirdparty.EventTriggerRuleComponent;
import com.codebarrel.automation.api.thirdparty.context.ComponentInputs;
import com.codebarrel.automation.api.thirdparty.context.RuleContext;
import com.codebarrel.automation.api.thirdparty.context.result.ExecutionResult;
import com.codebarrel.automation.api.thirdparty.context.result.ExecutionResultBuilder;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Trigger: a request moved into a status chosen from the workflow list. */
public class IssueStatusTrigger implements EventTriggerRuleComponent {
    private static final Set<String> EVENTS = Collections.singleton(IssueEvent.class.getName());

    @Override
    public Set<String> getHandledEventTypes() {
        return EVENTS;
    }

    @Override
    public Set<String> getProjectContext(Object event) {
        Issue issue = issueOf(event);
        if (issue == null || issue.getProjectId() == null) {
            return Collections.emptySet();
        }
        Set<String> ids = new HashSet<String>();
        ids.add(Long.toString(issue.getProjectId().longValue()));
        return ids;
    }

    @Override
    public String serializeEvent(Object event) {
        if (!(event instanceof IssueEvent)) {
            return null;
        }
        IssueEvent issueEvent = (IssueEvent) event;
        String[] change = transition(issueEvent);
        Issue issue = issueEvent.getIssue();
        if (change == null || issue == null || issue.getId() == null) {
            return null;
        }
        return "{\"event\":\"issue.status\",\"id\":" + issue.getId().longValue()
                + ",\"key\":\"" + escape(issue.getKey())
                + "\",\"status\":\"" + escape(change[0])
                + "\",\"statusName\":\"" + escape(change[1])
                + "\",\"previous\":\"" + escape(change[2]) + "\"}";
    }

    @Override
    public ExecutionResult execute(RuleContext ruleContext, ComponentInputs inputs) {
        String body = inputs == null ? "" : inputs.getSerializedEvent();
        if (!"issue.status".equals(AssetRuleValues.json(body, "event"))) {
            return ExecutionResultBuilder.stopRule();
        }
        String statusId = AssetRuleValues.json(body, "status");
        String statusName = AssetRuleValues.json(body, "statusName");
        String wanted = AssetRuleValues.field(ruleContext.getComponentConfigBean().getValue(), "issueStatus");
        if (!AssetRuleValues.issueStatusMatches(wanted, statusId, statusName)) {
            return ExecutionResultBuilder.stopRule();
        }
        int issueId = AssetRuleValues.jsonInt(body, "id");
        IssueManager manager = ComponentAccessor.getIssueManager();
        Issue issue = manager == null || issueId <= 0 ? null : manager.getIssueObject(Long.valueOf(issueId));
        if (issue == null) {
            ruleContext.getAuditLog().addMessage("asset-tree.automation.issueMiss");
            return ExecutionResultBuilder.stopRule();
        }
        ruleContext.getAuditLog().addMessage("asset-tree.automation.issueRan", issue.getKey(), statusName);
        return ExecutionResultBuilder.withIssues(Collections.singletonList(issue)).executeConcurrently();
    }

    private static Issue issueOf(Object event) {
        if (event instanceof IssueEvent) {
            return ((IssueEvent) event).getIssue();
        }
        return null;
    }

    /** [new id, new name, previous id], or null when the status did not change. */
    private static String[] transition(IssueEvent event) {
        GenericValue changeLog = event.getChangeLog();
        if (changeLog == null) {
            return null;
        }
        try {
            List<GenericValue> items = changeLog.getRelated("ChildChangeItem");
            if (items == null) {
                return null;
            }
            for (int i = 0; i < items.size(); i++) {
                GenericValue item = items.get(i);
                if (item == null || !"status".equals(item.getString("field"))) {
                    continue;
                }
                String previous = text(item, "oldvalue", "oldstring");
                if (previous.isEmpty()) {
                    continue;
                }
                String nextId = text(item, "newvalue", "newstring");
                String nextName = item.getString("newstring");
                Issue issue = event.getIssue();
                Status status = issue == null ? null : issue.getStatus();
                if (nextId.isEmpty() && status != null && status.getId() != null) {
                    nextId = status.getId();
                }
                if (nextName == null || nextName.trim().isEmpty()) {
                    nextName = status == null || status.getName() == null ? nextId : status.getName();
                }
                return new String[] { nextId, nextName.trim(), previous };
            }
        } catch (GenericEntityException ex) {
            return null;
        }
        return null;
    }

    private static String text(GenericValue item, String valueKey, String stringKey) {
        String raw = item.getString(valueKey);
        if (raw != null && raw.trim().matches("\\d+")) {
            return raw.trim();
        }
        String shown = item.getString(stringKey);
        if (shown != null && !shown.trim().isEmpty()) {
            return shown.trim();
        }
        return raw == null ? "" : raw.trim();
    }

    private static String escape(String value) {
        StringBuilder builder = new StringBuilder();
        String raw = value == null ? "" : value;
        for (int i = 0; i < raw.length(); i++) {
            char current = raw.charAt(i);
            if (current == '\\' || current == '"') {
                builder.append('\\').append(current);
            } else if (current == '\n') {
                builder.append("\\n");
            } else if (current >= 32) {
                builder.append(current);
            }
        }
        return builder.toString();
    }
}
