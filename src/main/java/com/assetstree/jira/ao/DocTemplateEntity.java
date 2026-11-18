package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

import java.util.Date;

/** A Word template uploaded for one project. The file itself lives under the Jira home. */
@Table("AST_DOCTPL")
public interface DocTemplateEntity extends Entity {
    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @StringLength(120)
    String getName();

    void setName(String name);

    @StringLength(120)
    String getFileName();

    void setFileName(String fileName);

    Date getCreated();

    void setCreated(Date created);
}
