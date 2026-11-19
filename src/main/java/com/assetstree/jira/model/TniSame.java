package com.assetstree.jira.model;

import java.util.List;

/**
 * Decides whether a portal sighting is a computer the project already has.
 * The short host ({@code KRUL-IT}) or the private address must agree.
 * A hand-made card wins over one this module already created.
 */
public final class TniSame {
    private TniSame() {
    }

    public static final class Card {
        public final int id;
        public final String name;
        public final boolean location;
        public final String externalKey;
        public final String watch;
        public final String serial;
        public final String inventory;

        public Card(int id, String name, boolean location, String externalKey, String watch) {
            this(id, name, location, externalKey, watch, "", "");
        }

        public Card(int id, String name, boolean location, String externalKey, String watch,
                    String serial, String inventory) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.location = location;
            this.externalKey = externalKey == null ? "" : externalKey;
            this.watch = watch == null ? "" : watch;
            this.serial = serial == null ? "" : serial;
            this.inventory = inventory == null ? "" : inventory;
        }
    }

    /**
     * @param pointed asset chosen on the request, used only when it is the same computer
     * @return the existing asset id, or {@code 0} when a new card is required
     */
    public static int pick(List<Card> cards, int pointed, String host, String address) {
        return pick(cards, pointed, host, address, "", "");
    }

    /**
     * @param serial filled serial number of the incoming card, or empty when it should not be compared
     * @param inventory filled inventory number of the incoming card, or empty when it should not be compared
     */
    public static int pick(List<Card> cards, int pointed, String host, String address, String serial, String inventory) {
        if (cards == null) {
            return 0;
        }
        String station = TniText.shortHost(host);
        String fqdn = TniName.clean(host);
        String ip = TniAddress.local(address);
        int count = 0;
        int only = 0;
        int oldest = 0;
        int hand = 0;
        int hands = 0;
        int pointedHit = 0;
        if (!(station.isEmpty() && fqdn.isEmpty() && ip.isEmpty())) {
            for (int i = 0; i < cards.size(); i++) {
                Card card = cards.get(i);
                if (!same(card, station, fqdn, ip, serial, inventory)) {
                    continue;
                }
                int[] score = score(card, pointed, count, only, oldest, hand, hands, pointedHit);
                count = score[0];
                only = score[1];
                oldest = score[2];
                hand = score[3];
                hands = score[4];
                pointedHit = score[5];
            }
        }
        int chosen = choose(pointedHit, count, only, hands, hand, oldest);
        if (chosen != 0) {
            return chosen;
        }
        return sameName(cards, pointed, host, serial, inventory);
    }

    private static int sameName(List<Card> cards, int pointed, String name, String serial, String inventory) {
        String wanted = plain(name);
        if (wanted.isEmpty()) {
            return 0;
        }
        int count = 0;
        int only = 0;
        int oldest = 0;
        int hand = 0;
        int hands = 0;
        int pointedHit = 0;
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            if (card == null || card.location || card.id == 0 || !wanted.equals(plain(card.name))) {
                continue;
            }
            if (!DuplicateMarks.shares(card.serial, serial, card.inventory, inventory)) {
                continue;
            }
            int[] score = score(card, pointed, count, only, oldest, hand, hands, pointedHit);
            count = score[0];
            only = score[1];
            oldest = score[2];
            hand = score[3];
            hands = score[4];
            pointedHit = score[5];
        }
        return choose(pointedHit, count, only, hands, hand, oldest);
    }

    private static int[] score(Card card, int pointed, int count, int only, int oldest, int hand, int hands, int pointedHit) {
        count++;
        only = card.id;
        if (oldest == 0 || card.id < oldest) {
            oldest = card.id;
        }
        if (!card.externalKey.startsWith("tni:")) {
            hands++;
            if (hand == 0 || card.id < hand) {
                hand = card.id;
            }
        }
        if (card.id == pointed) {
            pointedHit = card.id;
        }
        return new int[] {count, only, oldest, hand, hands, pointedHit};
    }

    private static int choose(int pointedHit, int count, int only, int hands, int hand, int oldest) {
        if (pointedHit != 0) {
            return pointedHit;
        }
        if (count == 1) {
            return only;
        }
        if (hands == 1) {
            return hand;
        }
        if (hand > 0) {
            return hand;
        }
        return oldest;
    }

    private static String plain(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(java.util.Locale.ROOT).replace('ё', 'е');
    }

    /** A card this browser report already owns may be refreshed. A hand-made card may not. */
    public static boolean claimed(String externalKey, String identity) {
        String key = externalKey == null ? "" : externalKey;
        String id = identity == null ? "" : identity;
        return !id.isEmpty() && key.equals("tni:" + id);
    }

    private static boolean same(Card card, String station, String fqdn, String ip, String serial, String inventory) {
        if (card == null || card.location || card.id == 0) {
            return false;
        }
        if (!(named(card.name, station, fqdn) || named(card.watch, station, fqdn) || addressHit(card, ip))) {
            return false;
        }
        return DuplicateMarks.agrees(card.serial, serial, card.inventory, inventory);
    }

    private static boolean addressHit(Card card, String ip) {
        if (ip.isEmpty()) {
            return false;
        }
        return ip.equals(card.name.trim()) || ip.equals(card.watch.trim());
    }

    private static boolean named(String value, String station, String fqdn) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return false;
        }
        if (!station.isEmpty() && (text.equalsIgnoreCase(station) || station.equals(TniText.shortHost(text)))) {
            return true;
        }
        return !fqdn.isEmpty() && text.equalsIgnoreCase(fqdn);
    }
}
