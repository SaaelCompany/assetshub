package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

import java.util.Date;

@Table("AST_CHECK")
public interface InventoryMarkEntity extends Entity {
    @NotNull
    @Indexed
    int getAssetId();

    void setAssetId(int assetId);

    @NotNull
    Date getCheckedAt();

    void setCheckedAt(Date checkedAt);

    @StringLength(255)
    String getCheckedBy();

    void setCheckedBy(String checkedBy);
}
