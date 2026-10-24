package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/**
 * How one Jira project shows its assets: objects under types in the tree and the default table columns.
 */
@Table("AST_PVIEW")
public interface ProjectViewEntity extends Entity {
    @NotNull
    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    /**
     * 0 — the tree ends at types, 1 — objects are listed under their type.
     */
    int getTreeMode();

    void setTreeMode(int treeMode);

    /**
     * Comma-separated column keys; empty keeps the built-in set.
     */
    @StringLength(StringLength.MAX_LENGTH)
    String getColumnList();

    void setColumnList(String columnList);

    /**
     * Comma-separated module keys. Empty keeps every optional module off.
     */
    @StringLength(255)
    String getModuleList();

    void setModuleList(String moduleList);

    /**
     * Options for the optional modules. Empty keeps the built-in defaults.
     */
    @StringLength(StringLength.UNLIMITED)
    String getOptionList();

    void setOptionList(String optionList);
}
