package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Table("AST_PCOND")
public interface PortalRuleConditionEntity extends Entity {
    @Indexed
    int getRuleId();

    void setRuleId(int ruleId);

    int getPosition();

    void setPosition(int position);

    @NotNull
    @StringLength(255)
    String getFieldName();

    void setFieldName(String fieldName);

    @NotNull
    @StringLength(255)
    String getOptionName();

    void setOptionName(String optionName);
}
