package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Table("AST_FLOW")
public interface FlowRuleEntity extends Entity {
    @NotNull
    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @NotNull
    @Indexed
    @StringLength(60)
    String getIssueTypeId();

    void setIssueTypeId(String issueTypeId);

    @NotNull
    @StringLength(255)
    String getIssueTypeName();

    void setIssueTypeName(String issueTypeName);

    @NotNull
    @StringLength(40)
    String getStatusKey();

    void setStatusKey(String statusKey);
}
