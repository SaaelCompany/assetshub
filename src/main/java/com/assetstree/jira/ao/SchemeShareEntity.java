package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/** A project that may open another project's drawing. canEdit lets that project change the drawing. */
@Table("AST_SCHEME_SHARE")
public interface SchemeShareEntity extends Entity {
    @Indexed
    int getSchemeId();

    void setSchemeId(int schemeId);

    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    /** 1 when the shared project may change the drawing. 0 places its own equipment only. */
    int getCanEdit();

    void setCanEdit(int canEdit);
}
