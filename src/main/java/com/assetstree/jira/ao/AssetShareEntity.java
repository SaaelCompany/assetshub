package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

import java.util.Date;

/** A person who can see this equipment card, the same idea as sharing a Jira request. */
@Table("AST_SHARE")
public interface AssetShareEntity extends Entity {
    @Indexed
    int getAssetId();

    void setAssetId(int assetId);

    @Indexed
    @StringLength(255)
    String getUserKey();

    void setUserKey(String userKey);

    Date getCreated();

    void setCreated(Date created);
}
