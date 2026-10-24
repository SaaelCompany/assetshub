package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class RoomDto {
    private int id;
    private String name;
    private int x;
    private int y;
    private int w;
    private int h;
    private String kind;
    private String color;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public int getW() { return w; }
    public void setW(int w) { this.w = w; }
    public int getH() { return h; }
    public void setH(int h) { this.h = h; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
