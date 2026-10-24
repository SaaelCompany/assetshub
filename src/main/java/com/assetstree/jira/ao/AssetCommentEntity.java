package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

import java.util.Date;

@Table("AST_NOTE")
public interface AssetCommentEntity extends Entity {
    @Indexed
    int getAssetId();

    void setAssetId(int assetId);

    String getAuthorKey();

    void setAuthorKey(String authorKey);

    @StringLength(StringLength.UNLIMITED)
    String getBody();

    void setBody(String body);

    Date getCreated();

    void setCreated(Date created);
}
