package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Table("AST_PLAN")
public interface ServicePlanEntity extends Entity {
    @Indexed
    int getAssetId();

    void setAssetId(int assetId);

    @StringLength(80)
    String getName();

    void setName(String name);

    @StringLength(16)
    String getLastDone();

    void setLastDone(String lastDone);

    int getEveryCount();

    void setEveryCount(int everyCount);

    @StringLength(8)
    String getEveryUnit();

    void setEveryUnit(String everyUnit);

    @StringLength(40)
    String getStatusKey();

    void setStatusKey(String statusKey);

    /** 1 sends a letter to the responsible person when the date is reached. */
    int getNotifyFlag();

    void setNotifyFlag(int notifyFlag);

    /** The due day already handled, so the same date is not applied again. */
    @StringLength(16)
    String getAppliedFor();

    void setAppliedFor(String appliedFor);

    /** to, kts, or verify. Empty on rows created before kinds existed. */
    @StringLength(16)
    String getKind();

    void setKind(String kind);

    @StringLength(255)
    String getAssigneeKey();

    void setAssigneeKey(String assigneeKey);

    @StringLength(StringLength.UNLIMITED)
    String getNote();

    void setNote(String note);

    @StringLength(8)
    String getStartTime();

    void setStartTime(String startTime);

    @StringLength(8)
    String getEndTime();

    void setEndTime(String endTime);

    /** The day a person marked the work done. Empty until then. */
    @StringLength(16)
    String getCompletedOn();

    void setCompletedOn(String completedOn);
}
