package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Table("AST_SCHEME")
public interface SchemeEntity extends Entity {
    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @StringLength(80)
    String getName();

    void setName(String name);

    @StringLength(StringLength.UNLIMITED)
    String getBody();

    void setBody(String body);

    long getUpdatedAt();

    void setUpdatedAt(long updatedAt);
}
