package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Table("AST_ROOM")
public interface RoomEntity extends Entity {
    @Indexed
    int getFloorId();

    void setFloorId(int floorId);

    @StringLength(80)
    String getName();

    void setName(String name);

    int getPosX();

    void setPosX(int posX);

    int getPosY();

    void setPosY(int posY);

    int getPosW();

    void setPosW(int posW);

    int getPosH();

    void setPosH(int posH);

    /** Clockwise degrees. Zero keeps the shape axis-aligned. */
    int getAngle();

    void setAngle(int angle);

    /** room, wall, door, window, column, stair, desk, partition, or a route: power, scs, comm, vent, water. Empty on rows drawn before kinds existed. */
    @StringLength(16)
    String getKind();

    void setKind(String kind);

    /** #rrggbb fill for a room. Empty keeps the default tint. */
    @StringLength(16)
    String getColor();

    void setColor(String color);

    /** Closed wall corners, "x,y;x,y". Empty when the room is a rectangle. */
    @StringLength(StringLength.UNLIMITED)
    String getPoints();

    void setPoints(String points);

    /** Place in the building tree. Zero when the room is not linked. */
    int getPlaceId();

    void setPlaceId(int placeId);

    /** Door swing. 1 opens the opposite way from the default leaf. */
    int getSwing();

    void setSwing(int swing);

    /** Furniture placed together. Zero when the shape stands alone. */
    int getGroupId();

    void setGroupId(int groupId);

    /** Asset drawn by this furniture shape. Zero when the shape is only a symbol. */
    int getAssetId();

    void setAssetId(int assetId);
}
