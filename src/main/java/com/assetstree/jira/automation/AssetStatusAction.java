package com.assetstree.jira.automation;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.codebarrel.automation.api.config.ComponentConfigBean;
import com.codebarrel.automation.api.thirdparty.AutomationRuleComponent;
import com.codebarrel.automation.api.thirdparty.context.ComponentInputs;
import com.codebarrel.automation.api.thirdparty.context.ErrorCollection;
import com.codebarrel.automation.api.thirdparty.context.ErrorCollectionImpl;
import com.codebarrel.automation.api.thirdparty.context.RuleContext;
import com.codebarrel.automation.api.thirdparty.context.result.ExecutionResult;
import com.codebarrel.automation.api.thirdparty.context.result.ExecutionResultBuilder;
import com.assetstree.jira.service.AssetBridge;
import com.assetstree.jira.model.AssetException;
import com.assetstree.jira.model.StatusMoment;
import com.assetstree.jira.service.AssetService;

import java.util.Date;
import java.util.List;

/** Action: set the status of equipment linked to the requests in the rule. */
public class AssetStatusAction implements AutomationRuleComponent {
    @Override
    public ErrorCollection validateConfig(RuleContext context, ComponentConfigBean config) {
        ErrorCollection errors = new ErrorCollectionImpl();
        if (AssetRuleValues.field(config == null ? null : config.getValue(), "status").isEmpty()) {
            errors.addError("status", "asset-tree.automation.statusRequired");
        }
        return errors;
    }

    @Override
    public ExecutionResult execute(RuleContext ruleContext, ComponentInputs inputs) {
        String status = AssetRuleValues.field(ruleContext.getComponentConfigBean().getValue(), "status");
        String whenField = AssetRuleValues.field(ruleContext.getComponentConfigBean().getValue(), "whenField");
        if (status.isEmpty()) {
            ruleContext.getAuditLog().addError("asset-tree.automation.statusRequired");
            return ExecutionResultBuilder.stopRule();
        }
        AssetService service = AssetBridge.service();
        if (service == null) {
            ruleContext.getAuditLog().addError("asset-tree.automation.statusMiss");
            return ExecutionResultBuilder.stopRule();
        }
        JiraAuthenticationContext authentication = ComponentAccessor.getJiraAuthenticationContext();
        ApplicationUser actor = authentication == null ? null : authentication.getLoggedInUser();
        int assetId = AssetRuleValues.assetId(inputs);
        List<Issue> issues = inputs == null ? null : inputs.getIssues();
        int changed = 0;
        try {
            if (assetId > 0) {
                Issue source = issues == null || issues.isEmpty() ? null : issues.get(0);
                changed = service.applyAssetStatus(actor, assetId, status, momentOf(source, whenField, ruleContext));
            } else if (issues != null) {
                for (int i = 0; i < issues.size(); i++) {
                    Issue issue = issues.get(i);
                    if (issue != null && issue.getId() != null) {
                        changed += service.applyIssueEquipmentStatus(actor, issue.getId().longValue(), status,
                                momentOf(issue, whenField, ruleContext));
                    }
                }
            }
        } catch (AssetException ex) {
            if ("asset-tree.error.statusWhen".equals(ex.getMessageKey())) {
                ruleContext.getAuditLog().addError("asset-tree.automation.statusWhenFuture");
            } else if (ex.getStatus() == 400) {
                ruleContext.getAuditLog().addError("asset-tree.automation.statusBad");
            } else {
                ruleContext.getAuditLog().addError("asset-tree.automation.statusDenied");
            }
            return ExecutionResultBuilder.stopRule();
        }
        if (changed == 0) {
            ruleContext.getAuditLog().addMessage("asset-tree.automation.statusMiss");
        } else {
            ruleContext.getAuditLog().addMessage("asset-tree.automation.statusSet", status);
        }
        return ExecutionResultBuilder.continueRule();
    }

    /** The request field holds the real moment. An empty setting, or an empty field, uses now. */
    private Date momentOf(Issue issue, String fieldId, RuleContext ruleContext) {
        if (fieldId == null || fieldId.trim().isEmpty() || issue == null) {
            return null;
        }
        CustomFieldManager manager = ComponentAccessor.getCustomFieldManager();
        String id = fieldId.trim();
        CustomField field = manager == null ? null : manager.getCustomFieldObject(id);
        if (field == null && manager != null && id.matches("\\d+")) {
            field = manager.getCustomFieldObject("customfield_" + id);
        }
        if (field == null) {
            ruleContext.getAuditLog().addMessage("asset-tree.automation.statusWhenMiss");
            return null;
        }
        Object raw = issue.getCustomFieldValue(field);
        if (raw instanceof String && ((String) raw).trim().isEmpty()) {
            raw = null;
        }
        Date parsed = StatusMoment.fromValue(raw);
        if (raw != null && parsed == null) {
            ruleContext.getAuditLog().addMessage("asset-tree.automation.statusWhenMiss");
            return null;
        }
        return StatusMoment.at(parsed == null ? null : Long.valueOf(parsed.getTime()), new Date());
    }
}
