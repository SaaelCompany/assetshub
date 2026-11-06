package com.assetstree.jira.model;

import com.assetstree.jira.dto.ModuleSetupDto;

import java.util.Locale;

/** How long a portal-discovered device is kept, and what happens when it stops answering. */
public final class TniPolicy {
    public static final String TRASH = "trash";
    public static final String DELETE = "delete";
    public static final String COMPUTER = "computer";
    public static final String LAPTOP = "laptop";
    public static final String PHONE = "phone";
    public static final String TABLET = "tablet";

    private TniPolicy() {
    }

    public static int keepDays(int raw) {
        return between(raw, 1, 3650, 90);
    }

    public static int probeDays(int raw) {
        return between(raw, 1, 30, 3);
    }

    public static int probeTries(int raw) {
        return between(raw, 1, 30, 3);
    }

    public static String action(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return DELETE.equals(value) ? DELETE : TRASH;
    }

    public static String typeKey(String raw) {
        if (raw == null) {
            return "";
        }
        String key = raw.trim().toLowerCase(Locale.ROOT);
        return AssetValidator.isTypeKey(key) ? key : "";
    }

    public static String typeFor(ModuleSetupDto setup, String kind) {
        if (setup == null || kind == null) {
            return "";
        }
        if (LAPTOP.equals(kind)) {
            return typeKey(setup.getTniLaptop());
        }
        if (PHONE.equals(kind)) {
            return typeKey(setup.getTniPhone());
        }
        if (TABLET.equals(kind)) {
            return typeKey(setup.getTniTablet());
        }
        if (COMPUTER.equals(kind)) {
            return typeKey(setup.getTniComputer());
        }
        return "";
    }

    private static int between(int raw, int min, int max, int fallback) {
        if (raw < min || raw > max) {
            return fallback;
        }
        return raw;
    }
}
