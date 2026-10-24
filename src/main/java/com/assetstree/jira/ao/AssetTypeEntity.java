package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;
import net.java.ao.schema.Unique;

@Table("AST_TYPE")
public interface AssetTypeEntity extends Entity {
    @NotNull
    @Unique
    @StringLength(64)
    String getTypeKey();

    void setTypeKey(String typeKey);

    @NotNull
    @StringLength(255)
    String getLabel();

    void setLabel(String label);

    @NotNull
    @StringLength(16)
    String getColor();

    void setColor(String color);

    @StringLength(40)
    String getIcon();

    void setIcon(String icon);

    boolean isSystemType();

    void setSystemType(boolean systemType);

    int getSortOrder();

    void setSortOrder(int sortOrder);

    @NotNull
    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @StringLength(40)
    String getBaseKey();

    void setBaseKey(String baseKey);

    boolean isLocation();

    void setLocation(boolean location);

    boolean isShowInTree();

    void setShowInTree(boolean showInTree);

    /** Schedules such as service and verification. Off until a type opts in. */
    boolean isService();

    void setService(boolean service);

    /** Label shown on a child card instead of the generic parent caption. Empty uses the type name. */
    @StringLength(80)
    String getPlaceCaption();

    void setPlaceCaption(String placeCaption);

    /** Empty for a root type. A subtype names one equipment type and cannot itself be a parent. */
    @Indexed
    @StringLength(64)
    String getParentTypeKey();

    void setParentTypeKey(String parentTypeKey);
}
