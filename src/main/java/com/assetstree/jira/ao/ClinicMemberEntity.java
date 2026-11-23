package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/** One person in a clinic access group. */
@Table("AST_CLINIC_USER")
public interface ClinicMemberEntity extends Entity {
    @Indexed
    int getCircleId();

    void setCircleId(int circleId);

    @Indexed
    @StringLength(255)
    String getUserKey();

    void setUserKey(String userKey);
}
