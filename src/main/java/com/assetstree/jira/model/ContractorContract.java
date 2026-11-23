package com.assetstree.jira.model;

import java.time.LocalDate;

/** Optional contractor maintenance stored on a lifecycle row. */
public final class ContractorContract {
    public static final int NAME_LIMIT = 120;
    public static final int NUMBER_LIMIT = 160;

    private ContractorContract() {
    }

    /** A contract is on the card only when a contractor or a contract number is saved. */
    public static boolean present(String contractor, String number) {
        return !blank(contractor) || !blank(number);
    }

    /**
     * Dates without a contractor and a number are rejected.
     * An end before the start is rejected. Empty values clear the contract.
     * Returns the error key, or null when the values can be stored.
     */
    public static String reject(String contractor, String number, String from, String until) {
        String name = trim(contractor);
        String no = trim(number);
        if (name.length() > NAME_LIMIT || no.length() > NUMBER_LIMIT) {
            return "asset-tree.error.life";
        }
        String start = FieldDates.canonical(from);
        String end = FieldDates.canonical(until);
        if (start == null || end == null) {
            return "asset-tree.error.life";
        }
        if (!present(name, no)) {
            return start.isEmpty() && end.isEmpty() ? null : "asset-tree.error.life";
        }
        if (!start.isEmpty() && !end.isEmpty() && start.compareTo(end) > 0) {
            return "asset-tree.error.life";
        }
        return null;
    }

    /** 0 none, 1 in force, 2 ended, 3 not started yet. */
    public static int phase(String contractor, String number, String from, String until, LocalDate today) {
        if (!present(contractor, number) || today == null) {
            return 0;
        }
        String start = FieldDates.canonical(from);
        String end = FieldDates.canonical(until);
        if (start == null) {
            start = "";
        }
        if (end == null) {
            end = "";
        }
        String day = today.toString();
        if (!start.isEmpty() && day.compareTo(start) < 0) {
            return 3;
        }
        if (!end.isEmpty() && day.compareTo(end) > 0) {
            return 2;
        }
        return 1;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
