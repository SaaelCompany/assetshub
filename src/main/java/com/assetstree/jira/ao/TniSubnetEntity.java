package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/** One scanner task: a named local network, range, or address list. */
@Table("AST_TNI_NET")
public interface TniSubnetEntity extends Entity {
    @NotNull
    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @NotNull
    @StringLength(80)
    String getName();

    void setName(String name);

    @NotNull
    @StringLength(StringLength.UNLIMITED)
    String getTarget();

    void setTarget(String target);

    int getHosts();

    void setHosts(int hosts);

    @StringLength(16)
    String getState();

    void setState(String state);

    int getScanned();

    void setScanned(int scanned);

    int getOnline();

    void setOnline(int online);

    int getAdded();

    void setAdded(int added);

    @StringLength(255)
    String getNote();

    void setNote(String note);

    @StringLength(StringLength.UNLIMITED)
    String getLog();

    void setLog(String log);

    long getStarted();

    void setStarted(long started);

    long getFinished();

    void setFinished(long finished);
}
