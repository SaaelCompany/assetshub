package com.assetstree.jira.model;

import java.util.Locale;
import java.util.regex.Pattern;

/** Centimeter limits for a drawn floor: rooms, walls and equipment pins share one canvas. */
public final class FloorBounds {
    public static final int CANVAS = 1200;
    public static final int CANVAS_HEIGHT = 760;
    /** 6.4 m. A new floor starts larger than this. */
    public static final int MIN_WIDTH = 640;
    public static final int MIN_HEIGHT = 480;
    /** 400 m by 300 m. One unit is one centimeter. */
    public static final int MAX_WIDTH = 40000;
    public static final int MAX_HEIGHT = 30000;
    public static final int MIN_ROOM = 16;
    public static final int MIN_PART = 4;
    public static final int MAX_FLOORS = 40;
    public static final int MAX_ROOMS = 400;
    public static final int MAX_PINS = 200;
    public static final String ROOM = "room";
    public static final String WALL = "wall";
    public static final String DOOR = "door";
    public static final String WINDOW = "window";
    public static final String COLUMN = "column";
    public static final String STAIR = "stair";
    public static final String DESK = "desk";
    public static final String PARTITION = "partition";
    public static final String TABLE = "table";
    public static final String CHAIR = "chair";
    public static final String CABINET = "cabinet";
    public static final String SOFA = "sofa";
    public static final String SEAT = "seat";
    public static final String PEDESTAL = "pedestal";
    public static final String BED = "bed";
    public static final String PLANT = "plant";
    public static final String BATH = "bath";
    public static final String TOILET = "toilet";
    public static final String SINK = "sink";
    public static final String STOVE = "stove";
    public static final String RUG = "rug";
    public static final String ARC = "arc";
    public static final String DIM = "dim";
    public static final String TEXT = "text";
    public static final String BOX = "box";
    public static final String ELLIPSE = "ellipse";
    public static final String DIAMOND = "diamond";
    public static final String TRIANGLE = "triangle";
    public static final String NOTE = "note";
    public static final String FLOW = "flow";
    public static final String CHART = "chart";
    public static final String POWER = "power";
    public static final String SCS = "scs";
    public static final String COMM = "comm";
    public static final String VENT = "vent";
    public static final String WATER = "water";
    public static final String HEAT = "heat";
    public static final String FIRE = "fire";
    public static final String GUARD = "guard";
    public static final String PNG = "png";
    public static final String PEN = "pen";
    public static final String RACK = "rack";
    public static final String SWITCH = "switch";
    private static final Pattern MARK = Pattern.compile("^(power|scs|comm|vent|water|heat|fire|guard)-[a-z]{1,12}$");

    private FloorBounds() {
    }

    public static int clamp(int value, int max) {
        if (value < 0) {
            return 0;
        }
        return value > max ? max : value;
    }

    /** Origin of a rotated shape. Its unrotated box may hang off the sheet. */
    public static int origin(int value) {
        if (value < -MAX_WIDTH) {
            return -MAX_WIDTH;
        }
        return value > span() ? span() : value;
    }

    /** Longest stored side of a rotated shape. */
    public static int span() {
        return MAX_WIDTH * 2;
    }

    public static String kind(String raw) {
        if (raw == null) {
            return ROOM;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (WALL.equals(value) || DOOR.equals(value) || WINDOW.equals(value)
                || COLUMN.equals(value) || STAIR.equals(value) || DESK.equals(value) || PARTITION.equals(value)
                || TABLE.equals(value) || CHAIR.equals(value) || CABINET.equals(value)
                || SOFA.equals(value) || SEAT.equals(value) || PEDESTAL.equals(value)
                || BED.equals(value) || PLANT.equals(value) || BATH.equals(value)
                || TOILET.equals(value) || SINK.equals(value) || STOVE.equals(value) || RUG.equals(value)
                || ARC.equals(value) || DIM.equals(value) || TEXT.equals(value)
                || BOX.equals(value) || ELLIPSE.equals(value) || DIAMOND.equals(value)
                || TRIANGLE.equals(value) || NOTE.equals(value) || FLOW.equals(value) || CHART.equals(value)
                || POWER.equals(value) || SCS.equals(value) || COMM.equals(value)
                || VENT.equals(value) || WATER.equals(value)
                || HEAT.equals(value) || FIRE.equals(value) || GUARD.equals(value) || PNG.equals(value)
                || PEN.equals(value) || RACK.equals(value) || SWITCH.equals(value)) {
            return value;
        }
        if (value.length() <= 16 && MARK.matcher(value).matches()) {
            return value;
        }
        return ROOM;
    }

    public static int minSize(String kind) {
        String value = kind(kind);
        if (ROOM.equals(value) || STAIR.equals(value)) {
            return MIN_ROOM;
        }
        if (COLUMN.equals(value) || DESK.equals(value) || TABLE.equals(value) || CHAIR.equals(value)
                || CABINET.equals(value) || SOFA.equals(value) || SEAT.equals(value) || PEDESTAL.equals(value)
                || BED.equals(value) || PLANT.equals(value) || BATH.equals(value) || TOILET.equals(value)
                || SINK.equals(value) || STOVE.equals(value) || RUG.equals(value) || TEXT.equals(value)
                || BOX.equals(value) || ELLIPSE.equals(value) || DIAMOND.equals(value)
                || TRIANGLE.equals(value) || NOTE.equals(value) || CHART.equals(value)
                || PNG.equals(value) || RACK.equals(value) || SWITCH.equals(value)) {
            return 8;
        }
        return MIN_PART;
    }

    public static int width(int raw) {
        if (raw <= 0) {
            return CANVAS;
        }
        if (raw < MIN_WIDTH) {
            return MIN_WIDTH;
        }
        return raw > MAX_WIDTH ? MAX_WIDTH : raw;
    }

    public static int height(int raw) {
        if (raw <= 0) {
            return CANVAS_HEIGHT;
        }
        if (raw < MIN_HEIGHT) {
            return MIN_HEIGHT;
        }
        return raw > MAX_HEIGHT ? MAX_HEIGHT : raw;
    }

    /** Degrees clockwise, matching the plan canvas. */
    public static int angle(int raw) {
        int value = raw % 360;
        if (value < 0) {
            value += 360;
        }
        return value;
    }

    /** Closed-room corners as "x,y;x,y". Empty for a plain rectangle. */
    public static String points(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String cleaned = raw.replaceAll("[^0-9,;\\-]", "");
        if (cleaned.length() > 4000) {
            cleaned = cleaned.substring(0, 4000);
        }
        return cleaned;
    }

    public static String color(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return value.matches("#[0-9a-f]{6}") ? value : "";
    }
}
