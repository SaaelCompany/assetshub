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
    private static final String DEFAULT_KINDS = "to,kts,verify,staff,event,update";
    private static final String DEFAULT_MARKS = "work||172b4d|8|1,night||4338ca|8|2,trip||0f766e|8|1,leave||d97706|0|0,sick||e11d48|0|0";
    private static final int KIND_LIMIT = 24;
    private static final int LABEL_LIMIT = 40;
    private static final List<String> UNITS = Arrays.asList("once", "day", "week", "month", "year");
    private static final List<String> GRIDS = Arrays.asList("8", "12", "16", "24", "32");

    private ModuleSetup() {
    }

    public static ModuleSetupDto read(String stored) {
        Map<String, String> values = split(stored);
        ModuleSetupDto setup = new ModuleSetupDto();
        String kindList = kinds(values.get("kinds"), !"2".equals(values.get("kver")));
        setup.setScheduleKinds(kindList);
        setup.setScheduleUnit(unit(values.get("unit")));
        setup.setScheduleLayer(layer(values.get("layer"), kindList));
        setup.setScheduleNotify(Boolean.valueOf("1".equals(values.get("notify"))));
        setup.setMapWidth(Integer.valueOf(FloorBounds.width(number(values.get("mw")))));
        setup.setMapHeight(Integer.valueOf(FloorBounds.height(number(values.get("mh")))));
        setup.setMapGrid(Integer.valueOf(grid(values.get("grid"))));
        setup.setMapSnap(Boolean.valueOf(!"0".equals(values.get("snap"))));
        setup.setReportHidden(hidden(values.get("hidden")));
        setup.setReportCharts(charts(values.get("charts")));
        setup.setReportMail(Boolean.valueOf("1".equals(values.get("rmail"))));
        setup.setReportMailEvery(mailEvery(values.get("revery")));
        setup.setReportMailDay(Integer.valueOf(mailDay(values.get("rday"), setup.getReportMailEvery())));
        setup.setReportMailTo(mailTo(values.get("rto")));
        setup.setReportMailSent(mailSent(values.get("rsent")));
        setup.setReportMailPart(mailPart(values.get("rpart")));
        setup.setLifeApproval(Boolean.valueOf(!"0".equals(values.get("approve"))));
        setup.setLifeApprover(who(values.get("who")));
        setup.setLifeCurrency(currency(values.get("cur")));
        setup.setLifeMonths(Integer.valueOf(months(values.get("months"))));
        setup.setStaffMarks(staffMarks(values.get("marks")));
        setup.setStaffOrder(staffOrder(values.get("order")));
        setup.setStaffPlaceOrder(staffPlaces(values.get("places")));
        setup.setScheduleBoards(boards(values.get("boards")));
        setup.setScheduleOpen(openBoard(values.get("open"), values.get("layer")));
        setup.setFurniture(furniture(values.get("furn")));
        return setup;
    }

    public static String write(ModuleSetupDto draft) {
        ModuleSetupDto value = draft == null ? new ModuleSetupDto() : draft;
        StringBuilder stored = new StringBuilder();
        String kindList = kinds(value.getScheduleKinds(), false);
        append(stored, "kinds", kindList);
        append(stored, "kver", "2");
        append(stored, "unit", unit(value.getScheduleUnit()));
        append(stored, "layer", layer(value.getScheduleLayer(), kindList));
        append(stored, "notify", Boolean.TRUE.equals(value.getScheduleNotify()) ? "1" : "0");
        append(stored, "mw", String.valueOf(FloorBounds.width(value.getMapWidth() == null ? 0 : value.getMapWidth().intValue())));
        append(stored, "mh", String.valueOf(FloorBounds.height(value.getMapHeight() == null ? 0 : value.getMapHeight().intValue())));
        append(stored, "grid", String.valueOf(grid(value.getMapGrid() == null ? "" : String.valueOf(value.getMapGrid()))));
        append(stored, "snap", Boolean.FALSE.equals(value.getMapSnap()) ? "0" : "1");
        append(stored, "hidden", hidden(value.getReportHidden()));
        append(stored, "charts", charts(value.getReportCharts()));
        String every = mailEvery(value.getReportMailEvery());
        append(stored, "rmail", Boolean.TRUE.equals(value.getReportMail()) ? "1" : "0");
        append(stored, "revery", every);
        append(stored, "rday", String.valueOf(value.getReportMailDay() == null ? mailDay("", every) : mailDay(String.valueOf(value.getReportMailDay()), every)));
        append(stored, "rto", mailTo(value.getReportMailTo()));
        append(stored, "rsent", mailSent(value.getReportMailSent()));
        append(stored, "rpart", mailPart(value.getReportMailPart()));
        append(stored, "approve", Boolean.FALSE.equals(value.getLifeApproval()) ? "0" : "1");
        append(stored, "who", who(value.getLifeApprover()));
        append(stored, "cur", currency(value.getLifeCurrency()));
        append(stored, "months", String.valueOf(months(value.getLifeMonths() == null ? "" : String.valueOf(value.getLifeMonths()))));
        append(stored, "marks", staffMarks(value.getStaffMarks()));
        append(stored, "order", staffOrder(value.getStaffOrder()));
        append(stored, "places", staffPlaces(value.getStaffPlaceOrder()));
        append(stored, "boards", boards(value.getScheduleBoards()));
        append(stored, "open", openBoard(value.getScheduleOpen(), value.getScheduleLayer()));
        append(stored, "furn", furniture(value.getFurniture()));
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

    /** Id to the project's own name. An empty name means the built-in label. */
    public static Map<String, String> kindLabels(String storedOptions) {
        Map<String, String> values = split(storedOptions);
        return parseKinds(values.get("kinds"), !"2".equals(values.get("kver")));
    }

    private static Map<String, String> parseKinds(String raw, boolean migrate) {
        String text = raw == null ? "" : raw.trim();
        if (migrate && (text.isEmpty() || "to,kts,verify".equals(text))) {
            text = DEFAULT_KINDS;
        }
        Map<String, String> labels = new LinkedHashMap<String, String>();
        String[] parts = text.split(",");
        for (String part : parts) {
            String piece = part == null ? "" : part.trim();
            if (piece.isEmpty()) {
                continue;
            }
            int bar = piece.indexOf('|');
            String id = WorkKind.id(bar < 0 ? piece : piece.substring(0, bar));
            if (id.isEmpty() || labels.containsKey(id)) {
                continue;
            }
            labels.put(id, bar < 0 ? "" : clipLabel(piece.substring(bar + 1)));
            if (labels.size() >= KIND_LIMIT) {
                break;
            }
        }
        if (labels.isEmpty()) {
            String[] defaults = DEFAULT_KINDS.split(",");
            for (String id : defaults) {
                labels.put(id, "");
            }
        }
        return labels;
    }

    private static String staffMarks(String raw) {
        Map<String, String[]> rows = new LinkedHashMap<String, String[]>();
        String source = raw == null || raw.trim().isEmpty() ? DEFAULT_MARKS : raw;
        String[] parts = source.split(",");
        for (String part : parts) {
            if (rows.size() >= 12) {
                break;
            }
            String[] piece = part.split("\\|", -1);
            if (piece.length < 1) {
                continue;
            }
            String id = WorkKind.id(piece[0]);
            if (id.isEmpty() || "off".equals(id) || "hours".equals(id) || rows.containsKey(id)) {
                continue;
            }
            String label = markLabel(piece.length > 1 ? piece[1] : "");
            String color = markColor(piece.length > 2 ? piece[2] : "");
            int hours = markHours(piece.length > 3 ? piece[3] : "8");
            int factor = markFactor(piece, hours, id);
            String formula = markFormula(piece.length > 5 ? piece[5] : "");
            rows.put(id, new String[] { label, color, String.valueOf(hours), String.valueOf(factor), formula });
        }
        if (!rows.containsKey("work")) {
            Map<String, String[]> withWork = new LinkedHashMap<String, String[]>();
            withWork.put("work", new String[] { "", "172b4d", "8", "1", "" });
            for (Map.Entry<String, String[]> entry : rows.entrySet()) {
                if (withWork.size() >= 12) {
                    break;
                }
                withWork.put(entry.getKey(), entry.getValue());
            }
            rows = withWork;
        }
        if (rows.isEmpty()) {
            return DEFAULT_MARKS;
        }
        StringBuilder stored = new StringBuilder();
        for (Map.Entry<String, String[]> entry : rows.entrySet()) {
            if (stored.length() > 0) {
                stored.append(',');
            }
            String[] value = entry.getValue();
            stored.append(entry.getKey()).append('|').append(value[0]).append('|').append(value[1]).append('|').append(value[2]).append('|').append(value[3]);
            if (value.length > 4 && value[4] != null && !value[4].isEmpty()) {
                stored.append('|').append(value[4]);
            }
        }
        return stored.toString();
    }

    /** Keeps an Excel-style formula. The client evaluates it; an empty field leaves the factor in charge. */
    private static String markFormula(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.replace(" ", "").trim().toLowerCase(Locale.ROOT);
        if (text.startsWith("=")) {
            text = text.substring(1).trim();
        }
        if (text.isEmpty() || text.length() > 96) {
            return "";
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z')) {
                continue;
            }
            if ("+-*/^().;=<>$:%".indexOf(c) >= 0) {
                continue;
            }
            return "";
        }
        return text;
    }

    /** 0 leaves the day out of the totals, 1 uses the person's hours, 2 doubles them. A missing field keeps the old meaning. */
    private static int markFactor(String[] piece, int hours, String id) {
        if (piece.length > 4 && piece[4] != null && !piece[4].trim().isEmpty()) {
            int value = number(piece[4]);
            if (value == 0 || value == 1 || value == 2) {
                return value;
            }
        }
        if (hours <= 0) {
            return 0;
        }
        if ("night".equals(id)) {
            return 2;
        }
        return 1;
    }

    private static String boards(String raw) {
        boolean calendar = false;
        boolean roster = false;
        if (raw != null) {
            String[] parts = raw.split(",");
            for (String part : parts) {
                String id = part == null ? "" : part.trim().toLowerCase(Locale.ROOT);
                if ("calendar".equals(id)) {
                    calendar = true;
                } else if ("roster".equals(id)) {
                    roster = true;
                }
            }
        }
        if (calendar && roster || !calendar && !roster) {
            return "calendar,roster";
        }
        return calendar ? "calendar" : "roster";
    }

    private static String openBoard(String raw, String layer) {
        String id = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if ("calendar".equals(id) || "roster".equals(id)) {
            return id;
        }
        String storedLayer = layer == null ? "" : layer.trim().toLowerCase(Locale.ROOT);
        if ("roster".equals(storedLayer) || "staff".equals(storedLayer)) {
            return "roster";
        }
        return "calendar";
    }

    private static String staffOrder(String raw) {
        List<String> keys = new ArrayList<String>();
        if (raw != null) {
            String[] parts = raw.split(",");
            for (String part : parts) {
                String key = part == null ? "" : part.trim();
                if (key.length() > 80 || !key.matches("[A-Za-z0-9._@-]+") || keys.contains(key)) {
                    continue;
                }
                keys.add(key);
                if (keys.size() >= 60) {
                    break;
                }
            }
        }
        StringBuilder stored = new StringBuilder();
        for (String key : keys) {
            if (stored.length() > 0) {
                stored.append(',');
            }
            stored.append(key);
        }
        return stored.toString();
    }

    private static String staffPlaces(String raw) {
        List<String> ids = new ArrayList<String>();
        if (raw != null) {
            String[] parts = raw.split(",");
            for (String part : parts) {
                String key = part == null ? "" : part.trim();
                if (!key.matches("\\d{1,9}")) {
                    continue;
                }
                try {
                    long value = Long.parseLong(key);
                    if (value < 0) {
                        continue;
                    }
                    key = String.valueOf(value);
                } catch (NumberFormatException ex) {
                    continue;
                }
                if (ids.contains(key)) {
                    continue;
                }
                ids.add(key);
                if (ids.size() >= 80) {
                    break;
                }
            }
        }
        StringBuilder stored = new StringBuilder();
        for (String key : ids) {
            if (stored.length() > 0) {
                stored.append(',');
            }
            stored.append(key);
        }
        return stored.toString();
    }

    private static String markLabel(String raw) {
        return clipLabel(raw == null ? "" : raw.replace('#', ' '));
    }

    private static String markColor(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        return value.matches("[0-9a-f]{6}") ? value : "64748b";
    }

    private static int markHours(String raw) {
        int value = number(raw);
        if (value < 0) {
            return 0;
        }
        return value > 24 ? 24 : value;
    }

    private static String layer(String raw, String kinds) {
        String id = WorkKind.id(raw);
        if (id.isEmpty()) {
            return "";
        }
        if ("roster".equals(id) || "staff".equals(id)) {
            return "roster";
        }
        return parseKinds(kinds, false).containsKey(id) ? id : "";
    }

    private static String kinds(String raw, boolean migrate) {
        StringBuilder stored = new StringBuilder();
        for (Map.Entry<String, String> entry : parseKinds(raw, migrate).entrySet()) {
            if (stored.length() > 0) {
                stored.append(',');
            }
            stored.append(entry.getKey());
            if (!entry.getValue().isEmpty()) {
                stored.append('|').append(entry.getValue());
            }
        }
        return stored.toString();
    }

    private static String clipLabel(String raw) {
        String value = raw == null ? "" : raw.replace(',', ' ').replace(';', ' ').replace('|', ' ').replace('=', ' ');
        value = value.trim().replaceAll("\\s+", " ");
        if (value.length() > LABEL_LIMIT) {
            value = value.substring(0, LABEL_LIMIT).trim();
        }
        return value;
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

    private static String charts(String raw) {
        StringBuilder stored = new StringBuilder();
        if (raw == null) {
            return "";
        }
        String[] parts = raw.split(",");
        List<String> seen = new ArrayList<String>();
        for (String part : parts) {
            int mark = part.indexOf(':');
            if (mark <= 0) {
                continue;
            }
            String key = part.substring(0, mark).trim().toLowerCase(Locale.ROOT);
            String chart = ReportCatalog.chart(part.substring(mark + 1));
            if (!ReportCatalog.isStandard(key) || chart.isEmpty() || seen.contains(key)) {
                continue;
            }
            seen.add(key);
            if (stored.length() > 0) {
                stored.append(',');
            }
            stored.append(key).append(':').append(chart);
        }
        return stored.toString();
    }

    private static String mailEvery(String raw) {
        return raw != null && "month".equals(raw.trim().toLowerCase(Locale.ROOT)) ? "month" : "week";
    }

    private static int mailDay(String raw, String every) {
        int value = number(raw);
        if ("month".equals(every)) {
            if (value < 1) {
                return 1;
            }
            return value > 31 ? 31 : value;
        }
        if (value < 1 || value > 7) {
            return 1;
        }
        return value;
    }

    private static String mailTo(String raw) {
        StringBuilder stored = new StringBuilder();
        if (raw == null) {
            return "";
        }
        List<String> seen = new ArrayList<String>();
        String[] parts = raw.split(",");
        for (String part : parts) {
            String key = part == null ? "" : part.trim();
            if (key.isEmpty() || key.length() > 80 || !key.matches("[A-Za-z0-9._@-]+") || seen.contains(key)) {
                continue;
            }
            if (seen.size() >= 20) {
                break;
            }
            seen.add(key);
            if (stored.length() > 0) {
                stored.append(',');
            }
            stored.append(key);
        }
        return stored.toString();
    }

    private static String mailSent(String raw) {
        String value = raw == null ? "" : raw.trim();
        return value.matches("\\d{4}-\\d{2}-\\d{2}") ? value : "";
    }

    /** {@code yyyy-mm-dd:user,user} — recipients already queued for that occurrence. */
    private static String mailPart(String raw) {
        String value = raw == null ? "" : raw.trim();
        int colon = value.indexOf(':');
        if (colon != 10) {
            return "";
        }
        String day = mailSent(value.substring(0, 10));
        if (day.isEmpty()) {
            return "";
        }
        String keys = mailTo(value.substring(11));
        if (keys.isEmpty()) {
            return "";
        }
        return day + ":" + keys;
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
        if (raw == null || raw.trim().isEmpty()) {
            return 60;
        }
        int value = number(raw);
        if (value < 0) {
            return 0;
        }
        return value > 600 ? 600 : value;
    }

    /** id^name^kind,x,y,w,h,color,label,angle!shape|next . Colors are stored without a hash. Angle is optional. */
    private static String furniture(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        StringBuilder stored = new StringBuilder();
        String[] templates = raw.split("\\|");
        int count = 0;
        for (String template : templates) {
            if (count >= 24) {
                break;
            }
            String[] head = template.split("\\^", 3);
            if (head.length < 3) {
                continue;
            }
            String id = furnId(head[0]);
            String name = furnLabel(head[1]);
            String shapes = furnShapes(head[2]);
            if (id.isEmpty() || name.isEmpty() || shapes.isEmpty()) {
                continue;
            }
            if (stored.length() > 0) {
                stored.append('|');
            }
            stored.append(id).append('^').append(name).append('^').append(shapes);
            count++;
        }
        return stored.toString();
    }

    private static String furnId(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return value.matches("[a-z][a-z0-9]{0,7}") ? value : "";
    }

    private static String furnLabel(String raw) {
        return clipLabel(raw).replace('^', ' ').replace('!', ' ').trim();
    }

    private static String furnShapes(String raw) {
        StringBuilder stored = new StringBuilder();
        String[] parts = raw.split("!");
        int count = 0;
        for (String part : parts) {
            if (count >= 16) {
                break;
            }
            String[] bits = part.split(",", -1);
            if (bits.length < 5) {
                continue;
            }
            String kind = FloorBounds.kind(bits[0]);
            int x = furnCoord(bits[1], 2000);
            int y = furnCoord(bits[2], 2000);
            int w = Math.max(FloorBounds.minSize(kind), furnCoord(bits[3], 800));
            int h = Math.max(FloorBounds.minSize(kind), furnCoord(bits[4], 800));
            String color = "";
            if (bits.length > 5 && bits[5] != null && !bits[5].trim().isEmpty()) {
                String rawColor = bits[5].trim();
                if (!rawColor.startsWith("#")) {
                    rawColor = "#" + rawColor;
                }
                color = FloorBounds.color(rawColor).replace("#", "");
            }
            String label = bits.length > 6 ? furnLabel(bits[6]) : "";
            int angle = bits.length > 7 ? FloorBounds.angle(number(bits[7])) : 0;
            if (stored.length() > 0) {
                stored.append('!');
            }
            stored.append(kind).append(',').append(x).append(',').append(y).append(',').append(w).append(',').append(h)
                    .append(',').append(color).append(',').append(label).append(',').append(angle);
            count++;
        }
        return stored.toString();
    }

    private static int furnCoord(String raw, int max) {
        int value = number(raw);
        if (value < 0) {
            return 0;
        }
        return value > max ? max : value;
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
