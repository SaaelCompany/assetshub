package com.assetstree.jira.automation;

import com.atlassian.jira.component.ComponentAccessor;
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
import com.assetstree.jira.model.AssetException;
import com.assetstree.jira.model.AssetMailDraft;
import com.assetstree.jira.service.AssetBridge;
import com.assetstree.jira.service.AssetService;

/** Action: send a plain letter about the equipment that started the rule. */
public class AssetMailAction implements AutomationRuleComponent {
    @Override
    public ErrorCollection validateConfig(RuleContext context, ComponentConfigBean config) {
        ErrorCollection errors = new ErrorCollectionImpl();
        Object value = config == null ? null : config.getValue();
        if ("user".equals(AssetRuleValues.field(value, "to")) && AssetRuleValues.field(value, "userKey").isEmpty()) {
            errors.addError("userKey", "asset-tree.automation.mailRequired");
        }
        return errors;
    }

    @Override
    public ExecutionResult execute(RuleContext ruleContext, ComponentInputs inputs) {
        int assetId = AssetRuleValues.assetId(inputs);
        if (assetId <= 0) {
            ruleContext.getAuditLog().addMessage("asset-tree.automation.mailNoAsset");
            return ExecutionResultBuilder.continueRule();
        }
        AssetService service = AssetBridge.service();
        if (service == null) {
            ruleContext.getAuditLog().addError("asset-tree.automation.mailMiss");
            return ExecutionResultBuilder.stopRule();
        }
        Object value = ruleContext.getComponentConfigBean().getValue();
        String body = inputs == null ? "" : inputs.getSerializedEvent();
        AssetMailDraft draft = new AssetMailDraft();
        draft.setRole(AssetRuleValues.field(value, "to"));
        draft.setUserKey(AssetRuleValues.field(value, "userKey"));
        draft.setParts(AssetRuleValues.field(value, "parts"));
        draft.setExtra(AssetRuleValues.field(value, "extra"));
        draft.setEvent(AssetRuleValues.json(body, "event"));
        draft.setDetail(AssetRuleValues.json(body, "detail"));
        draft.setPlan(AssetRuleValues.json(body, "plan"));
        draft.setDue(AssetRuleValues.json(body, "due"));
        draft.setPrevious(AssetRuleValues.json(body, "previous"));
        draft.setNote(AssetRuleValues.json(body, "note"));
        JiraAuthenticationContext authentication = ComponentAccessor.getJiraAuthenticationContext();
        ApplicationUser actor = authentication == null ? null : authentication.getLoggedInUser();
        int sent;
        try {
            sent = service.mailEquipment(actor, assetId, draft);
        } catch (AssetException ex) {
            ruleContext.getAuditLog().addError("asset-tree.automation.mailMiss");
            return ExecutionResultBuilder.stopRule();
        }
        if (sent == 0) {
            ruleContext.getAuditLog().addMessage("asset-tree.automation.mailMiss");
        } else {
            ruleContext.getAuditLog().addMessage("asset-tree.automation.mailSent", String.valueOf(sent));
        }
        return ExecutionResultBuilder.continueRule();
    }
}
