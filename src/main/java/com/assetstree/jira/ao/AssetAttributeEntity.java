package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Table("AST_ATTR")
public interface AssetAttributeEntity extends Entity {
    @Indexed
    int getAssetId();

    void setAssetId(int assetId);

    @NotNull
    @StringLength(80)
    String getAttrName();

    void setAttrName(String attrName);

    @StringLength(StringLength.UNLIMITED)
    String getAttrValue();

    void setAttrValue(String attrValue);

    int getAttrPosition();

    void setAttrPosition(int attrPosition);
}
