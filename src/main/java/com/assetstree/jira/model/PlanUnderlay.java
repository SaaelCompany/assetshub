package com.assetstree.jira.model;

/**
 * A scan sitting under a floor drawing. Stored as {@code x,y,w,h|data:image/...}.
 * Only a JPEG or PNG data URL is kept, and only inside the scheme body budget.
 */
public final class PlanUnderlay {
    public static final int MAX = 900000;

    private PlanUnderlay() {
    }

    public static final class Sheet {
        public final String src;
        public final int x;
        public final int y;
        public final int w;
        public final int h;

        public Sheet(String src, int x, int y, int w, int h) {
            this.src = src == null ? "" : src;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    public static String store(String src, int x, int y, int w, int h) {
        if (src == null || src.trim().isEmpty()) {
            return "";
        }
        String image = src.trim();
        if (!image.startsWith("data:image/jpeg;base64,") && !image.startsWith("data:image/png;base64,")) {
            return "";
        }
        if (image.length() > MAX) {
            return "";
        }
        if (image.indexOf('|') >= 0 || image.indexOf(';') != image.indexOf(";base64,")) {
            return "";
        }
        if (w < 10 || h < 10 || w > 20000 || h > 20000) {
            return "";
        }
        if (x < -20000 || y < -20000 || x > 20000 || y > 20000) {
            return "";
        }
        return x + "," + y + "," + w + "," + h + "|" + image;
    }

    public static Sheet read(String stored) {
        if (stored == null || stored.isEmpty()) {
            return new Sheet("", 0, 0, 0, 0);
        }
        int bar = stored.indexOf('|');
        if (bar <= 0) {
            return new Sheet("", 0, 0, 0, 0);
        }
        String[] box = stored.substring(0, bar).split(",");
        if (box.length != 4) {
            return new Sheet("", 0, 0, 0, 0);
        }
        try {
            int x = Integer.parseInt(box[0]);
            int y = Integer.parseInt(box[1]);
            int w = Integer.parseInt(box[2]);
            int h = Integer.parseInt(box[3]);
            String packed = store(stored.substring(bar + 1), x, y, w, h);
            if (packed.isEmpty()) {
                return new Sheet("", 0, 0, 0, 0);
            }
            return new Sheet(stored.substring(bar + 1), x, y, w, h);
        } catch (NumberFormatException ex) {
            return new Sheet("", 0, 0, 0, 0);
        }
    }
}
