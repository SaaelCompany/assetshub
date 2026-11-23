package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/** A named set of equipment types. A grant can point at this set. */
@Table("AST_SCHEMA")
public interface TypeSchemaEntity extends Entity {
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
    String getTypeKeys();

    void setTypeKeys(String typeKeys);
}
