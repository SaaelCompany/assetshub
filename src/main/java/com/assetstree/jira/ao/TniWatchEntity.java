package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/** An optional reachability check for one asset. Absent or disarmed objects are not pinged. */
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
}
