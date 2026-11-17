package com.assetstree.jira.model;

/** Subject and description of the request created from one calendar day. */
public final class CalendarRequest {
    public static final int SUMMARY_LIMIT = 255;
    public static final int DESCRIPTION_LIMIT = 10000;

    private final String summary;
    private final String description;

    private CalendarRequest(String summary, String description) {
        this.summary = summary;
        this.description = description;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Builds the request text. Empty fields are left out. The date is added to the subject
     * so two days of the same work stay distinct in the request list.
     */
    public static CalendarRequest compose(String kindName, String name, String date, String start, String end,
                                          String assignee, String assetKey, String assetName, String note,
                                          String kindLabel, String dateLabel, String timeLabel, String assigneeLabel,
                                          String assetLabel, String noteLabel, String fallback) {
        String summary = summary(kindName, name, date, fallback);
        String description = description(kindName, date, start, end, assignee, assetKey, assetName, note,
                kindLabel, dateLabel, timeLabel, assigneeLabel, assetLabel, noteLabel);
        return new CalendarRequest(summary, description);
    }

    static String summary(String kindName, String name, String date, String fallback) {
        String kind = oneLine(kindName);
        String title = oneLine(name);
        String day = oneLine(date);
        String head;
        if (!kind.isEmpty() && !title.isEmpty() && !kind.equalsIgnoreCase(title)) {
            head = kind + ": " + title;
        } else if (!kind.isEmpty()) {
            head = kind;
        } else {
            head = title;
        }
        String text;
        if (head.isEmpty()) {
            text = day.isEmpty() ? oneLine(fallback) : day;
        } else if (day.isEmpty() || head.indexOf(day) >= 0) {
            text = head;
        } else {
            text = head + ", " + day;
        }
        if (text.isEmpty()) {
            text = "Calendar";
        }
        return clip(text, SUMMARY_LIMIT);
    }

    static String description(String kindName, String date, String start, String end, String assignee,
                              String assetKey, String assetName, String note, String kindLabel, String dateLabel,
                              String timeLabel, String assigneeLabel, String assetLabel, String noteLabel) {
        StringBuilder out = new StringBuilder();
        line(out, kindLabel, oneLine(kindName));
        line(out, dateLabel, oneLine(date));
        line(out, timeLabel, time(start, end));
        line(out, assigneeLabel, oneLine(assignee));
        line(out, assetLabel, equipment(assetKey, assetName));
        String body = note == null ? "" : note.trim();
        if (!body.isEmpty()) {
            if (out.length() > 0) {
                out.append('\n');
            }
            String caption = oneLine(noteLabel);
            if (!caption.isEmpty()) {
                out.append(caption).append(":\n");
            }
            out.append(body);
        }
        return clip(out.toString(), DESCRIPTION_LIMIT);
    }

    private static void line(StringBuilder out, String label, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (out.length() > 0) {
            out.append('\n');
        }
        String caption = oneLine(label);
        if (!caption.isEmpty()) {
            out.append(caption).append(": ");
        }
        out.append(value);
    }

    private static String time(String start, String end) {
        String from = oneLine(start);
        String to = oneLine(end);
        if (from.isEmpty()) {
            return to;
        }
        if (to.isEmpty()) {
            return from;
        }
        return from + "\u2013" + to;
    }

    private static String equipment(String assetKey, String assetName) {
        String key = oneLine(assetKey);
        String title = oneLine(assetName);
        if (key.isEmpty()) {
            return title;
        }
        if (title.isEmpty()) {
            return key;
        }
        return key + " " + title;
    }

    private static String oneLine(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        boolean space = false;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == ' ' || current == '\n' || current == '\r' || current == '\t') {
                space = out.length() > 0;
                continue;
            }
            if (space) {
                out.append(' ');
                space = false;
            }
            out.append(current);
        }
        return out.toString();
    }

    private static String clip(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value == null ? "" : value;
        }
        return value.substring(0, limit);
    }
}
