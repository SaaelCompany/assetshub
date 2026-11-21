package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.Table;

/** A place whose equipment a clinic group can see. */
@Table("AST_CLINIC_PLACE")
public interface ClinicPlaceEntity extends Entity {
    @Indexed
    int getCircleId();

    void setCircleId(int circleId);

    @Indexed
    int getAssetId();

    void setAssetId(int assetId);
}
