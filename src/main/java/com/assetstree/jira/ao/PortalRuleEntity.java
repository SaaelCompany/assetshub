package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Table("AST_PRULE")
public interface PortalRuleEntity extends Entity {
    @NotNull
    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @Indexed
    int getAssetId();

    void setAssetId(int assetId);

    /** Set when the rule opens a type on the place, empty when it opens the place or one object. */
    @StringLength(64)
    String getTypeKey();

    void setTypeKey(String typeKey);

    int getPosition();

    void setPosition(int position);
}
