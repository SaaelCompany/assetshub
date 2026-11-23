package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/**
 * A named bundle of access, such as "Мед инженер". People and Jira groups
 * point at the name. Changing the bundle changes everyone who has it.
 */
@Table("AST_ROLE")
public interface AccessRoleEntity extends Entity {
    @NotNull
    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @NotNull
    @StringLength(80)
    String getName();

    void setName(String name);

    @StringLength(StringLength.UNLIMITED)
    String getRights();

    void setRights(String rights);

    /** "all" is the whole project. "own" is the person's places and the cards where they are custodian. */
    @StringLength(16)
    String getReach();

    void setReach(String reach);
}
