package com.assetstree.jira.model;

import com.assetstree.jira.dto.ModuleSetupDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Compact storage for {@link ModuleSetupDto}. Unknown keys are ignored. Missing keys keep the defaults. */
public final class ModuleSetup {
    private static final List<String> KINDS = Arrays.asList("to", "kts", "verify");
    private static final List<String> UNITS = Arrays.asList("once", "day", "week", "month", "year");
    private static final List<String> GRIDS = Arrays.asList("8", "12", "16", "24", "32");

    private ModuleSetup() {
    }

    public static ModuleSetupDto read(String stored) {
        Map<String, String> values = split(stored);
        ModuleSetupDto setup = new ModuleSetupDto();
        setup.setScheduleKinds(kinds(values.get("kinds")));
        setup.setScheduleUnit(unit(values.get("unit")));
        setup.setScheduleNotify(Boolean.valueOf("1".equals(values.get("notify"))));
        setup.setMapWidth(Integer.valueOf(FloorBounds.width(number(values.get("mw")))));
        setup.setMapHeight(Integer.valueOf(FloorBounds.height(number(values.get("mh")))));
        setup.setMapGrid(Integer.valueOf(grid(values.get("grid"))));
        setup.setMapSnap(Boolean.valueOf(!"0".equals(values.get("snap"))));
        setup.setReportHidden(hidden(values.get("hidden")));
        setup.setLifeApproval(Boolean.valueOf(!"0".equals(values.get("approve"))));
        setup.setLifeApprover(who(values.get("who")));
        setup.setLifeCurrency(currency(values.get("cur")));
        setup.setLifeMonths(Integer.valueOf(months(values.get("months"))));
        return setup;
    }

    public static String write(ModuleSetupDto draft) {
        ModuleSetupDto value = draft == null ? new ModuleSetupDto() : draft;
        StringBuilder stored = new StringBuilder();
        append(stored, "kinds", kinds(value.getScheduleKinds()));
        append(stored, "unit", unit(value.getScheduleUnit()));
        append(stored, "notify", Boolean.TRUE.equals(value.getScheduleNotify()) ? "1" : "0");
        append(stored, "mw", String.valueOf(FloorBounds.width(value.getMapWidth() == null ? 0 : value.getMapWidth().intValue())));
        append(stored, "mh", String.valueOf(FloorBounds.height(value.getMapHeight() == null ? 0 : value.getMapHeight().intValue())));
        append(stored, "grid", String.valueOf(grid(value.getMapGrid() == null ? "" : String.valueOf(value.getMapGrid()))));
        append(stored, "snap", Boolean.FALSE.equals(value.getMapSnap()) ? "0" : "1");
        append(stored, "hidden", hidden(value.getReportHidden()));
        append(stored, "approve", Boolean.FALSE.equals(value.getLifeApproval()) ? "0" : "1");
        append(stored, "who", who(value.getLifeApprover()));
        append(stored, "cur", currency(value.getLifeCurrency()));
        append(stored, "months", String.valueOf(months(value.getLifeMonths() == null ? "" : String.valueOf(value.getLifeMonths()))));
        return stored.toString();
    }

    private static void append(StringBuilder stored, String key, String value) {
        if (stored.length() > 0) {
            stored.append(';');
        }
        stored.append(key).append('=').append(value == null ? "" : value);
    }

    private static Map<String, String> split(String stored) {
        Map<String, String> values = new LinkedHashMap<String, String>();
        if (stored == null || stored.trim().isEmpty()) {
            return values;
        }
        String[] parts = stored.split(";");
        for (String part : parts) {
            int mark = part.indexOf('=');
            if (mark <= 0) {
                continue;
            }
            values.put(part.substring(0, mark).trim().toLowerCase(Locale.ROOT), part.substring(mark + 1).trim());
        }
        return values;
    }

    private static String kinds(String raw) {
        List<String> kept = new ArrayList<String>();
        if (raw != null) {
            String[] parts = raw.split(",");
            for (String part : parts) {
                String key = part.trim().toLowerCase(Locale.ROOT);
                if (KINDS.contains(key) && !kept.contains(key)) {
                    kept.add(key);
                }
            }
        }
        if (kept.isEmpty()) {
            return "to,kts,verify";
        }
        StringBuilder stored = new StringBuilder();
        for (String key : KINDS) {
            if (!kept.contains(key)) {
                continue;
            }
            if (stored.length() > 0) {
                stored.append(',');
            }
            stored.append(key);
        }
        return stored.toString();
    }

    private static String unit(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return UNITS.contains(value) ? value : "month";
    }

    private static int grid(String raw) {
        String value = raw == null ? "" : raw.trim();
        return GRIDS.contains(value) ? Integer.parseInt(value) : 24;
    }

    private static String hidden(String raw) {
        List<String> kept = new ArrayList<String>();
        if (raw != null) {
            String[] parts = raw.split(",");
            for (String part : parts) {
                String key = part.trim().toLowerCase(Locale.ROOT);
                if (ReportCatalog.isStandard(key) && !kept.contains(key)) {
                    kept.add(key);
                }
            }
        }
        StringBuilder stored = new StringBuilder();
        for (String key : kept) {
            if (stored.length() > 0) {
                stored.append(',');
            }
            stored.append(key);
        }
        return stored.toString();
    }

    private static String who(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.length() > 80 || !value.matches("[A-Za-z0-9._@-]*")) {
            return "";
        }
        return value;
    }

    private static String currency(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        return value.matches("[A-Z]{3}") ? value : "RUB";
    }

    private static int months(String raw) {
        int value = number(raw);
        if (value < 0) {
            return 0;
        }
        return value > 600 ? 600 : value;
    }

    private static int number(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
