package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Table("AST_GRANT")
public interface ProjectGrantEntity extends Entity {
    @NotNull
    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @NotNull
    @StringLength(255)
    String getGroupName();

    void setGroupName(String groupName);

    @Indexed
    @StringLength(255)
    String getUserKey();

    void setUserKey(String userKey);

    @NotNull
    @StringLength(16)
    String getLevel();

    void setLevel(String level);

    @StringLength(80)
    String getCaps();

    void setCaps(String caps);

    /** Comma-separated equipment type keys. Empty means every type. */
    @StringLength(StringLength.UNLIMITED)
    String getTypeScope();

    void setTypeScope(String typeScope);

    /** fieldKey:read or fieldKey:write. Empty means every field. */
    @StringLength(StringLength.UNLIMITED)
    String getFieldScope();

    void setFieldScope(String fieldScope);

    /** Name of a saved type schema. When set, it replaces the type list. */
    @StringLength(80)
    String getSchemaName();

    void setSchemaName(String schemaName);
}
