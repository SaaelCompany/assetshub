package com.assetstree.jira.servlet;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * Reads one uploaded file from a multipart body.
 * Plugin servlets often cannot use {@code getPart}: the container either ignores
 * {@code @MultipartConfig} or consumes the stream while rejecting it.
 */
public final class MultipartBody {
    private static final Charset LATIN = Charset.forName("ISO-8859-1");

    private MultipartBody() {
    }

    public static FilePart firstFile(byte[] body, String contentType) {
        if (body == null || body.length == 0 || contentType == null) {
            return null;
        }
        String boundary = boundary(contentType);
        if (boundary.isEmpty()) {
            return null;
        }
        byte[] fence = ("--" + boundary).getBytes(LATIN);
        byte[] separator = join(new byte[] {'\r', '\n'}, fence);
        int cursor = indexOf(body, fence, 0);
        if (cursor < 0) {
            return null;
        }
        cursor += fence.length;
        while (cursor < body.length) {
            if (cursor + 1 < body.length && body[cursor] == '-' && body[cursor + 1] == '-') {
                break;
            }
            if (body[cursor] == '\r') {
                cursor += 2;
            } else if (body[cursor] == '\n') {
                cursor += 1;
            }
            int headerEnd = indexOf(body, new byte[] {'\r', '\n', '\r', '\n'}, cursor);
            int gap = 4;
            if (headerEnd < 0) {
                headerEnd = indexOf(body, new byte[] {'\n', '\n'}, cursor);
                gap = 2;
            }
            if (headerEnd < 0) {
                return null;
            }
            String headers = new String(body, cursor, headerEnd - cursor, LATIN);
            int dataStart = headerEnd + gap;
            int next = indexOf(body, separator, dataStart);
            int dataEnd = next;
            int step = separator.length;
            if (next < 0) {
                next = indexOf(body, fence, dataStart);
                dataEnd = next;
                step = fence.length;
                if (dataEnd >= 2 && body[dataEnd - 2] == '\r' && body[dataEnd - 1] == '\n') {
                    dataEnd -= 2;
                } else if (dataEnd >= 1 && body[dataEnd - 1] == '\n') {
                    dataEnd -= 1;
                }
            }
            if (dataEnd < 0) {
                dataEnd = body.length;
                step = 0;
                next = body.length;
            }
            if (dataEnd < dataStart) {
                return null;
            }
            String fileName = fileName(headers);
            if (fileName != null && !fileName.isEmpty()) {
                byte[] data = new byte[dataEnd - dataStart];
                System.arraycopy(body, dataStart, data, 0, data.length);
                return new FilePart(fileName, contentTypeOf(headers), data);
            }
            cursor = next + step;
        }
        return null;
    }

    public static final class FilePart {
        private final String fileName;
        private final String contentType;
        private final byte[] data;

        private FilePart(String fileName, String contentType, byte[] data) {
            this.fileName = fileName;
            this.contentType = contentType;
            this.data = data;
        }

        public String getFileName() {
            return fileName;
        }

        public String getContentType() {
            return contentType;
        }

        public byte[] getData() {
            return data;
        }
    }

    private static String boundary(String contentType) {
        String lower = contentType.toLowerCase(Locale.ROOT);
        int at = lower.indexOf("boundary=");
        if (at < 0) {
            return "";
        }
        String boundary = contentType.substring(at + "boundary=".length()).trim();
        if (boundary.startsWith("\"") && boundary.endsWith("\"") && boundary.length() > 1) {
            boundary = boundary.substring(1, boundary.length() - 1);
        }
        int semi = boundary.indexOf(';');
        if (semi >= 0) {
            boundary = boundary.substring(0, semi).trim();
        }
        return boundary;
    }

    private static String fileName(String headers) {
        String plain = null;
        String encoded = null;
        String[] lines = headers.split("\r?\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String lower = line.toLowerCase(Locale.ROOT);
            if (!lower.startsWith("content-disposition:")) {
                continue;
            }
            int starred = lower.indexOf("filename*=");
            if (starred >= 0) {
                encoded = token(line.substring(starred + "filename*=".length()).trim());
            }
            int named = lower.indexOf("filename=");
            if (named >= 0 && (starred < 0 || named < starred)) {
                plain = token(line.substring(named + "filename=".length()).trim());
            }
        }
        String chosen = encoded != null ? decodeStar(encoded) : unquote(plain);
        if (chosen == null) {
            return null;
        }
        int slash = Math.max(chosen.lastIndexOf('/'), chosen.lastIndexOf('\\'));
        return slash >= 0 ? chosen.substring(slash + 1) : chosen;
    }

    private static String contentTypeOf(String headers) {
        String[] lines = headers.split("\r?\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.toLowerCase(Locale.ROOT).startsWith("content-type:")) {
                String value = line.substring("content-type:".length()).trim();
                int semi = value.indexOf(';');
                return semi >= 0 ? value.substring(0, semi).trim() : value;
            }
        }
        return "application/octet-stream";
    }

    private static String token(String value) {
        int semi = value.indexOf(';');
        return semi >= 0 ? value.substring(0, semi).trim() : value;
    }

    private static String unquote(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.startsWith("\"") && text.endsWith("\"") && text.length() > 1) {
            text = text.substring(1, text.length() - 1);
        }
        return text;
    }

    private static String decodeStar(String value) {
        String text = unquote(value);
        int mark = text.indexOf("''");
        if (mark >= 0) {
            text = text.substring(mark + 2);
        }
        try {
            return URLDecoder.decode(text, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            return text;
        } catch (IllegalArgumentException ex) {
            return text;
        }
    }

    private static byte[] join(byte[] left, byte[] right) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(left.length + right.length);
        out.write(left, 0, left.length);
        out.write(right, 0, right.length);
        return out.toByteArray();
    }

    static int indexOf(byte[] data, byte[] needle, int from) {
        if (needle.length == 0 || from < 0) {
            return -1;
        }
        for (int i = from; i + needle.length <= data.length; i++) {
            boolean match = true;
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }
}
