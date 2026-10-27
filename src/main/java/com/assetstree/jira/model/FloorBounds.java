package com.assetstree.jira.model;

import java.util.Locale;

/** Pixel limits for a drawn floor: rooms, walls and equipment pins share one canvas. */
public final class FloorBounds {
    public static final int CANVAS = 1200;
    public static final int CANVAS_HEIGHT = 760;
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
        if (value < -2400) {
            return -2400;
        }
        return value > 4800 ? 4800 : value;
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
                || ARC.equals(value) || DIM.equals(value) || TEXT.equals(value)) {
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
                || SINK.equals(value) || STOVE.equals(value) || RUG.equals(value) || TEXT.equals(value)) {
            return 8;
        }
        return MIN_PART;
    }

    public static int width(int raw) {
        if (raw <= 0) {
            return CANVAS;
        }
        if (raw < 640) {
            return 640;
        }
        return raw > 2400 ? 2400 : raw;
    }

    public static int height(int raw) {
        if (raw <= 0) {
            return CANVAS_HEIGHT;
        }
        if (raw < 480) {
            return 480;
        }
        return raw > 1600 ? 1600 : raw;
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
