package com.assetstree.jira.model;

import java.util.Locale;

/** Readable name and description for an asset created from a portal device. */
public final class TniText {
    private TniText() {
    }

    public static String name(String kindLabel, String person, String address) {
        return name(kindLabel, person, address, "");
    }

    /**
     * A network name such as {@code krul-it.budzdorov.ru} becomes the object name {@code krul-it}.
     * Without one, the card keeps the kind and the person.
     */
    public static String name(String kindLabel, String person, String address, String host) {
        String station = shortHost(host);
        if (!station.isEmpty()) {
            return clip(station);
        }
        String who = clean(person);
        if (who.isEmpty()) {
            who = clean(address);
        }
        String title = (clean(kindLabel) + " " + who).trim();
        return title.isEmpty() ? "TNI" : clip(title);
    }

    /** First label of a DNS name, in lower case. {@code krul-it.budzdorov.ru} is {@code krul-it}. */
    public static String shortHost(String host) {
        String clean = TniName.clean(host);
        if (clean.isEmpty()) {
            return "";
        }
        int dot = clean.indexOf('.');
        String label = dot < 0 ? clean : clean.substring(0, dot);
        if (label.isEmpty() || label.length() > 63) {
            return "";
        }
        return label.toLowerCase(Locale.ROOT);
    }

    /**
     * Keeps a name a person typed. Replaces the automatic kind-and-person title, the stored
     * automatic title, or the full DNS name with the short host.
     */
    public static String preferred(String current, String stamped, String kindLabel, String person,
                                   String address, String host) {
        String station = shortHost(host);
        String now = clean(current);
        if (station.isEmpty()) {
            return now.isEmpty() ? name(kindLabel, person, address) : now;
        }
        if (now.isEmpty() || now.equalsIgnoreCase(station)) {
            return station;
        }
        String stamp = clean(stamped);
        String auto = name(kindLabel, person, address);
        String fqdn = TniName.clean(host);
        if (now.equals(stamp) || now.equals(auto) || (!fqdn.isEmpty() && now.equalsIgnoreCase(fqdn))) {
            return station;
        }
        return now;
    }

    /** An empty card, or one this module already wrote, can be refreshed. */
    public static boolean owned(String description) {
        if (description == null) {
            return true;
        }
        String text = description.trim();
        return text.isEmpty() || text.startsWith("TNI");
    }

    public static String describe(TniFacts facts, String intro, String seenLabel, String hostLabel, String addressLabel,
                                  String systemLabel, String browserLabel, String screenLabel, String coresLabel,
                                  String memoryLabel, String gpuLabel, String requestLabel, String issueKey) {
        TniFacts source = facts == null ? TniFacts.from(null) : facts;
        StringBuilder body = new StringBuilder();
        String head = clean(intro);
        if (head.isEmpty()) {
            head = "TNI.";
        }
        if (!head.startsWith("TNI")) {
            head = "TNI. " + head;
        }
        body.append(head);
        line(body, seenLabel, source.seen);
        line(body, hostLabel, source.host);
        if (!clean(source.address).equals(clean(source.seen))) {
            line(body, addressLabel, source.address);
        }
        line(body, systemLabel, source.os);
        line(body, browserLabel, source.browser);
        line(body, screenLabel, source.screen);
        line(body, coresLabel, source.cores);
        line(body, memoryLabel, source.memory);
        line(body, gpuLabel, source.gpu);
        line(body, requestLabel, issueKey);
        return body.toString();
    }

    private static void line(StringBuilder body, String label, String value) {
        String item = clean(value);
        if (item.isEmpty()) {
            return;
        }
        body.append('\n');
        String caption = clean(label);
        if (!caption.isEmpty()) {
            body.append(caption).append(": ");
        }
        body.append(item);
    }

    private static String clip(String title) {
        if (title.length() > 255) {
            return title.substring(0, 255).trim();
        }
        return title;
    }

    private static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace('\n', ' ').replace('\r', ' ').trim();
    }
}
