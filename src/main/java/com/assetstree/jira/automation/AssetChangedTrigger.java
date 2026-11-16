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
import com.codebarrel.automation.api.thirdparty.smartvalues.SmartValueContextProvider;
import com.assetstree.jira.event.AssetChangedEvent;
import com.assetstree.jira.service.AssetBridge;
import com.assetstree.jira.service.AssetService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Trigger: equipment was created, changed, moved to the trash, restored, or its status changed. */
public class AssetChangedTrigger implements EventTriggerRuleComponent, SmartValueContextProvider {
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
        String status = AssetRuleValues.json(body, "status");
        String previous = AssetRuleValues.json(body, "previous");
        String wanted = AssetRuleValues.field(ruleContext.getComponentConfigBean().getValue(), "status");
        if (!AssetRuleValues.statusMatches(wanted, status, previous)) {
            return ExecutionResultBuilder.stopRule();
        }
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
        Map<String, Serializable> extra = assetInputs(body);
        if (linked.isEmpty()) {
            ruleContext.getAuditLog().addMessage("asset-tree.automation.noIssue", objectKey);
            return ExecutionResultBuilder.continueRuleWithAdditionalInputs(extra);
        }
        return ExecutionResultBuilder.withIssues(linked).withInputs(extra).executeConcurrently();
    }

    @Override
    public Map<String, Object> getContext(Map<String, Object> current, ComponentInputs inputs) {
        Map<String, Object> asset = new HashMap<String, Object>();
        String body = inputs == null ? "" : inputs.getSerializedEvent();
        asset.put("id", Integer.valueOf(AssetRuleValues.jsonInt(body, "id")));
        asset.put("key", AssetRuleValues.json(body, "key"));
        asset.put("name", AssetRuleValues.json(body, "name"));
        asset.put("status", AssetRuleValues.json(body, "status"));
        asset.put("previous", AssetRuleValues.json(body, "previous"));
        asset.put("type", AssetRuleValues.json(body, "type"));
        asset.put("project", AssetRuleValues.json(body, "project"));
        asset.put("event", AssetRuleValues.json(body, "event"));
        Map<String, Object> context = new HashMap<String, Object>();
        if (current != null) {
            context.putAll(current);
        }
        context.put("asset", asset);
        return context;
    }

    private static Map<String, Serializable> assetInputs(String body) {
        Map<String, Serializable> extra = new HashMap<String, Serializable>();
        extra.put("assetId", Integer.valueOf(AssetRuleValues.jsonInt(body, "id")));
        extra.put("assetKey", AssetRuleValues.json(body, "key"));
        extra.put("assetName", AssetRuleValues.json(body, "name"));
        extra.put("assetStatus", AssetRuleValues.json(body, "status"));
        return extra;
    }
}
