package com.assetstree.jira.automation;

import com.atlassian.jira.issue.Issue;
import com.codebarrel.automation.api.thirdparty.AutomationRuleComponent;
import com.codebarrel.automation.api.thirdparty.context.ComponentInputs;
import com.codebarrel.automation.api.thirdparty.context.RuleContext;
import com.codebarrel.automation.api.thirdparty.context.result.ExecutionResult;
import com.codebarrel.automation.api.thirdparty.context.result.ExecutionResultBuilder;
import com.assetstree.jira.service.AssetBridge;
import com.assetstree.jira.service.AssetService;

import java.util.ArrayList;
import java.util.List;

/** Condition: the request has linked equipment, optionally in one status. */
public class AssetLinkedCondition implements AutomationRuleComponent {
    @Override
    public ExecutionResult execute(RuleContext ruleContext, ComponentInputs inputs) {
        List<Issue> issues = inputs == null ? null : inputs.getIssues();
        if (issues == null || issues.isEmpty()) {
            ruleContext.getAuditLog().addMessage("asset-tree.automation.linkedStop");
            return ExecutionResultBuilder.stopRule();
        }
        String status = AssetRuleValues.field(ruleContext.getComponentConfigBean().getValue(), "status");
        AssetService service = AssetBridge.service();
        List<Issue> matched = new ArrayList<Issue>();
        if (service != null) {
            for (int i = 0; i < issues.size(); i++) {
                Issue issue = issues.get(i);
                if (issue != null && issue.getId() != null && service.issueHasEquipment(issue.getId().longValue(), status)) {
                    matched.add(issue);
                }
            }
        }
        if (matched.isEmpty()) {
            ruleContext.getAuditLog().addMessage("asset-tree.automation.linkedStop");
            return ExecutionResultBuilder.stopRule();
        }
        return ExecutionResultBuilder.withIssues(matched).executeBulk();
    }
}
