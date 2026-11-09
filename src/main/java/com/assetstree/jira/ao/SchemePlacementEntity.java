package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/** Equipment markers one project placed on a shared drawing. */
@Table("AST_SCHEME_PLACE")
public interface SchemePlacementEntity extends Entity {
    @Indexed
    int getSchemeId();

    void setSchemeId(int schemeId);

    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @StringLength(StringLength.UNLIMITED)
    String getBody();

    void setBody(String body);

    long getUpdatedAt();

    void setUpdatedAt(long updatedAt);
}
