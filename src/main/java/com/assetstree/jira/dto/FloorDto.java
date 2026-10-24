package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class FloorDto {
    private int id;
    private String name;
    private int level;
    private int placeId;
    private String placeName;
    private int planW;
    private int planH;
    private List<RoomDto> rooms = new ArrayList<RoomDto>();
    private List<PinDto> pins = new ArrayList<PinDto>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getPlaceId() { return placeId; }
    public void setPlaceId(int placeId) { this.placeId = placeId; }
    public String getPlaceName() { return placeName; }
    public void setPlaceName(String placeName) { this.placeName = placeName; }
    public int getPlanW() { return planW; }
    public void setPlanW(int planW) { this.planW = planW; }
    public int getPlanH() { return planH; }
    public void setPlanH(int planH) { this.planH = planH; }
    public List<RoomDto> getRooms() { return rooms; }
    public void setRooms(List<RoomDto> rooms) { this.rooms = rooms; }
    public List<PinDto> getPins() { return pins; }
    public void setPins(List<PinDto> pins) { this.pins = pins; }
}
