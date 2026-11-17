package com.assetstree.jira.automation;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.ErrorCollection;
import com.codebarrel.automation.api.config.ComponentConfigBean;
import com.codebarrel.automation.api.thirdparty.AutomationRuleComponent;
import com.codebarrel.automation.api.thirdparty.audit.AuditItemAssociatedType;
import com.codebarrel.automation.api.thirdparty.context.ComponentInputs;
import com.codebarrel.automation.api.thirdparty.context.ErrorCollectionImpl;
import com.codebarrel.automation.api.thirdparty.context.RuleContext;
import com.codebarrel.automation.api.thirdparty.context.result.ExecutionResult;
import com.codebarrel.automation.api.thirdparty.context.result.ExecutionResultBuilder;
import com.assetstree.jira.event.ScheduleDueEvent;
import com.assetstree.jira.model.AssetException;
import com.assetstree.jira.model.CalendarRequest;
import com.assetstree.jira.service.AssetBridge;
import com.assetstree.jira.service.AssetService;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/** Action: create one request from the calendar day and link the equipment when the event has one. */
public class CalendarRequestAction implements AutomationRuleComponent {
    @Override
    public com.codebarrel.automation.api.thirdparty.context.ErrorCollection validateConfig(RuleContext context,
                                                                                           ComponentConfigBean config) {
        com.codebarrel.automation.api.thirdparty.context.ErrorCollection errors = new ErrorCollectionImpl();
        Object value = config == null ? null : config.getValue();
        if (AssetRuleValues.field(value, "issueTypeId").isEmpty()) {
            errors.addError("issueTypeId", "asset-tree.automation.requestTypeRequired");
        }
        return errors;
    }

    @Override
    public ExecutionResult execute(RuleContext ruleContext, ComponentInputs inputs) {
        String body = inputs == null ? "" : inputs.getSerializedEvent();
        if (!ScheduleDueEvent.NAME.equals(AssetRuleValues.json(body, "event"))) {
            ruleContext.getAuditLog().addMessage("asset-tree.automation.requestNoEvent");
            return ExecutionResultBuilder.stopRule();
        }
        String typeId = AssetRuleValues.field(ruleContext.getComponentConfigBean().getValue(), "issueTypeId");
        if (typeId.isEmpty()) {
            ruleContext.getAuditLog().addError("asset-tree.automation.requestTypeRequired");
            return ExecutionResultBuilder.stopRule();
        }
        ApplicationUser actor = actor(inputs);
        if (actor == null) {
            ruleContext.getAuditLog().addError("asset-tree.automation.requestNoActor");
            return ExecutionResultBuilder.stopRule();
        }
        ProjectManager projects = ComponentAccessor.getProjectManager();
        Project project = projects == null ? null : projects.getProjectByCurrentKey(AssetRuleValues.json(body, "project"));
        if (project == null || project.getId() == null) {
            ruleContext.getAuditLog().addError("asset-tree.automation.requestNoProject");
            return ExecutionResultBuilder.stopRule();
        }
        IssueService issueService = ComponentAccessor.getComponent(IssueService.class);
        if (issueService == null) {
            ruleContext.getAuditLog().addError("asset-tree.automation.requestFailed", "");
            return ExecutionResultBuilder.stopRule();
        }
        CalendarRequest text = CalendarRequest.compose(
                AssetRuleValues.json(body, "kindName"),
                AssetRuleValues.json(body, "plan"),
                AssetRuleValues.json(body, "due"),
                AssetRuleValues.json(body, "start"),
                AssetRuleValues.json(body, "end"),
                person(body),
                AssetRuleValues.json(body, "key"),
                AssetRuleValues.json(body, "name"),
                AssetRuleValues.json(body, "note"),
                word(ruleContext, "asset-tree.automation.requestKind"),
                word(ruleContext, "asset-tree.automation.requestDate"),
                word(ruleContext, "asset-tree.automation.requestTime"),
                word(ruleContext, "asset-tree.automation.requestAssignee"),
                word(ruleContext, "asset-tree.automation.requestAsset"),
                word(ruleContext, "asset-tree.automation.requestNote"),
                word(ruleContext, "asset-tree.automation.requestFallback"));
        ApplicationUser assignee = assignee(AssetRuleValues.json(body, "assignee"));
        IssueService.CreateValidationResult ready = validate(issueService, actor, project.getId(), typeId, text, assignee);
        boolean droppedAssignee = false;
        if (!ready.isValid() && assignee != null) {
            ready = validate(issueService, actor, project.getId(), typeId, text, null);
            droppedAssignee = ready.isValid();
        }
        if (!ready.isValid()) {
            ruleContext.getAuditLog().addError("asset-tree.automation.requestFailed", problem(ready.getErrorCollection()));
            return ExecutionResultBuilder.stopRule();
        }
        IssueService.IssueResult created = issueService.create(actor, ready);
        if (created == null || !created.isValid() || created.getIssue() == null) {
            ErrorCollection errors = created == null ? null : created.getErrorCollection();
            ruleContext.getAuditLog().addError("asset-tree.automation.requestFailed", problem(errors));
            return ExecutionResultBuilder.stopRule();
        }
        Issue issue = created.getIssue();
        int assetId = AssetRuleValues.jsonInt(body, "id");
        boolean linked = assetId > 0 && link(actor, assetId, issue);
        if (issue.getId() != null) {
            ruleContext.getAuditLog().addAssociatedItem(AuditItemAssociatedType.ISSUE,
                    String.valueOf(issue.getId()), issue.getKey() == null ? "" : issue.getKey());
        }
        String key = issue.getKey() == null ? "" : issue.getKey();
        if (linked) {
            ruleContext.getAuditLog().addMessage("asset-tree.automation.requestLinked", key);
        } else if (assetId > 0) {
            ruleContext.getAuditLog().addError("asset-tree.automation.requestLinkMiss", key);
        } else {
            ruleContext.getAuditLog().addMessage("asset-tree.automation.requestRan", key);
        }
        if (droppedAssignee) {
            ruleContext.getAuditLog().addMessage("asset-tree.automation.requestUnassigned", key);
        }
        Map<String, Serializable> extra = new HashMap<String, Serializable>();
        extra.put("assetId", Integer.valueOf(assetId));
        extra.put("assetKey", AssetRuleValues.json(body, "key"));
        extra.put("assetName", AssetRuleValues.json(body, "name"));
        extra.put("assetStatus", AssetRuleValues.json(body, "status"));
        return ExecutionResultBuilder.withIssue(issue).withInputs(extra).build();
    }

    private static IssueService.CreateValidationResult validate(IssueService issueService, ApplicationUser actor,
                                                                Long projectId, String typeId, CalendarRequest text,
                                                                ApplicationUser assignee) {
        IssueInputParameters params = issueService.newIssueInputParameters();
        params.setProjectId(projectId);
        params.setIssueTypeId(typeId);
        params.setSummary(text.getSummary());
        params.setDescription(text.getDescription());
        params.setSkipScreenCheck(true);
        params.setApplyDefaultValuesWhenParameterNotProvided(true);
        if (assignee != null && assignee.getName() != null && !assignee.getName().isEmpty()) {
            params.setAssigneeId(assignee.getName());
        }
        return issueService.validateCreate(actor, params);
    }

    private static boolean link(ApplicationUser actor, int assetId, Issue issue) {
        if (issue.getKey() == null || issue.getKey().isEmpty()) {
            return false;
        }
        AssetService service = AssetBridge.service();
        if (service == null) {
            return false;
        }
        try {
            service.linkIssue(actor, assetId, issue.getKey());
            return true;
        } catch (AssetException ex) {
            return ex.getStatus() == 409;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static ApplicationUser actor(ComponentInputs inputs) {
        JiraAuthenticationContext authentication = ComponentAccessor.getJiraAuthenticationContext();
        ApplicationUser actor = authentication == null ? null : authentication.getLoggedInUser();
        if (actor != null || inputs == null || inputs.getInitiatorKey() == null || !inputs.getInitiatorKey().isPresent()) {
            return actor;
        }
        UserManager users = ComponentAccessor.getUserManager();
        return users == null ? null : users.getUserByKey(inputs.getInitiatorKey().get());
    }

    private static ApplicationUser assignee(String stored) {
        if (stored == null || stored.trim().isEmpty()) {
            return null;
        }
        UserManager users = ComponentAccessor.getUserManager();
        if (users == null) {
            return null;
        }
        String name = stored.trim();
        ApplicationUser user = users.getUserByName(name);
        if (user == null) {
            user = users.getUserByKey(name);
        }
        return user;
    }

    private static String person(String body) {
        String name = AssetRuleValues.json(body, "assigneeName");
        if (!name.isEmpty()) {
            return name;
        }
        return AssetRuleValues.json(body, "assignee");
    }

    private static String word(RuleContext ruleContext, String key) {
        if (ruleContext == null || ruleContext.getI18n() == null) {
            return key;
        }
        String text = ruleContext.getI18n().getText(key);
        return text == null || text.isEmpty() ? key : text;
    }

    private static String problem(ErrorCollection errors) {
        if (errors == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        if (errors.getErrorMessages() != null) {
            for (String message : errors.getErrorMessages()) {
                addProblem(out, message);
            }
        }
        if (errors.getErrors() != null) {
            for (String message : errors.getErrors().values()) {
                addProblem(out, message);
            }
        }
        if (out.length() > 300) {
            return out.substring(0, 300);
        }
        return out.toString();
    }

    private static void addProblem(StringBuilder out, String message) {
        if (message == null || message.trim().isEmpty() || out.length() >= 300) {
            return;
        }
        if (out.length() > 0) {
            out.append(' ');
        }
        String raw = message.trim().replace('\n', ' ').replace('\r', ' ');
        out.append(raw);
    }
}
