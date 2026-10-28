package com.assetstree.jira.service;

import com.assetstree.jira.model.AssetValidator;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Keeps the small set of tags the comment editor writes. Everything else is dropped. */
public final class CommentHtml {
    private static final Pattern BLOCK = Pattern.compile("(?is)<\\s*(script|style)[^>]*>.*?<\\s*/\\s*\\1\\s*>");
    private static final Pattern COMMENT = Pattern.compile("(?is)<!--.*?-->");
    private static final Pattern TAG = Pattern.compile("(?is)<\\s*(/?)\\s*([a-z0-9]+)([^>]*)>");
    private static final Pattern HREF = Pattern.compile("(?i)href\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))");

    private CommentHtml() {
    }

    public static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.replace("\u0000", "").trim();
        if (text.isEmpty() || text.indexOf('<') < 0) {
            return clip(text);
        }
        text = BLOCK.matcher(text).replaceAll("");
        text = COMMENT.matcher(text).replaceAll("");
        Matcher matcher = TAG.matcher(text);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String slash = matcher.group(1);
            String name = matcher.group(2).toLowerCase(Locale.ROOT);
            String attrs = matcher.group(3) == null ? "" : matcher.group(3);
            String replacement = "";
            if ("br".equals(name)) {
                replacement = "<br>";
            } else if (allowed(name)) {
                if (slash != null && slash.length() > 0) {
                    replacement = "</" + name + ">";
                } else if ("a".equals(name)) {
                    String href = href(attrs);
                    replacement = href.isEmpty() ? "" : "<a href=\"" + href + "\">";
                } else if ("span".equals(name)) {
                    String color = colorOf(attrs);
                    replacement = color.isEmpty() ? "" : "<span style=\"color:" + color + "\">";
                } else {
                    replacement = "<" + name + ">";
                }
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return clip(out.toString().trim());
    }

    private static boolean allowed(String name) {
        return "p".equals(name) || "h2".equals(name) || "h3".equals(name) || "strong".equals(name)
                || "b".equals(name) || "em".equals(name) || "i".equals(name) || "u".equals(name)
                || "ul".equals(name) || "ol".equals(name) || "li".equals(name) || "a".equals(name)
                || "span".equals(name);
    }

    private static String colorOf(String attrs) {
        Matcher matcher = Pattern.compile("(?i)color\\s*:\\s*(#[0-9a-f]{3,8}|rgb\\(\\s*\\d{1,3}\\s*,\\s*\\d{1,3}\\s*,\\s*\\d{1,3}\\s*\\))").matcher(attrs);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1).replace(" ", "");
    }

    private static String href(String attrs) {
        Matcher matcher = HREF.matcher(attrs);
        if (!matcher.find()) {
            return "";
        }
        String value = matcher.group(1) != null ? matcher.group(1) : (matcher.group(2) != null ? matcher.group(2) : matcher.group(3));
        if (value == null) {
            return "";
        }
        value = value.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("https://") && !lower.startsWith("http://") && !lower.startsWith("mailto:")) {
            return "";
        }
        return value.replace("&", "&amp;").replace("\"", "").replace("<", "").replace(">", "");
    }

    private static String clip(String text) {
        if (text.length() <= AssetValidator.MAX_DESCRIPTION) {
            return text;
        }
        return text.substring(0, AssetValidator.MAX_DESCRIPTION);
    }
}
