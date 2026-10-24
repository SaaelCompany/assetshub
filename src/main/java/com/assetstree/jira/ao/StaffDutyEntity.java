package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/** One person's working week or one leave range inside a project. */
@Table("AST_DUTY")
public interface StaffDutyEntity extends Entity {
    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @Indexed
    @StringLength(255)
    String getUserKey();

    void setUserKey(String userKey);

    /** hours or leave. */
    @StringLength(8)
    String getKind();

    void setKind(String kind);

    /** Bit 0 is Monday, bit 6 is Sunday. Used by an hours row. */
    int getWeekdays();

    void setWeekdays(int weekdays);

    /** Hours on each working day. Used by an hours row. */
    int getHours();

    void setHours(int hours);

    /** Inclusive leave start, yyyy-MM-dd. Empty on an hours row. */
    @StringLength(16)
    String getStartOn();

    void setStartOn(String startOn);

    /** Inclusive leave end, yyyy-MM-dd. Empty on an hours row. */
    @StringLength(16)
    String getEndOn();

    void setEndOn(String endOn);
}
