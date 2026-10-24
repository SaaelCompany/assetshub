package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.Table;

@Table("AST_LINK")
public interface AssetIssueLinkEntity extends Entity {
    @Indexed
    int getAssetId();

    void setAssetId(int assetId);

    @Indexed
    long getIssueId();

    void setIssueId(long issueId);

    /** Equipment status when the asset joined the request; empty for links made before 1.3.11. */
    String getStatusBefore();

    void setStatusBefore(String statusBefore);
}
