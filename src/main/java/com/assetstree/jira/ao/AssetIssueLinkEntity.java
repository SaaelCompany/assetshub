package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
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

    /**
     * Empty for a request link. The column stays optional. H2 1.4 rejects the
     * statement Active Objects uses to change nullability, and that failure
     * stops the whole plugin from starting.
     */
    @StringLength(32)
    String getLinkType();

    void setLinkType(String linkType);
}
