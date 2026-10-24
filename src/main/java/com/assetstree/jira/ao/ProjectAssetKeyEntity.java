package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/**
 * The asset-key prefix of one Jira project. Object keys already issued stay on the asset.
 */
@Table("AST_PKEY")
public interface ProjectAssetKeyEntity extends Entity {
    @NotNull
    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @NotNull
    @StringLength(16)
    String getAssetPrefix();

    void setAssetPrefix(String assetPrefix);
}
