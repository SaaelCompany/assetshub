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

    /** room, wall, door, window, column, stair, desk, or partition. Empty on rows drawn before kinds existed. */
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
}
