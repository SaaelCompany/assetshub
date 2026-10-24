package com.assetstree.jira.model;

public final class StatusCategories {
    private static final String[] KEYS = new String[] {
            "todo", "progress", "done",
            "blue", "orange", "red", "purple", "teal", "gray", "pink", "lime", "brown"
    };
    private static final String[] COLORS = new String[] {
            "#4a6785", "#ffd351", "#14892c",
            "#0052cc", "#ff8b00", "#de350b", "#6554c0", "#00a3bf", "#6b778c", "#cd519d", "#36b37e", "#974f0c"
    };

    private StatusCategories() {
    }

    public static boolean allowed(String category) {
        return index(category) >= 0;
    }

    public static String color(String category) {
        int found = index(category);
        return COLORS[found >= 0 ? found : 0];
    }

    private static int index(String category) {
        if (category == null) {
            return -1;
        }
        for (int i = 0; i < KEYS.length; i++) {
            if (KEYS[i].equals(category)) {
                return i;
            }
        }
        return -1;
    }
}
