package com.assetstree.jira.model;

/** Turns «Бондаренко Олег Сергеевич» into «Бондаренко О.С.». The first word stays, the rest become initials. */
public final class NameShort {
    private NameShort() {
    }

    public static String of(String display) {
        String text = display == null ? "" : display.trim().replaceAll("\\s+", " ");
        if (text.isEmpty()) {
            return "";
        }
        String[] parts = text.split(" ");
        if (parts.length == 1) {
            return parts[0];
        }
        StringBuilder out = new StringBuilder(parts[0]);
        StringBuilder initials = new StringBuilder();
        for (int i = 1; i < parts.length; i++) {
            initials.append(initialsOf(parts[i]));
        }
        if (initials.length() > 0) {
            out.append(' ').append(initials);
        }
        return out.toString();
    }

    private static String initialsOf(String word) {
        String raw = word == null ? "" : word.trim();
        if (raw.isEmpty()) {
            return "";
        }
        String[] bits = raw.split("-");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < bits.length; i++) {
            String bit = bits[i].replace(".", "");
            if (bit.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append('-');
            }
            if (bit.length() <= 2 && lettersOnly(bit)) {
                for (int c = 0; c < bit.length(); c++) {
                    out.append(Character.toUpperCase(bit.charAt(c))).append('.');
                }
            } else {
                out.append(Character.toUpperCase(bit.charAt(0))).append('.');
            }
        }
        return out.toString();
    }

    private static boolean lettersOnly(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isLetter(value.charAt(i))) {
                return false;
            }
        }
        return value.length() > 0;
    }
}
