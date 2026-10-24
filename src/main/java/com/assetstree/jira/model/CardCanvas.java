package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Stores free-form card blocks as a JSON array. A blank or unreadable value means the
 * project still uses the older line layout.
 */
public final class CardCanvas {
    private CardCanvas() {
    }

    public static String write(List<CardElement> elements) {
        StringBuilder out = new StringBuilder();
        out.append('[');
        if (elements != null) {
            for (int i = 0; i < elements.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                writeElement(out, elements.get(i));
            }
        }
        out.append(']');
        return out.toString();
    }

    public static List<CardElement> read(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        Object parsed = parse(raw.trim());
        if (!(parsed instanceof List)) {
            return null;
        }
        List<CardElement> elements = new ArrayList<CardElement>();
        for (Object item : (List<?>) parsed) {
            if (!(item instanceof Map)) {
                continue;
            }
            elements.add(elementOf((Map<?, ?>) item));
        }
        return elements;
    }

    private static void writeElement(StringBuilder out, CardElement element) {
        out.append('{');
        field(out, "id", element.getId(), true);
        field(out, "kind", element.getKind(), false);
        field(out, "token", element.getToken(), false);
        field(out, "text", element.getText(), false);
        field(out, "src", element.getSrc(), false);
        number(out, "x", element.getX());
        number(out, "y", element.getY());
        number(out, "w", element.getW());
        number(out, "h", element.getH());
        out.append(",\"size\":").append(element.getSize());
        field(out, "color", element.getColor(), false);
        field(out, "font", element.getFont(), false);
        out.append(",\"bold\":").append(element.getBold() ? "true" : "false");
        field(out, "align", element.getAlign(), false);
        out.append('}');
    }

    private static void field(StringBuilder out, String name, String value, boolean first) {
        if (!first) {
            out.append(',');
        }
        out.append('"').append(name).append("\":");
        quote(out, value == null ? "" : value);
    }

    private static void number(StringBuilder out, String name, double value) {
        out.append(",\"").append(name).append("\":");
        out.append(String.format(Locale.US, "%.2f", value));
    }

    private static void quote(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        out.append(String.format(Locale.US, "\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                    break;
            }
        }
        out.append('"');
    }

    private static CardElement elementOf(Map<?, ?> map) {
        CardElement element = new CardElement();
        element.setId(text(map.get("id")));
        element.setKind(text(map.get("kind")));
        element.setToken(text(map.get("token")));
        element.setText(text(map.get("text")));
        element.setSrc(text(map.get("src")));
        element.setX(number(map.get("x")));
        element.setY(number(map.get("y")));
        element.setW(number(map.get("w")));
        element.setH(number(map.get("h")));
        element.setSize((int) Math.round(number(map.get("size"))));
        element.setColor(text(map.get("color")));
        element.setFont(text(map.get("font")));
        element.setBold(Boolean.TRUE.equals(map.get("bold")));
        element.setAlign(text(map.get("align")));
        return element;
    }

    private static String text(Object value) {
        return value instanceof String ? (String) value : "";
    }

    private static double number(Object value) {
        return value instanceof Double ? ((Double) value).doubleValue() : 0;
    }

    static Object parse(String raw) {
        Parser parser = new Parser(raw);
        try {
            Object value = parser.value();
            parser.skip();
            if (parser.index != raw.length()) {
                return null;
            }
            return value;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static final class Parser {
        private final String raw;
        private int index;

        private Parser(String raw) {
            this.raw = raw;
        }

        private Object value() {
            skip();
            if (index >= raw.length()) {
                throw new IllegalArgumentException("end");
            }
            char c = raw.charAt(index);
            if (c == '"') {
                return string();
            }
            if (c == '{') {
                return object();
            }
            if (c == '[') {
                return array();
            }
            if (c == 't' || c == 'f') {
                return bool();
            }
            if (c == 'n') {
                literal("null");
                return null;
            }
            if (c == '-' || (c >= '0' && c <= '9')) {
                return number();
            }
            throw new IllegalArgumentException("value");
        }

        private Map<String, Object> object() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            skip();
            if (peek('}')) {
                index++;
                return map;
            }
            while (index < raw.length()) {
                skip();
                String key = string();
                skip();
                expect(':');
                map.put(key, value());
                skip();
                if (peek('}')) {
                    index++;
                    return map;
                }
                expect(',');
            }
            throw new IllegalArgumentException("object");
        }

        private List<Object> array() {
            expect('[');
            List<Object> list = new ArrayList<Object>();
            skip();
            if (peek(']')) {
                index++;
                return list;
            }
            while (index < raw.length()) {
                list.add(value());
                skip();
                if (peek(']')) {
                    index++;
                    return list;
                }
                expect(',');
            }
            throw new IllegalArgumentException("array");
        }

        private String string() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (index < raw.length()) {
                char c = raw.charAt(index++);
                if (c == '"') {
                    return out.toString();
                }
                if (c == '\\') {
                    if (index >= raw.length()) {
                        throw new IllegalArgumentException("escape");
                    }
                    char mark = raw.charAt(index++);
                    switch (mark) {
                        case '"':
                        case '\\':
                        case '/':
                            out.append(mark);
                            break;
                        case 'b':
                            out.append('\b');
                            break;
                        case 'f':
                            out.append('\f');
                            break;
                        case 'n':
                            out.append('\n');
                            break;
                        case 'r':
                            out.append('\r');
                            break;
                        case 't':
                            out.append('\t');
                            break;
                        case 'u':
                            if (index + 4 > raw.length()) {
                                throw new IllegalArgumentException("unicode");
                            }
                            int code = Integer.parseInt(raw.substring(index, index + 4), 16);
                            out.append((char) code);
                            index += 4;
                            break;
                        default:
                            throw new IllegalArgumentException("escape");
                    }
                } else if (c >= 0x20) {
                    out.append(c);
                } else {
                    throw new IllegalArgumentException("control");
                }
            }
            throw new IllegalArgumentException("string");
        }

        private Boolean bool() {
            if (raw.startsWith("true", index)) {
                index += 4;
                return Boolean.TRUE;
            }
            if (raw.startsWith("false", index)) {
                index += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("bool");
        }

        private void literal(String expected) {
            if (!raw.startsWith(expected, index)) {
                throw new IllegalArgumentException(expected);
            }
            index += expected.length();
        }

        private Double number() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            if (index >= raw.length() || raw.charAt(index) < '0' || raw.charAt(index) > '9') {
                throw new IllegalArgumentException("number");
            }
            while (index < raw.length() && raw.charAt(index) >= '0' && raw.charAt(index) <= '9') {
                index++;
            }
            if (peek('.')) {
                index++;
                if (index >= raw.length() || raw.charAt(index) < '0' || raw.charAt(index) > '9') {
                    throw new IllegalArgumentException("fraction");
                }
                while (index < raw.length() && raw.charAt(index) >= '0' && raw.charAt(index) <= '9') {
                    index++;
                }
            }
            return Double.valueOf(raw.substring(start, index));
        }

        private void skip() {
            while (index < raw.length()) {
                char c = raw.charAt(index);
                if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
                    return;
                }
                index++;
            }
        }

        private boolean peek(char expected) {
            return index < raw.length() && raw.charAt(index) == expected;
        }

        private void expect(char expected) {
            if (!peek(expected)) {
                throw new IllegalArgumentException(String.valueOf(expected));
            }
            index++;
        }
    }
}
