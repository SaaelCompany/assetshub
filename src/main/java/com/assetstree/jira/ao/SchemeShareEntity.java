package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/** A project that may open another project's drawing and place its own equipment on it. */
@Table("AST_SCHEME_SHARE")
public interface SchemeShareEntity extends Entity {
    @Indexed
    int getSchemeId();

    void setSchemeId(int schemeId);

    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);
}
