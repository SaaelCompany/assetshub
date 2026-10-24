package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;
import net.java.ao.schema.Unique;

@Table("AST_FGRAPH")
public interface FlowGraphEntity extends Entity {
    @NotNull
    @Indexed
    @Unique
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @StringLength(StringLength.UNLIMITED)
    String getBody();

    void setBody(String body);
}
