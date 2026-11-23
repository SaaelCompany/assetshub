package com.assetstree.jira.model;

import java.util.Locale;

/** The three portal profile layouts. Anything else falls back to tiles. */
public final class PortalLayouts {
    public static final String TILES = "tiles";
    public static final String CARDS = "cards";
    public static final String LIST = "list";

    private PortalLayouts() {
    }

    public static String canonical(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (CARDS.equals(value) || LIST.equals(value) || TILES.equals(value)) {
            return value;
        }
        return TILES;
    }

    public static boolean known(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return TILES.equals(value) || CARDS.equals(value) || LIST.equals(value);
    }
}
