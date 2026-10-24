package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * What a project's printed equipment card shows, and how the sticker is arranged.
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class CardLayout {
    public static final int MAX_LINES = 12;
    public static final int MAX_ELEMENTS = 24;
    public static final int MAX_TEXT = 180;
    public static final int MAX_SRC = 90000;
    public static final String SIZE_CARD = "card";
    public static final String SIZE_WIDE = "wide";
    public static final String SIZE_LABEL = "label";
    public static final String SIZE_CUSTOM = "custom";
    public static final double MIN_EDGE = 15;
    public static final double MAX_WIDTH = 210;
    public static final double MAX_HEIGHT = 297;
    public static final String QR_LEFT = "left";
    public static final String QR_RIGHT = "right";
    public static final String QR_TOP = "top";
    public static final String QR_NONE = "none";
    public static final String KIND_TEXT = "text";
    public static final String KIND_FIELD = "field";
    public static final String KIND_QR = "qr";
    public static final String KIND_LOGO = "logo";
    public static final String KIND_PICTURE = "picture";

    private static final Pattern ATTR = Pattern.compile("^attr:[a-z][a-z0-9-]{0,39}$");
    private static final Pattern ID = Pattern.compile("^[a-z0-9]{4,16}$");
    private static final Pattern COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final Pattern SRC = Pattern.compile("^data:image/(png|jpeg);base64,[A-Za-z0-9+/]+=*$");
    private static final Set<String> QRS = new HashSet<String>(Arrays.asList(QR_LEFT, QR_RIGHT, QR_TOP, QR_NONE));
    private static final Set<String> KINDS = new HashSet<String>(Arrays.asList(KIND_TEXT, KIND_FIELD, KIND_QR, KIND_LOGO, KIND_PICTURE));
    private static final Set<String> FONTS = new HashSet<String>(Arrays.asList("sans", "serif", "mono"));
    private static final Set<String> ALIGNS = new HashSet<String>(Arrays.asList("left", "center", "right"));
    private static final Set<String> BLOCKS = new LinkedHashSet<String>(Arrays.asList(
            "name", "key", "type", "place", "status", "custodian", "project", "description", "scan"));

    private String size;
    private String qr;
    private List<String> lines;
    private List<CardElement> elements;
    private Double width;
    private Double height;
    private Integer grid;

    public static CardLayout defaults() {
        CardLayout layout = new CardLayout();
        layout.size = SIZE_CARD;
        layout.qr = QR_LEFT;
        layout.lines = new ArrayList<String>(Arrays.asList("name", "type", "place", "key", "scan"));
        layout.width = Double.valueOf(86);
        layout.height = Double.valueOf(54);
        layout.grid = Integer.valueOf(2);
        return layout;
    }

    public static CardLayout normalize(CardLayout raw) {
        CardLayout layout = defaults();
        if (raw == null) {
            return layout;
        }
        applySize(layout, token(raw.size), raw.width, raw.height);
        applyGrid(layout, raw.grid);
        String qr = token(raw.qr);
        if (QRS.contains(qr)) {
            layout.qr = qr;
        }
        if (raw.lines != null) {
            layout.lines = fieldLines(raw.lines);
        }
        if (raw.elements != null) {
            layout.elements = cleanElements(raw.elements, layout.width.doubleValue(), layout.height.doubleValue());
            layout.lines = new ArrayList<String>();
            boolean assetCode = false;
            for (CardElement element : layout.elements) {
                if (KIND_FIELD.equals(element.getKind())) {
                    layout.lines.add(element.getToken());
                } else if (KIND_QR.equals(element.getKind()) && "asset".equals(element.getToken())) {
                    assetCode = true;
                }
            }
            layout.qr = assetCode ? QR_LEFT : QR_NONE;
        }
        return layout;
    }

    static String fieldToken(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        String token = BLOCKS.contains(lower) ? lower : trimmed;
        if (BLOCKS.contains(token) || ATTR.matcher(token).matches()) {
            return token;
        }
        return "";
    }

    private static void applySize(CardLayout layout, String size, Double width, Double height) {
        if (SIZE_CUSTOM.equals(size)) {
            layout.size = SIZE_CUSTOM;
            layout.width = Double.valueOf(clamp(roundMm(width == null ? 86 : width.doubleValue()), MIN_EDGE, MAX_WIDTH));
            layout.height = Double.valueOf(clamp(roundMm(height == null ? 54 : height.doubleValue()), MIN_EDGE, MAX_HEIGHT));
            return;
        }
        if (SIZE_WIDE.equals(size)) {
            layout.size = SIZE_WIDE;
            layout.width = Double.valueOf(100);
            layout.height = Double.valueOf(64);
            return;
        }
        if (SIZE_LABEL.equals(size)) {
            layout.size = SIZE_LABEL;
            layout.width = Double.valueOf(58);
            layout.height = Double.valueOf(36);
            return;
        }
        layout.size = SIZE_CARD;
        layout.width = Double.valueOf(86);
        layout.height = Double.valueOf(54);
    }

    private static void applyGrid(CardLayout layout, Integer grid) {
        if (grid != null && (grid.intValue() == 0 || grid.intValue() == 1 || grid.intValue() == 2 || grid.intValue() == 5)) {
            layout.grid = grid;
            return;
        }
        layout.grid = Integer.valueOf(2);
    }

    public static double widthOf(String size) {
        if (SIZE_WIDE.equals(size)) {
            return 100;
        }
        if (SIZE_LABEL.equals(size)) {
            return 58;
        }
        return 86;
    }

    public static double heightOf(String size) {
        if (SIZE_WIDE.equals(size)) {
            return 64;
        }
        if (SIZE_LABEL.equals(size)) {
            return 36;
        }
        return 54;
    }

    private static List<String> fieldLines(List<String> lines) {
        List<String> next = new ArrayList<String>();
        Set<String> seen = new HashSet<String>();
        for (String line : lines) {
            String token = fieldToken(line);
            if (token.isEmpty() || !seen.add(token)) {
                continue;
            }
            next.add(token);
            if (next.size() >= MAX_LINES) {
                break;
            }
        }
        return next;
    }

    private static List<CardElement> cleanElements(List<CardElement> raw, double width, double height) {
        List<CardElement> next = new ArrayList<CardElement>();
        Set<String> ids = new HashSet<String>();
        Set<String> fields = new HashSet<String>();
        boolean assetCode = false;
        boolean picture = false;
        int serial = 0;
        for (CardElement item : raw) {
            if (item == null || next.size() >= MAX_ELEMENTS) {
                continue;
            }
            String kind = token(item.getKind());
            if (!KINDS.contains(kind)) {
                continue;
            }
            CardElement kept = new CardElement();
            kept.setKind(kind);
            kept.setText("");
            kept.setToken("");
            kept.setSrc("");
            if (KIND_FIELD.equals(kind)) {
                String token = fieldToken(item.getToken());
                if (token.isEmpty() || !fields.add(token)) {
                    continue;
                }
                kept.setToken(token);
            } else if (KIND_QR.equals(kind)) {
                String mode = token(item.getToken());
                if (!mode.isEmpty() && !"asset".equals(mode)) {
                    continue;
                }
                if (assetCode) {
                    continue;
                }
                assetCode = true;
                kept.setToken("asset");
            } else if (KIND_LOGO.equals(kind)) {
                String src = item.getSrc() == null ? "" : item.getSrc().trim();
                if (src.length() > MAX_SRC || !SRC.matcher(src).matches()) {
                    continue;
                }
                kept.setSrc(src);
            } else if (KIND_PICTURE.equals(kind)) {
                if (picture) {
                    continue;
                }
                picture = true;
            } else {
                String caption = cleanText(item.getText());
                if (caption.isEmpty()) {
                    continue;
                }
                kept.setText(caption);
            }
            String id = item.getId() == null ? "" : item.getId().trim().toLowerCase(Locale.ROOT);
            if (!ID.matcher(id).matches() || !ids.add(id)) {
                id = nextId(ids, serial);
            }
            serial++;
            kept.setId(id);
            boolean mark = KIND_QR.equals(kept.getKind()) || KIND_LOGO.equals(kept.getKind()) || KIND_PICTURE.equals(kept.getKind());
            double w = mark ? 22 : 36;
            double h = mark ? 22 : 8;
            if (item.getW() >= 4) {
                w = item.getW();
            }
            if (item.getH() >= 4) {
                h = item.getH();
            }
            w = clamp(roundMm(w), 4, width);
            h = clamp(roundMm(h), 4, height);
            kept.setW(w);
            kept.setH(h);
            kept.setX(clamp(roundMm(item.getX()), 0, width - 4));
            kept.setY(clamp(roundMm(item.getY()), 0, height - 4));
            int fontSize = item.getSize();
            if (fontSize < 6 || fontSize > 36) {
                fontSize = 11;
            }
            kept.setSize(fontSize);
            String color = item.getColor() == null ? "" : item.getColor().trim();
            kept.setColor(COLOR.matcher(color).matches() ? color.toLowerCase(Locale.ROOT) : "#172b4d");
            String font = token(item.getFont());
            kept.setFont(FONTS.contains(font) ? font : "sans");
            String align = token(item.getAlign());
            kept.setAlign(ALIGNS.contains(align) ? align : "left");
            kept.setBold(item.getBold());
            next.add(kept);
        }
        return next;
    }

    private static String nextId(Set<String> used, int serial) {
        int probe = serial + 100000;
        String id = Integer.toString(probe, 36);
        while (!ID.matcher(id).matches() || !used.add(id)) {
            probe++;
            id = Integer.toString(probe, 36);
        }
        return id;
    }

    private static String cleanText(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        boolean space = false;
        int breaks = 0;
        for (int i = 0; i < raw.length() && out.length() < MAX_TEXT; i++) {
            char c = raw.charAt(i);
            if (c == '\r') {
                continue;
            }
            if (c == '\n') {
                if (out.length() == 0 || out.charAt(out.length() - 1) == '\n' || breaks >= 8) {
                    continue;
                }
                out.append('\n');
                breaks++;
                space = false;
                continue;
            }
            if (c == '\t' || c == ' ') {
                space = out.length() > 0 && out.charAt(out.length() - 1) != '\n';
                continue;
            }
            if (c < 0x20) {
                continue;
            }
            if (space) {
                out.append(' ');
                space = false;
            }
            out.append(c);
        }
        return out.toString().trim();
    }

    private static double roundMm(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0;
        }
        return Math.round(value * 100.0) / 100.0;
    }

    private static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }

    public static String join(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line);
        }
        return builder.toString();
    }

    public static List<String> split(String raw) {
        List<String> lines = new ArrayList<String>();
        if (raw == null || raw.isEmpty()) {
            return lines;
        }
        for (String part : raw.split("\n")) {
            if (!part.trim().isEmpty()) {
                lines.add(part.trim());
            }
        }
        return lines;
    }

    public static Set<String> blocks() {
        return Collections.unmodifiableSet(BLOCKS);
    }

    private static String token(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getQr() {
        return qr;
    }

    public void setQr(String qr) {
        this.qr = qr;
    }

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
    }

    public List<CardElement> getElements() {
        return elements;
    }

    public void setElements(List<CardElement> elements) {
        this.elements = elements;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public Integer getGrid() {
        return grid;
    }

    public void setGrid(Integer grid) {
        this.grid = grid;
    }
}
