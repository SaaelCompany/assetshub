package com.assetstree.jira.model;

/** Readable name and description for an asset created from a portal device. */
public final class TniText {
    private TniText() {
    }

    public static String name(String kindLabel, String person, String address) {
        String who = clean(person);
        if (who.isEmpty()) {
            who = clean(address);
        }
        String title = (clean(kindLabel) + " " + who).trim();
        if (title.length() > 255) {
            title = title.substring(0, 255).trim();
        }
        return title.isEmpty() ? "TNI" : title;
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

    private static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace('\n', ' ').replace('\r', ' ').trim();
    }
}
