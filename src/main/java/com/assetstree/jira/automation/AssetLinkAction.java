package com.assetstree.jira.automation;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.codebarrel.automation.api.thirdparty.AutomationRuleComponent;
import com.codebarrel.automation.api.thirdparty.context.ComponentInputs;
import com.codebarrel.automation.api.thirdparty.context.RuleContext;
import com.codebarrel.automation.api.thirdparty.context.result.ExecutionResult;
import com.codebarrel.automation.api.thirdparty.context.result.ExecutionResultBuilder;
import com.assetstree.jira.model.AssetException;
import com.assetstree.jira.service.AssetBridge;
import com.assetstree.jira.service.AssetService;

import java.util.List;

/** Action: attach the requests in the rule to the equipment that started it. */
public class AssetLinkAction implements AutomationRuleComponent {
    @Override
    public ExecutionResult execute(RuleContext ruleContext, ComponentInputs inputs) {
        int assetId = AssetRuleValues.assetId(inputs);
        if (assetId <= 0) {
            ruleContext.getAuditLog().addMessage("asset-tree.automation.linkMiss");
            return ExecutionResultBuilder.stopRule();
        }
        List<Issue> issues = inputs == null ? null : inputs.getIssues();
        if (issues == null || issues.isEmpty()) {
            ruleContext.getAuditLog().addMessage("asset-tree.automation.linkNoIssue");
            return ExecutionResultBuilder.stopRule();
        }
        AssetService service = AssetBridge.service();
        if (service == null) {
            ruleContext.getAuditLog().addError("asset-tree.automation.linkMiss");
            return ExecutionResultBuilder.stopRule();
        }
        JiraAuthenticationContext authentication = ComponentAccessor.getJiraAuthenticationContext();
        ApplicationUser actor = authentication == null ? null : authentication.getLoggedInUser();
        int linked = 0;
        try {
            for (int i = 0; i < issues.size(); i++) {
                Issue issue = issues.get(i);
                if (issue == null || issue.getKey() == null) {
                    continue;
                }
                try {
                    service.linkIssue(actor, assetId, issue.getKey());
                    linked++;
                } catch (AssetException ex) {
                    if (ex.getStatus() == 409) {
                        linked++;
                    } else {
                        throw ex;
                    }
                }
            }
        } catch (AssetException ex) {
            if (ex.getStatus() == 403 || ex.getStatus() == 404) {
                ruleContext.getAuditLog().addError("asset-tree.automation.linkDenied");
            } else {
                ruleContext.getAuditLog().addError("asset-tree.automation.linkMiss");
            }
            return ExecutionResultBuilder.stopRule();
        }
        if (linked == 0) {
            ruleContext.getAuditLog().addMessage("asset-tree.automation.linkNoIssue");
            return ExecutionResultBuilder.stopRule();
        }
        ruleContext.getAuditLog().addMessage("asset-tree.automation.linkSet", String.valueOf(linked));
        return ExecutionResultBuilder.continueRule();
    }
}
