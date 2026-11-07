package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/** A saved Active Directory connection. The password is write-only. */
@Table("AST_TNI_SRC")
public interface TniSourceEntity extends Entity {
    @NotNull
    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @NotNull
    @StringLength(8)
    String getKind();

    void setKind(String kind);

    @StringLength(253)
    String getHost();

    void setHost(String host);

    int getPort();

    void setPort(int port);

    boolean getSsl();

    void setSsl(boolean ssl);

    @StringLength(400)
    String getBase();

    void setBase(String base);

    @StringLength(200)
    String getAccount();

    void setAccount(String account);

    @StringLength(200)
    String getSecret();

    void setSecret(String secret);

    @StringLength(255)
    String getNote();

    void setNote(String note);

    long getFinished();

    void setFinished(long finished);
}
