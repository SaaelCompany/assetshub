package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;
import net.java.ao.schema.Unique;

import java.util.Date;

@Table("AST_NODE")
public interface AssetEntity extends Entity {
    @NotNull
    @StringLength(255)
    String getName();

    void setName(String name);

    @NotNull
    @Unique
    @StringLength(32)
    String getObjectKey();

    void setObjectKey(String objectKey);

    @StringLength(StringLength.UNLIMITED)
    String getDescription();

    void setDescription(String description);

    @NotNull
    @Indexed
    @StringLength(64)
    String getTypeKey();

    void setTypeKey(String typeKey);

    @NotNull
    @StringLength(32)
    String getStatus();

    void setStatus(String status);

    @Indexed
    Integer getParentId();

    void setParentId(Integer parentId);

    int getSortOrder();

    void setSortOrder(int sortOrder);

    Date getCreated();

    void setCreated(Date created);

    Date getUpdated();

    void setUpdated(Date updated);

    @StringLength(255)
    String getCreatedBy();

    void setCreatedBy(String createdBy);

    @StringLength(255)
    String getUpdatedBy();

    void setUpdatedBy(String updatedBy);

    @NotNull
    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @Indexed
    @StringLength(255)
    String getCustodianKey();

    void setCustodianKey(String custodianKey);

    /** Equipment types pinned to this place. Newline-separated type keys. Empty until someone adds a type here. */
    @StringLength(StringLength.UNLIMITED)
    String getOfferedTypes();

    void setOfferedTypes(String offeredTypes);

    /** png, jpeg, or empty when this equipment has no card picture. */
    @StringLength(16)
    String getCardImage();

    void setCardImage(String cardImage);
}
