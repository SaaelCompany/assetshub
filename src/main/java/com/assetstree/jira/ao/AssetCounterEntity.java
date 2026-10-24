package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Table("AST_SEQ")
public interface AssetCounterEntity extends Entity {
    int getNextValue();

    void setNextValue(int nextValue);

    /** Asset prefix this sequence belongs to. Empty on the old shared AST- counter. */
    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);
}
