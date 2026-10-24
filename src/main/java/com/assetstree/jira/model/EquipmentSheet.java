package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Equipment travels as CSV so Excel can open it. The file uses a semicolon,
 * which is what Excel uses in Russian locales, and a UTF-8 mark at the start.
 * A file saved with commas is read as well.
 */
public final class EquipmentSheet {
    public static final char DELIMITER = ';';

    private EquipmentSheet() {
    }

    public static final class Sheet {
        private final String name;
        private final List<String> headers;
        private final List<Record> records;

        Sheet(List<String> headers, List<Record> records) {
            this("", headers, records);
        }

        Sheet(String name, List<String> headers, List<Record> records) {
            this.name = name == null ? "" : name;
            this.headers = headers;
            this.records = records;
        }

        public String getName() {
            return name;
        }

        public List<String> getHeaders() {
            return headers;
        }

        public List<Record> getRecords() {
            return records;
        }
    }

    public static final class Record {
        private final int row;
        private final List<String> cells;

        Record(int row, List<String> cells) {
            this.row = row;
            this.cells = cells;
        }

        public int getRow() {
            return row;
        }

        int width() {
            return cells.size();
        }

        public String cell(int index) {
            if (index < 0 || index >= cells.size() || cells.get(index) == null) {
                return "";
            }
            return cells.get(index);
        }
    }

    public static String write(List<String> headers, List<List<String>> rows) {
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        writeLine(builder, headers);
        if (rows != null) {
            for (List<String> row : rows) {
                writeLine(builder, row);
            }
        }
        return builder.toString();
    }

    public static Sheet read(String text) {
        String source = text == null ? "" : text;
        if (!source.isEmpty() && source.charAt(0) == '\uFEFF') {
            source = source.substring(1);
        }
        source = source.replace("\r\n", "\n").replace('\r', '\n');
        char delimiter = detect(source);
        List<Record> records = new ArrayList<Record>();
        List<String> headers = new ArrayList<String>();
        int line = 1;
        int index = 0;
        while (index < source.length()) {
            int startLine = line;
            List<String> cells = new ArrayList<String>();
            boolean any = false;
            while (index < source.length()) {
                StringBuilder cell = new StringBuilder();
                boolean quoted = false;
                if (source.charAt(index) == '"') {
                    quoted = true;
                    index++;
                }
                while (index < source.length()) {
                    char current = source.charAt(index);
                    if (quoted && current == '"') {
                        if (index + 1 < source.length() && source.charAt(index + 1) == '"') {
                            cell.append('"');
                            index += 2;
                            continue;
                        }
                        quoted = false;
                        index++;
                        continue;
                    }
                    if (!quoted && (current == delimiter || current == '\n')) {
                        break;
                    }
                    if (current == '\n') {
                        line++;
                    }
                    cell.append(current);
                    index++;
                }
                cells.add(cell.toString());
                any = true;
                if (index >= source.length()) {
                    break;
                }
                char stop = source.charAt(index);
                index++;
                if (stop == '\n') {
                    line++;
                    break;
                }
            }
            if (!any) {
                break;
            }
            boolean blank = true;
            for (String cell : cells) {
                if (cell != null && !cell.trim().isEmpty()) {
                    blank = false;
                    break;
                }
            }
            if (blank) {
                continue;
            }
            if (headers.isEmpty()) {
                for (String cell : cells) {
                    headers.add(cell == null ? "" : cell.trim());
                }
            } else {
                records.add(new Record(startLine, cells));
            }
        }
        return new Sheet(headers, records);
    }

    private static char detect(String source) {
        boolean quoted = false;
        int semicolons = 0;
        int commas = 0;
        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            if (current == '"') {
                if (quoted && i + 1 < source.length() && source.charAt(i + 1) == '"') {
                    i++;
                    continue;
                }
                quoted = !quoted;
                continue;
            }
            if (!quoted && current == '\n') {
                break;
            }
            if (!quoted && current == ';') {
                semicolons++;
            } else if (!quoted && current == ',') {
                commas++;
            }
        }
        if (semicolons > 0 && semicolons >= commas) {
            return ';';
        }
        return ',';
    }

    private static void writeLine(StringBuilder builder, List<String> cells) {
        if (cells == null) {
            builder.append('\n');
            return;
        }
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                builder.append(DELIMITER);
            }
            builder.append(quote(cells.get(i)));
        }
        builder.append('\n');
    }

    private static String quote(String value) {
        String text = value == null ? "" : value;
        boolean wrap = text.indexOf(DELIMITER) >= 0 || text.indexOf('"') >= 0 || text.indexOf('\n') >= 0
                || text.indexOf('\r') >= 0 || (!text.isEmpty() && (text.charAt(0) == ' ' || text.charAt(text.length() - 1) == ' '));
        if (!wrap) {
            return text;
        }
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}
