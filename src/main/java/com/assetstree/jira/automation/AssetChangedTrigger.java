package com.assetstree.jira.automation;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.codebarrel.automation.api.thirdparty.EventTriggerRuleComponent;
import com.codebarrel.automation.api.thirdparty.context.ComponentInputs;
import com.codebarrel.automation.api.thirdparty.context.RuleContext;
import com.codebarrel.automation.api.thirdparty.context.result.ExecutionResult;
import com.codebarrel.automation.api.thirdparty.context.result.ExecutionResultBuilder;
import com.assetstree.jira.event.AssetChangedEvent;
import com.assetstree.jira.service.AssetBridge;
import com.assetstree.jira.service.AssetService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Trigger: equipment was created, changed, moved to the trash, or restored. */
public class AssetChangedTrigger implements EventTriggerRuleComponent {
    private static final Set<String> EVENTS = Collections.singleton(AssetChangedEvent.class.getName());

    @Override
    public Set<String> getHandledEventTypes() {
        return EVENTS;
    }

    @Override
    public Set<String> getProjectContext(Object event) {
        if (!(event instanceof AssetChangedEvent)) {
            return Collections.emptySet();
        }
        ProjectManager projects = ComponentAccessor.getProjectManager();
        Project project = projects == null ? null : projects.getProjectByCurrentKey(((AssetChangedEvent) event).getProjectKey());
        if (project == null || project.getId() == null) {
            return Collections.emptySet();
        }
        Set<String> ids = new HashSet<String>();
        ids.add(Long.toString(project.getId().longValue()));
        return ids;
    }

    @Override
    public String serializeEvent(Object event) {
        if (event instanceof AssetChangedEvent) {
            return ((AssetChangedEvent) event).toJson();
        }
        return null;
    }

    @Override
    public ExecutionResult execute(RuleContext ruleContext, ComponentInputs inputs) {
        String body = inputs == null ? "" : inputs.getSerializedEvent();
        String kind = AssetRuleValues.json(body, "event");
        String filter = AssetRuleValues.field(ruleContext.getComponentConfigBean().getValue(), "event");
        if (!AssetRuleValues.matches(filter, kind)) {
            return ExecutionResultBuilder.stopRule();
        }
        String objectKey = AssetRuleValues.json(body, "key");
        int assetId = AssetRuleValues.jsonInt(body, "id");
        ruleContext.getAuditLog().addMessage("asset-tree.automation.ran", objectKey, kind);
        AssetService service = AssetBridge.service();
        IssueManager issues = ComponentAccessor.getIssueManager();
        List<Issue> linked = new ArrayList<Issue>();
        if (service != null && issues != null && assetId > 0) {
            List<Long> ids = service.issueIdsForAsset(assetId);
            for (int i = 0; ids != null && i < ids.size(); i++) {
                Long id = ids.get(i);
                Issue issue = id == null ? null : issues.getIssueObject(id.longValue());
                if (issue != null) {
                    linked.add(issue);
                }
            }
        }
        if (linked.isEmpty()) {
            ruleContext.getAuditLog().addMessage("asset-tree.automation.noIssue", objectKey);
            return ExecutionResultBuilder.continueRule();
        }
        return ExecutionResultBuilder.withIssues(linked).executeConcurrently();
    }
}
