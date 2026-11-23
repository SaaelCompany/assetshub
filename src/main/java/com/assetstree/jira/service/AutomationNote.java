package com.assetstree.jira.service;

/** The sentence that used to be written on a linked request when equipment changed. */
public final class AutomationNote {
    private static final String[] RUSSIAN = { "создано", "изменено", "удалено", "восстановлено" };
    private static final String[] ENGLISH = { "created", "changed", "removed", "restored" };

    private AutomationNote() {
    }

    public static String text(String pattern, String objectKey, String name, String verb) {
        String body = pattern == null ? "" : pattern;
        body = body.replace("{0}", objectKey == null ? "" : objectKey);
        body = body.replace("{1}", name == null ? "" : name);
        body = body.replace("{2}", verb == null ? "" : verb);
        return body;
    }

    /** True for a comment this plugin wrote about an equipment change. */
    public static boolean equipmentChange(String body) {
        String text = body == null ? "" : body.replace('\r', ' ').trim();
        return sentence(text, "Оборудование ", "«", "»", RUSSIAN)
                || sentence(text, "Equipment ", "\"", "\"", ENGLISH);
    }

    private static boolean sentence(String text, String start, String open, String close, String[] verbs) {
        if (!text.startsWith(start) || !text.endsWith(".")) {
            return false;
        }
        int opened = text.indexOf(open, start.length());
        int closed = opened < 0 ? -1 : text.indexOf(close, opened + open.length());
        int mark = text.lastIndexOf(": ");
        if (opened < 0 || closed < opened || mark < closed) {
            return false;
        }
        String verb = text.substring(mark + 2, text.length() - 1);
        for (int i = 0; i < verbs.length; i++) {
            if (verbs[i].equals(verb)) {
                return true;
            }
        }
        return false;
    }
}
