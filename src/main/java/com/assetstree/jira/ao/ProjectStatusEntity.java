package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Table("AST_PSTATUS")
public interface ProjectStatusEntity extends Entity {
    @NotNull
    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @NotNull
    @StringLength(40)
    String getStatusKey();

    void setStatusKey(String statusKey);

    @NotNull
    @StringLength(255)
    String getLabel();

    void setLabel(String label);

    @NotNull
    @StringLength(16)
    String getCategory();

    void setCategory(String category);

    int getSortOrder();

    void setSortOrder(int sortOrder);

    /**
     * 0 — not chosen yet (repair, maintenance and written off stay visible),
     * 1 — show the count in the summary, 2 — hide it.
     * A new Active Objects column reads as 0 on rows that already exist.
     */
    int getSummaryMode();

    void setSummaryMode(int summaryMode);
}
