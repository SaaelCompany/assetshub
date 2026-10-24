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

    @NotNull
    @StringLength(16)
    String getLevel();

    void setLevel(String level);

    @StringLength(80)
    String getCaps();

    void setCaps(String caps);
}
