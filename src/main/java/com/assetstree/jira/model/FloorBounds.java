package com.assetstree.jira.model;

import java.util.Locale;

/** Pixel limits for a drawn floor: rooms, walls and equipment pins share one canvas. */
public final class FloorBounds {
    public static final int CANVAS = 1200;
    public static final int CANVAS_HEIGHT = 760;
    public static final int MIN_ROOM = 36;
    public static final int MIN_PART = 6;
    public static final int MAX_FLOORS = 40;
    public static final int MAX_ROOMS = 80;
    public static final int MAX_PINS = 200;
    public static final String ROOM = "room";
    public static final String WALL = "wall";
    public static final String DOOR = "door";
    public static final String WINDOW = "window";

    private FloorBounds() {
    }

    public static int clamp(int value, int max) {
        if (value < 0) {
            return 0;
        }
        return value > max ? max : value;
    }

    public static String kind(String raw) {
        if (raw == null) {
            return ROOM;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (WALL.equals(value) || DOOR.equals(value) || WINDOW.equals(value)) {
            return value;
        }
        return ROOM;
    }

    public static int minSize(String kind) {
        return ROOM.equals(kind(kind)) ? MIN_ROOM : MIN_PART;
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

    public static String color(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return value.matches("#[0-9a-f]{6}") ? value : "";
    }
}
