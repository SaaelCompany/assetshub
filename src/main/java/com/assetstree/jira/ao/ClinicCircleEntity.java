package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

import java.util.Date;

/** A named set of people who share one clinic's equipment. */
@Table("AST_CLINIC")
public interface ClinicCircleEntity extends Entity {
    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @StringLength(80)
    String getName();

    void setName(String name);

    /** Comma-separated report ids this group may see on the portal. */
    @StringLength(400)
    String getReports();

    void setReports(String reports);

    Date getCreated();

    void setCreated(Date created);
}
