package com.assetstree.jira.model;

import java.util.Locale;

/**
 * Serial and inventory numbers used to tell two cards with the same name apart.
 * A number is compared only when both sides have it filled in.
 */
public final class DuplicateMarks {
    private DuplicateMarks() {
    }

    public static boolean agrees(String leftSerial, String rightSerial, String leftInventory, String rightInventory) {
        return same(leftSerial, rightSerial) && same(leftInventory, rightInventory);
    }

    /** Both sides filled the same serial or the same inventory number, and nothing filled disagrees. */
    public static boolean shares(String leftSerial, String rightSerial, String leftInventory, String rightInventory) {
        return agrees(leftSerial, rightSerial, leftInventory, rightInventory)
                && (equalFilled(leftSerial, rightSerial) || equalFilled(leftInventory, rightInventory));
    }

    /**
     * A printer, phone, laptop or any other card with the same name.
     * A shared number agrees. Both sides empty also agree, so the next import updates that card.
     * A number on only one side does not, so an empty row leaves a numbered card alone.
     */
    public static boolean sameObject(String leftSerial, String rightSerial, String leftInventory, String rightInventory) {
        if (!agrees(leftSerial, rightSerial, leftInventory, rightInventory)) {
            return false;
        }
        if (shares(leftSerial, rightSerial, leftInventory, rightInventory)) {
            return true;
        }
        return blank(leftSerial) && blank(rightSerial) && blank(leftInventory) && blank(rightInventory);
    }

    public static boolean isSerial(String key, String label) {
        String bareKey = bare(key);
        String bareLabel = bare(label);
        if ("serial".equals(bareKey) || "sn".equals(bareKey) || "s/n".equals(bareKey)) {
            return true;
        }
        return bareLabel.contains("serial") || bareLabel.contains("серийн") || bareLabel.contains("заводск")
                || "sn".equals(bareLabel) || "s/n".equals(bareLabel) || bareLabel.startsWith("s/n");
    }

    public static boolean isInventory(String key, String label) {
        String bareKey = bare(key);
        String bareLabel = bare(label);
        if ("inventory".equals(bareKey) || "inv".equals(bareKey) || "инв".equals(bareKey)) {
            return true;
        }
        return bareLabel.contains("inventory") || bareLabel.contains("инвентар")
                || "inv".equals(bareLabel) || "инв".equals(bareLabel);
    }

    private static boolean same(String left, String right) {
        String one = fold(left);
        String other = fold(right);
        if (one.isEmpty() || other.isEmpty()) {
            return true;
        }
        return one.equals(other);
    }

    private static boolean blank(String raw) {
        return fold(raw).isEmpty();
    }

    private static boolean equalFilled(String left, String right) {
        String one = fold(left);
        String other = fold(right);
        return !one.isEmpty() && one.equals(other);
    }

    private static String fold(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT).replace('ё', 'е');
    }

    /** Lower case, without spaces or trailing punctuation such as the colon in {@code S/N:}. */
    private static String bare(String raw) {
        String folded = fold(raw);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < folded.length(); i++) {
            char current = folded.charAt(i);
            if (Character.isWhitespace(current) || current == ':' || current == '.' || current == ';'
                    || current == ',' || current == '#' || current == '№') {
                continue;
            }
            builder.append(current);
        }
        return builder.toString();
    }
}
