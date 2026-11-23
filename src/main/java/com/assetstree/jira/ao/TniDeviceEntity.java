package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/**
 * A device noticed on the customer portal. The asset appears when that person creates a request.
 */
@Table("AST_TNI")
public interface TniDeviceEntity extends Entity {
    @NotNull
    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @NotNull
    @Indexed
    @StringLength(64)
    String getIdentity();

    void setIdentity(String identity);

    @Indexed
    @StringLength(255)
    String getUserKey();

    void setUserKey(String userKey);

    @StringLength(64)
    String getAddress();

    void setAddress(String address);

    @StringLength(16)
    String getKind();

    void setKind(String kind);

    @StringLength(StringLength.UNLIMITED)
    String getSummary();

    void setSummary(String summary);

    @StringLength(255)
    String getLabel();

    void setLabel(String label);

    long getSightAt();

    void setSightAt(long sightAt);

    long getSeenAt();

    void setSeenAt(long seenAt);

    int getAssetId();

    void setAssetId(int assetId);

    long getProbeFrom();

    void setProbeFrom(long probeFrom);

    int getProbeFails();

    void setProbeFails(int probeFails);

    long getLastProbe();

    void setLastProbe(long lastProbe);

    int getRetired();

    void setRetired(int retired);

    @StringLength(64)
    String getIssueKey();

    void setIssueKey(String issueKey);
}
