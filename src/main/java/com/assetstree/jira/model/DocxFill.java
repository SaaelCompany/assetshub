package com.assetstree.jira.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Fills {{tokens}} in a Word document, including a token split across several text runs. */
public final class DocxFill {
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final int MAX_XML = 8 * 1024 * 1024;
    private static final Pattern PARAGRAPH = Pattern.compile("<w:p\\b[^>]*>.*?</w:p>", Pattern.DOTALL);
    private static final Pattern TEXT = Pattern.compile("<w:t(\\s[^>]*)?>(.*?)</w:t>", Pattern.DOTALL);
    private static final Pattern TOKEN = Pattern.compile("\\{\\{([^{}]{1,80})\\}\\}");

    private DocxFill() {
    }

    public static boolean isDocx(byte[] data) {
        return data != null && data.length > 4 && data[0] == 'P' && data[1] == 'K';
    }

    public static byte[] fill(byte[] docx, Map<String, String> values) throws IOException {
        if (!isDocx(docx)) {
            throw new IOException("docx");
        }
        Map<String, String> ready = new HashMap<String, String>();
        if (values != null) {
            for (Map.Entry<String, String> entry : values.entrySet()) {
                ready.put(normalize(entry.getKey()), entry.getValue() == null ? "" : entry.getValue());
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(docx.length);
        ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(docx));
        ZipOutputStream zip = new ZipOutputStream(out);
        try {
            ZipEntry entry;
            int total = 0;
            while ((entry = in.getNextEntry()) != null) {
                byte[] data = read(in);
                total += data.length;
                if (total > MAX_XML) {
                    throw new IOException("docx");
                }
                if (mergeable(entry.getName())) {
                    data = mergeXml(new String(data, StandardCharsets.UTF_8), ready).getBytes(StandardCharsets.UTF_8);
                }
                zip.putNextEntry(new ZipEntry(entry.getName()));
                zip.write(data);
                zip.closeEntry();
            }
        } finally {
            zip.close();
            in.close();
        }
        return out.toByteArray();
    }

    static boolean mergeable(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return "word/document.xml".equals(lower)
                || (lower.startsWith("word/header") && lower.endsWith(".xml"))
                || (lower.startsWith("word/footer") && lower.endsWith(".xml"))
                || "word/footnotes.xml".equals(lower)
                || "word/endnotes.xml".equals(lower);
    }

    static String mergeXml(String xml, Map<String, String> values) {
        Matcher matcher = PARAGRAPH.matcher(xml);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(mergeParagraph(matcher.group(), values)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    static String mergeParagraph(String paragraph, Map<String, String> values) {
        Matcher texts = TEXT.matcher(paragraph);
        StringBuilder joined = new StringBuilder();
        int count = 0;
        while (texts.find()) {
            joined.append(unescape(texts.group(2)));
            count++;
        }
        if (count == 0 || joined.indexOf("{{") < 0) {
            return paragraph;
        }
        String filled = replaceTokens(joined.toString(), values);
        Matcher again = TEXT.matcher(paragraph);
        StringBuffer out = new StringBuffer();
        boolean first = true;
        while (again.find()) {
            String body = first ? escape(filled) : "";
            first = false;
            String attrs = again.group(1) == null ? "" : again.group(1);
            again.appendReplacement(out, Matcher.quoteReplacement("<w:t" + attrs + ">" + body + "</w:t>"));
        }
        again.appendTail(out);
        return out.toString();
    }

    static String replaceTokens(String text, Map<String, String> values) {
        Matcher matcher = TOKEN.matcher(text);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String key = normalize(matcher.group(1));
            if (values.containsKey(key)) {
                matcher.appendReplacement(out, Matcher.quoteReplacement(values.get(key)));
            } else {
                matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(out);
        return out.toString();
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim().toLowerCase(Locale.ROOT).replace('ё', 'е');
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '/') {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String unescape(String text) {
        String named = text.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&");
        StringBuilder out = new StringBuilder(named.length());
        for (int i = 0; i < named.length(); i++) {
            char current = named.charAt(i);
            if (current == '&' && i + 3 < named.length() && named.charAt(i + 1) == '#') {
                int end = named.indexOf(';', i + 2);
                if (end > i + 2 && end - i <= 10) {
                    String body = named.substring(i + 2, end);
                    int code = characterCode(body);
                    if (code >= 0) {
                        out.append((char) code);
                        i = end;
                        continue;
                    }
                }
            }
            out.append(current);
        }
        return out.toString();
    }

    private static int characterCode(String body) {
        try {
            int code;
            if (body.length() > 1 && (body.charAt(0) == 'x' || body.charAt(0) == 'X')) {
                code = Integer.parseInt(body.substring(1), 16);
            } else {
                code = Integer.parseInt(body);
            }
            if (code >= 0 && code <= 0xFFFF) {
                return code;
            }
        } catch (NumberFormatException ex) {
            return -1;
        }
        return -1;
    }

    private static byte[] read(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            if (out.size() > MAX_XML) {
                throw new IOException("docx");
            }
        }
        return out.toByteArray();
    }
}
