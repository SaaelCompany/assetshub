package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/** Clock and exception for one asset whose type follows the TNI storage rule. */
@Table("AST_TNI_W")
public interface TniWatchEntity extends Entity {
    @Indexed
    int getAssetId();

    void setAssetId(int assetId);

    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @StringLength(253)
    String getAddress();

    void setAddress(String address);

    int getArmed();

    void setArmed(int armed);

    long getCheckedAt();

    void setCheckedAt(long checkedAt);

    int getUp();

    void setUp(int up);

    /** One means this object is left alone even when the type rule is on. */
    int getExempt();

    void setExempt(int exempt);

    long getSeenAt();

    void setSeenAt(long seenAt);

    long getProbeFrom();

    void setProbeFrom(long probeFrom);

    int getProbeFails();

    void setProbeFails(int probeFails);

    long getLastProbe();

    void setLastProbe(long lastProbe);
}
