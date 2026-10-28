package com.assetstree.jira.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Turns an attached file into something the equipment card can show. */
public final class FilePreview {
    private static final int TEXT_LIMIT = 8000;
    private static final int ENTRY_LIMIT = 1024 * 1024;

    private FilePreview() {
    }

    /** Content type the browser may render inline, or null when the bytes are not a picture or PDF. */
    public static String inlineType(byte[] data) {
        if (data == null || data.length < 4) {
            return null;
        }
        if (data.length >= 8
                && data[0] == (byte) 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) {
            return "image/png";
        }
        if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (data[0] == 'G' && data[1] == 'I' && data[2] == 'F' && data[3] == '8') {
            return "image/gif";
        }
        if (data.length >= 12
                && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P') {
            return "image/webp";
        }
        if (data[0] == '%' && data[1] == 'P' && data[2] == 'D' && data[3] == 'F') {
            return "application/pdf";
        }
        return null;
    }

    /** Plain-text preview of a document. Empty when the file has no readable text. */
    public static String textOf(String fileName, byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        String ext = extension(fileName);
        if (data.length >= 2 && data[0] == 'P' && data[1] == 'K') {
            String office = officeText(data);
            if (office != null) {
                return clip(office);
            }
        }
        if ("rtf".equals(ext)) {
            return clip(rtfText(data));
        }
        if (textExtension(ext)) {
            return clip(decode(data));
        }
        return "";
    }

    private static String officeText(byte[] data) {
        byte[] document = entry(data, "word/document.xml");
        if (document != null) {
            return docxText(new String(document, StandardCharsets.UTF_8));
        }
        byte[] sheet = entry(data, "xl/worksheets/sheet1.xml");
        if (sheet == null) {
            return null;
        }
        byte[] shared = entry(data, "xl/sharedStrings.xml");
        return xlsxText(new String(sheet, StandardCharsets.UTF_8),
                shared == null ? "" : new String(shared, StandardCharsets.UTF_8));
    }

    private static String docxText(String xml) {
        String[] paragraphs = xml.split("</w:p>");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < paragraphs.length; i++) {
            String line = tags(paragraphs[i], "w:t").trim();
            if (line.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(line);
        }
        return out.toString();
    }

    private static String xlsxText(String sheet, String sharedXml) {
        List<String> strings = sharedStrings(sharedXml);
        StringBuilder table = new StringBuilder();
        int rows = 0;
        int cursor = 0;
        while (rows < 40) {
            int start = sheet.indexOf("<row", cursor);
            if (start < 0) {
                break;
            }
            int end = sheet.indexOf("</row>", start);
            if (end < 0) {
                break;
            }
            String row = sheet.substring(start, end);
            StringBuilder line = new StringBuilder();
            int cell = 0;
            while (true) {
                int mark = row.indexOf("<c", cell);
                if (mark < 0) {
                    break;
                }
                int close = row.indexOf("</c>", mark);
                if (close < 0) {
                    break;
                }
                String piece = row.substring(mark, close);
                String value = tags(piece, "v").trim();
                if (piece.contains("t=\"s\"") || piece.contains("t='s'")) {
                    value = sharedAt(strings, value);
                }
                if (line.length() > 0) {
                    line.append('\t');
                }
                line.append(value);
                cell = close + 4;
            }
            if (line.length() > 0) {
                if (table.length() > 0) {
                    table.append('\n');
                }
                table.append(line);
                rows++;
            }
            cursor = end + 6;
        }
        return table.toString();
    }

    private static List<String> sharedStrings(String xml) {
        List<String> values = new ArrayList<String>();
        int cursor = 0;
        while (cursor < xml.length()) {
            int start = xml.indexOf("<si", cursor);
            if (start < 0) {
                break;
            }
            int end = xml.indexOf("</si>", start);
            if (end < 0) {
                break;
            }
            values.add(tags(xml.substring(start, end), "t"));
            cursor = end + 5;
        }
        return values;
    }

    private static String sharedAt(List<String> strings, String value) {
        try {
            int index = Integer.parseInt(value.trim());
            if (index >= 0 && index < strings.size()) {
                return strings.get(index);
            }
        } catch (NumberFormatException ex) {
            return "";
        }
        return "";
    }

    private static String tags(String xml, String tag) {
        StringBuilder out = new StringBuilder();
        String open = "<" + tag;
        String close = "</" + tag + ">";
        int cursor = 0;
        while (cursor < xml.length()) {
            int start = xml.indexOf(open, cursor);
            if (start < 0) {
                break;
            }
            int gt = xml.indexOf('>', start);
            if (gt < 0) {
                break;
            }
            int end = xml.indexOf(close, gt);
            if (end < 0) {
                break;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(unescape(xml.substring(gt + 1, end)));
            cursor = end + close.length();
        }
        return out.toString();
    }

    private static String rtfText(byte[] data) {
        String text = decode(data);
        text = text.replaceAll("\\\\'[0-9a-fA-F]{2}", " ");
        text = text.replaceAll("\\\\[a-zA-Z]+-?\\d* ?", "");
        return text.replace("{", "").replace("}", "");
    }

    private static String unescape(String value) {
        String text = value.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&apos;", "'");
        StringBuilder out = new StringBuilder();
        int index = 0;
        while (index < text.length()) {
            if (text.startsWith("&#", index)) {
                int end = text.indexOf(';', index);
                if (end > index + 2 && end - index < 12) {
                    String body = text.substring(index + 2, end);
                    try {
                        int code = body.startsWith("x") || body.startsWith("X")
                                ? Integer.parseInt(body.substring(1), 16)
                                : Integer.parseInt(body);
                        if (code > 0 && code <= 0x10FFFF) {
                            out.append(new String(Character.toChars(code)));
                            index = end + 1;
                            continue;
                        }
                    } catch (NumberFormatException ex) {
                        out.append(text.charAt(index));
                        index++;
                        continue;
                    }
                }
            }
            out.append(text.charAt(index));
            index++;
        }
        return out.toString();
    }

    private static byte[] entry(byte[] data, String name) {
        ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data));
        try {
            ZipEntry item;
            while ((item = zip.getNextEntry()) != null) {
                if (!name.equals(item.getName())) {
                    continue;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int total = 0;
                int read;
                while ((read = zip.read(buffer)) >= 0) {
                    total += read;
                    if (total > ENTRY_LIMIT) {
                        return null;
                    }
                    out.write(buffer, 0, read);
                }
                return out.toByteArray();
            }
        } catch (IOException ex) {
            return null;
        } finally {
            try {
                zip.close();
            } catch (IOException ignored) {
                // The bytes already read stay usable.
            }
        }
        return null;
    }

    private static String decode(byte[] data) {
        int length = Math.min(data.length, TEXT_LIMIT * 4);
        int start = 0;
        if (length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF) {
            start = 3;
        }
        return new String(data, start, length - start, StandardCharsets.UTF_8);
    }

    private static String clip(String value) {
        String text = value == null ? "" : value.replace('\r', '\n').trim();
        if (text.length() > TEXT_LIMIT) {
            text = text.substring(0, TEXT_LIMIT);
        }
        return text;
    }

    private static boolean textExtension(String ext) {
        return "txt".equals(ext) || "csv".equals(ext) || "log".equals(ext) || "md".equals(ext)
                || "json".equals(ext) || "xml".equals(ext) || "html".equals(ext) || "htm".equals(ext);
    }

    private static String extension(String fileName) {
        String name = fileName == null ? "" : fileName.trim().toLowerCase(java.util.Locale.ROOT);
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1);
    }
}
