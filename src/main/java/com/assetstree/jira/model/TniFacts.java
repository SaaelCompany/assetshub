package com.assetstree.jira.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;

/** What the browser could see about the device that opened the portal form. */
public final class TniFacts {
    public final String kind;
    public final String address;
    public final String os;
    public final String browser;
    public final String screen;
    public final String cores;
    public final String memory;
    public final String gpu;

    private TniFacts(String kind, String address, String os, String browser, String screen,
                     String cores, String memory, String gpu) {
        this.kind = kind;
        this.address = address;
        this.os = os;
        this.browser = browser;
        this.screen = screen;
        this.cores = cores;
        this.memory = memory;
        this.gpu = gpu;
    }

    public static TniFacts from(TniSightDraft draft) {
        TniSightDraft source = draft == null ? new TniSightDraft() : draft;
        String agent = clip(source.getUa(), 400);
        String platform = clip(source.getPlatform(), 80);
        String screen = screen(source.getScreen());
        return new TniFacts(
                kind(agent, platform, source.getTouch(), source.getBattery(), screen),
                TniAddress.choose(source.getAddresses()),
                operatingSystem(agent, platform),
                browser(agent),
                screen,
                count(source.getCores(), 1, 256),
                memory(source.getMemory()),
                clip(oneLine(source.getGpu()), 120));
    }

    public static TniFacts parse(String stored) {
        String os = "";
        String browser = "";
        String screen = "";
        String cores = "";
        String memory = "";
        String gpu = "";
        String kind = TniPolicy.COMPUTER;
        String address = "";
        if (stored != null) {
            String[] lines = stored.split("\n");
            for (String line : lines) {
                int mark = line.indexOf('=');
                if (mark <= 0) {
                    continue;
                }
                String key = line.substring(0, mark);
                String value = clip(line.substring(mark + 1), 120);
                if ("os".equals(key)) {
                    os = value;
                } else if ("browser".equals(key)) {
                    browser = value;
                } else if ("screen".equals(key)) {
                    screen = screen(value);
                } else if ("cores".equals(key)) {
                    cores = value;
                } else if ("memory".equals(key)) {
                    memory = value;
                } else if ("gpu".equals(key)) {
                    gpu = value;
                } else if ("kind".equals(key)) {
                    kind = knownKind(value);
                } else if ("address".equals(key)) {
                    address = TniAddress.allowed(value) ? value : "";
                }
            }
        }
        return new TniFacts(kind, address, os, browser, screen, cores, memory, gpu);
    }

    public String summary() {
        StringBuilder body = new StringBuilder();
        pair(body, "kind", kind);
        pair(body, "address", address);
        pair(body, "os", os);
        pair(body, "browser", browser);
        pair(body, "screen", screen);
        pair(body, "cores", cores);
        pair(body, "memory", memory);
        pair(body, "gpu", gpu);
        return body.toString();
    }

    public static String identity(String projectKey, String userKey, TniFacts facts) {
        TniFacts source = facts == null ? from(null) : facts;
        String tail = source.address.isEmpty()
                ? source.kind + "\n" + source.screen + "\n" + source.os
                : source.address;
        String material = clip(projectKey, 80) + "\n" + clip(userKey, 255) + "\n" + tail;
        return sha(material).substring(0, 32);
    }

    public static String kind(String agent, String platform, Integer touch, Boolean battery, String screen) {
        String blob = (agent + " " + platform).toLowerCase(Locale.ROOT);
        boolean phone = blob.contains("iphone") || blob.contains("ipod") || blob.contains("windows phone")
                || (blob.contains("android") && blob.contains("mobile"));
        if (phone) {
            return TniPolicy.PHONE;
        }
        boolean tablet = blob.contains("ipad") || blob.contains("tablet")
                || (blob.contains("android") && !blob.contains("mobile"));
        if (tablet) {
            return TniPolicy.TABLET;
        }
        if (Boolean.TRUE.equals(battery)) {
            return TniPolicy.LAPTOP;
        }
        int width = screenWidth(screen);
        int points = touch == null ? 0 : touch.intValue();
        if (points > 0 && width > 0 && width < 900) {
            return TniPolicy.PHONE;
        }
        return TniPolicy.COMPUTER;
    }

    private static String knownKind(String value) {
        if (TniPolicy.LAPTOP.equals(value) || TniPolicy.PHONE.equals(value) || TniPolicy.TABLET.equals(value)) {
            return value;
        }
        return TniPolicy.COMPUTER;
    }

    static String operatingSystem(String agent, String platform) {
        String blob = (agent + " " + platform).toLowerCase(Locale.ROOT);
        if (blob.contains("iphone") || blob.contains("ipad") || blob.contains("ios")) {
            return "iOS";
        }
        if (blob.contains("android")) {
            return "Android";
        }
        if (blob.contains("cros")) {
            return "ChromeOS";
        }
        if (blob.contains("windows")) {
            return "Windows";
        }
        if (blob.contains("mac")) {
            return "macOS";
        }
        if (blob.contains("linux")) {
            return "Linux";
        }
        return "";
    }

    static String browser(String agent) {
        String blob = agent == null ? "" : agent;
        if (blob.contains("Edg/")) {
            return "Edge";
        }
        if (blob.contains("Chrome/")) {
            return "Chrome";
        }
        if (blob.contains("Firefox/")) {
            return "Firefox";
        }
        if (blob.contains("Safari/") && !blob.contains("Chrome/")) {
            return "Safari";
        }
        if (blob.contains("Trident") || blob.contains("MSIE")) {
            return "Internet Explorer";
        }
        return "";
    }

    private static void pair(StringBuilder body, String key, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (body.length() > 0) {
            body.append('\n');
        }
        body.append(key).append('=').append(value);
    }

    private static String screen(String raw) {
        String text = clip(oneLine(raw), 20);
        int width = screenWidth(text);
        if (width <= 0) {
            return "";
        }
        int mark = text.indexOf('x');
        if (mark < 0 || mark == text.length() - 1) {
            return "";
        }
        String height = text.substring(mark + 1);
        if (!digits(height) || height.length() > 5) {
            return "";
        }
        int high = Integer.parseInt(height);
        if (high <= 0 || high > 10000) {
            return "";
        }
        return width + "x" + high;
    }

    private static int screenWidth(String screen) {
        if (screen == null) {
            return 0;
        }
        int mark = screen.indexOf('x');
        if (mark <= 0) {
            return 0;
        }
        String width = screen.substring(0, mark);
        if (!digits(width)) {
            return 0;
        }
        try {
            int value = Integer.parseInt(width);
            return value > 0 && value <= 10000 ? value : 0;
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String count(Integer raw, int min, int max) {
        if (raw == null) {
            return "";
        }
        int value = raw.intValue();
        if (value < min || value > max) {
            return "";
        }
        return String.valueOf(value);
    }

    private static String memory(Double raw) {
        if (raw == null) {
            return "";
        }
        double value = raw.doubleValue();
        if (value < 0.25d || value > 512d) {
            return "";
        }
        if (Math.abs(value - Math.rint(value)) < 0.01d) {
            return String.valueOf((int) Math.rint(value));
        }
        return String.valueOf(value);
    }

    private static String oneLine(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static String clip(String raw, int max) {
        String text = raw == null ? "" : raw.trim();
        if (text.length() > max) {
            return text.substring(0, max);
        }
        return text;
    }

    private static boolean digits(String part) {
        if (part == null || part.isEmpty()) {
            return false;
        }
        for (int i = 0; i < part.length(); i++) {
            char ch = part.charAt(i);
            if (ch < '0' || ch > '9') {
                return false;
            }
        }
        return true;
    }

    private static String sha(String material) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                hex.append(String.format(Locale.ROOT, "%02x", Integer.valueOf(item & 0xff)));
            }
            return hex.toString();
        } catch (Exception ex) {
            return "00000000000000000000000000000000";
        }
    }

    /** Kept so a caller can pass the raw list without building a draft. */
    public static String chooseAddress(List<String> addresses) {
        return TniAddress.choose(addresses);
    }
}
