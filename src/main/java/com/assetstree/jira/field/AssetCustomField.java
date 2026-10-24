package com.assetstree.jira.field;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.impl.GenericTextCFType;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.assetstree.jira.service.AssetBridge;

import java.util.Map;

public class AssetCustomField extends GenericTextCFType {
    public AssetCustomField() {
        this(ComponentAccessor.getComponent(CustomFieldValuePersister.class),
                ComponentAccessor.getComponent(GenericConfigManager.class));
    }

    private AssetCustomField(CustomFieldValuePersister customFieldValuePersister,
                             GenericConfigManager genericConfigManager) {
        super(customFieldValuePersister, genericConfigManager);
    }

    @Override
    public void createValue(CustomField field, Issue issue, String value) {
        super.createValue(field, issue, value);
        sync(issue, null, value);
    }

    @Override
    public void updateValue(CustomField field, Issue issue, String value) {
        String previous = getValueFromIssue(field, issue);
        super.updateValue(field, issue, value);
        sync(issue, previous, value);
    }

    @Override
    public Map<String, Object> getVelocityParameters(Issue issue, CustomField field, FieldLayoutItem fieldLayoutItem) {
        Map<String, Object> parameters = super.getVelocityParameters(issue, field, fieldLayoutItem);
        String projectKey = "";
        if (issue != null && issue.getProjectObject() != null) {
            projectKey = issue.getProjectObject().getKey();
        }
        parameters.put("projectKey", projectKey);
        Object raw = parameters.get("value");
        parameters.put("assetLabel", AssetBridge.label(raw == null ? "" : String.valueOf(raw)));
        return parameters;
    }

    private void sync(Issue issue, String previous, String value) {
        if (AssetBridge.service() == null) {
            return;
        }
        AssetBridge.service().syncRequestAsset(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(), issue, previous, value);
    }
}
