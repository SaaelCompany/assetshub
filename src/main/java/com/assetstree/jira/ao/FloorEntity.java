package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Table("AST_FLOOR")
public interface FloorEntity extends Entity {
    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    int getPlaceId();

    void setPlaceId(int placeId);

    @StringLength(80)
    String getName();

    void setName(String name);

    int getLevel();

    void setLevel(int level);

    int getSortOrder();

    void setSortOrder(int sortOrder);

    int getPlanW();

    void setPlanW(int planW);

    int getPlanH();

    void setPlanH(int planH);
}
