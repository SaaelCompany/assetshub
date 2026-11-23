package com.assetstree.jira.automation;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.codebarrel.automation.api.thirdparty.EventTriggerRuleComponent;
import com.codebarrel.automation.api.thirdparty.context.ComponentInputs;
import com.codebarrel.automation.api.thirdparty.context.RuleContext;
import com.codebarrel.automation.api.thirdparty.context.result.ExecutionResult;
import com.codebarrel.automation.api.thirdparty.context.result.ExecutionResultBuilder;
import com.codebarrel.automation.api.thirdparty.smartvalues.SmartValueContextProvider;
import com.assetstree.jira.event.ScheduleDueEvent;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Trigger: a calendar day has arrived. One rule serves every work on the calendar. */
public class ScheduleDueTrigger implements EventTriggerRuleComponent, SmartValueContextProvider {
    private static final Set<String> EVENTS = Collections.singleton(ScheduleDueEvent.class.getName());

    @Override
    public Set<String> getHandledEventTypes() {
        return EVENTS;
    }

    @Override
    public Set<String> getProjectContext(Object event) {
        if (!(event instanceof ScheduleDueEvent)) {
            return Collections.emptySet();
        }
        ProjectManager projects = ComponentAccessor.getProjectManager();
        Project project = projects == null ? null : projects.getProjectByCurrentKey(((ScheduleDueEvent) event).getProjectKey());
        if (project == null || project.getId() == null) {
            return Collections.emptySet();
        }
        Set<String> ids = new HashSet<String>();
        ids.add(Long.toString(project.getId().longValue()));
        return ids;
    }

    @Override
    public String serializeEvent(Object event) {
        if (event instanceof ScheduleDueEvent) {
            return ((ScheduleDueEvent) event).toJson();
        }
        return null;
    }

    @Override
    public ExecutionResult execute(RuleContext ruleContext, ComponentInputs inputs) {
        String body = inputs == null ? "" : inputs.getSerializedEvent();
        if (!ScheduleDueEvent.NAME.equals(AssetRuleValues.json(body, "event"))) {
            return ExecutionResultBuilder.stopRule();
        }
        String wanted = AssetRuleValues.field(ruleContext.getComponentConfigBean().getValue(), "kind");
        if (!AssetRuleValues.kindMatches(wanted, AssetRuleValues.json(body, "kind"))) {
            return ExecutionResultBuilder.stopRule();
        }
        String name = AssetRuleValues.json(body, "plan");
        String due = AssetRuleValues.json(body, "due");
        ruleContext.getAuditLog().addMessage("asset-tree.automation.dayRan", name, due);
        Map<String, Serializable> extra = new HashMap<String, Serializable>();
        extra.put("assetId", Integer.valueOf(AssetRuleValues.jsonInt(body, "id")));
        extra.put("assetKey", AssetRuleValues.json(body, "key"));
        extra.put("assetName", AssetRuleValues.json(body, "name"));
        extra.put("assetStatus", AssetRuleValues.json(body, "status"));
        return ExecutionResultBuilder.continueRuleWithAdditionalInputs(extra);
    }

    @Override
    public Map<String, Object> getContext(Map<String, Object> current, ComponentInputs inputs) {
        String body = inputs == null ? "" : inputs.getSerializedEvent();
        Map<String, Object> schedule = new HashMap<String, Object>();
        schedule.put("name", AssetRuleValues.json(body, "plan"));
        schedule.put("date", AssetRuleValues.json(body, "due"));
        schedule.put("note", AssetRuleValues.json(body, "note"));
        schedule.put("kind", AssetRuleValues.json(body, "kind"));
        schedule.put("kindName", AssetRuleValues.json(body, "kindName"));
        schedule.put("assignee", AssetRuleValues.json(body, "assignee"));
        schedule.put("assigneeName", AssetRuleValues.json(body, "assigneeName"));
        schedule.put("start", AssetRuleValues.json(body, "start"));
        schedule.put("end", AssetRuleValues.json(body, "end"));
        schedule.put("asset", AssetRuleValues.json(body, "name"));
        schedule.put("key", AssetRuleValues.json(body, "key"));
        schedule.put("id", Integer.valueOf(AssetRuleValues.jsonInt(body, "id")));
        Map<String, Object> context = new HashMap<String, Object>();
        if (current != null) {
            context.putAll(current);
        }
        context.put("schedule", schedule);
        return context;
    }
}
