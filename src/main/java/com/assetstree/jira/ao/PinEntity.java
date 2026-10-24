package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.Table;

@Table("AST_PIN")
public interface PinEntity extends Entity {
    @Indexed
    int getFloorId();

    void setFloorId(int floorId);

    @Indexed
    int getAssetId();

    void setAssetId(int assetId);

    int getPosX();

    void setPosX(int posX);

    int getPosY();

    void setPosY(int posY);
}
