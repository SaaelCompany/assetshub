package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

import java.util.Date;

@Table("AST_FILE")
public interface AssetFileEntity extends Entity {
    @Indexed
    int getAssetId();

    void setAssetId(int assetId);

    String getFileName();

    void setFileName(String fileName);

    String getContentType();

    void setContentType(String contentType);

    long getSizeBytes();

    void setSizeBytes(long sizeBytes);

    String getAuthorKey();

    void setAuthorKey(String authorKey);

    Date getCreated();

    void setCreated(Date created);

    /** Hidden from the customer portal. */
    boolean isInternal();

    void setInternal(boolean internal);

    /** Empty for a general attachment. buy or contract keeps the file on the lifecycle card. */
    @StringLength(16)
    String getSlot();

    void setSlot(String slot);
}
