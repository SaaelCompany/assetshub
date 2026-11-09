package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/** One row: the portal card layout used by every project until a person picks their own. */
@Table("AST_PLAYOUT")
public interface PortalLayoutEntity extends Entity {
    @StringLength(16)
    String getLayout();

    void setLayout(String layout);
}
