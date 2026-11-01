package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/** The first result of an import key, so a retry does not write the rows again. */
@Table("AST_IDEMP")
public interface IdempotencyEntity extends Entity {
    @NotNull
    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @NotNull
    @Indexed
    @StringLength(120)
    String getToken();

    void setToken(String token);

    @StringLength(255)
    String getSummary();

    void setSummary(String summary);

    long getCreatedAt();

    void setCreatedAt(long createdAt);
}
