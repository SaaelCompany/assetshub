package com.assetstree.jira.automation;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
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
import com.assetstree.jira.service.AssetService;

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
        if (status.isEmpty()) {
            ruleContext.getAuditLog().addError("asset-tree.automation.statusRequired");
            return ExecutionResultBuilder.stopRule();
        }
        List<Issue> issues = inputs == null ? null : inputs.getIssues();
        if (issues == null || issues.isEmpty()) {
            ruleContext.getAuditLog().addMessage("asset-tree.automation.statusMiss");
            return ExecutionResultBuilder.continueRule();
        }
        AssetService service = AssetBridge.service();
        if (service == null) {
            ruleContext.getAuditLog().addError("asset-tree.automation.statusMiss");
            return ExecutionResultBuilder.stopRule();
        }
        JiraAuthenticationContext authentication = ComponentAccessor.getJiraAuthenticationContext();
        ApplicationUser actor = authentication == null ? null : authentication.getLoggedInUser();
        int changed = 0;
        try {
            for (int i = 0; i < issues.size(); i++) {
                Issue issue = issues.get(i);
                if (issue != null && issue.getId() != null) {
                    changed += service.applyIssueEquipmentStatus(actor, issue.getId().longValue(), status);
                }
            }
        } catch (AssetException ex) {
            if (ex.getStatus() == 400) {
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
}
