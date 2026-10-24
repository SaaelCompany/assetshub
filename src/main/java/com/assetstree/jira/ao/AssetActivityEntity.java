package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

import java.util.Date;

@Table("AST_LOG")
public interface AssetActivityEntity extends Entity {
    @Indexed
    int getAssetId();

    void setAssetId(int assetId);

    String getAuthorKey();

    void setAuthorKey(String authorKey);

    @StringLength(32)
    String getKind();

    void setKind(String kind);

    @StringLength(255)
    String getFieldName();

    void setFieldName(String fieldName);

    @StringLength(StringLength.UNLIMITED)
    String getOldValue();

    void setOldValue(String oldValue);

    @StringLength(StringLength.UNLIMITED)
    String getNewValue();

    void setNewValue(String newValue);

    Date getCreated();

    void setCreated(Date created);
}
