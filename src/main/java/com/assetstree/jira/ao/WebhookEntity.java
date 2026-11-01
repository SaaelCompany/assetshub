package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/** An outbound call made when an asset changes. Jira Automation can receive it. */
@Table("AST_HOOK")
public interface WebhookEntity extends Entity {
    @NotNull
    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @NotNull
    @StringLength(500)
    String getUrl();

    void setUrl(String url);

    @StringLength(80)
    String getSecret();

    void setSecret(String secret);

    @StringLength(255)
    String getEvents();

    void setEvents(String events);

    boolean isEnabled();

    void setEnabled(boolean enabled);
}
