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

    public static boolean isSerial(String key, String label) {
        String foldedKey = fold(key);
        String foldedLabel = fold(label);
        if ("serial".equals(foldedKey) || "sn".equals(foldedKey) || "s/n".equals(foldedKey)) {
            return true;
        }
        return foldedLabel.contains("serial") || foldedLabel.contains("серийн") || foldedLabel.contains("заводск")
                || "sn".equals(foldedLabel) || "s/n".equals(foldedLabel);
    }

    public static boolean isInventory(String key, String label) {
        String foldedKey = fold(key);
        String foldedLabel = fold(label);
        if ("inventory".equals(foldedKey) || "inv".equals(foldedKey)) {
            return true;
        }
        return foldedLabel.contains("inventory") || foldedLabel.contains("инвентар");
    }

    private static boolean same(String left, String right) {
        String one = fold(left);
        String other = fold(right);
        if (one.isEmpty() || other.isEmpty()) {
            return true;
        }
        return one.equals(other);
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
}
