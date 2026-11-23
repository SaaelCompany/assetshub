package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/** One named report board. Panels are {@code report|chart|width} pairs separated by commas. */
@Table("AST_BOARD")
public interface ReportBoardEntity extends Entity {
    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @StringLength(80)
    String getName();

    void setName(String name);

    int getPosition();

    void setPosition(int position);

    @StringLength(StringLength.UNLIMITED)
    String getPanels();

    void setPanels(String panels);
}
