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

        public Card(int id, String name, boolean location, String externalKey, String watch) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.location = location;
            this.externalKey = externalKey == null ? "" : externalKey;
            this.watch = watch == null ? "" : watch;
        }
    }

    /**
     * @param pointed asset chosen on the request, used only when it is the same computer
     * @return the existing asset id, or {@code 0} when a new card is required
     */
    public static int pick(List<Card> cards, int pointed, String host, String address) {
        String station = TniText.shortHost(host);
        String fqdn = TniName.clean(host);
        String ip = TniAddress.local(address);
        if (station.isEmpty() && fqdn.isEmpty() && ip.isEmpty()) {
            return 0;
        }
        int count = 0;
        int only = 0;
        int oldest = 0;
        int hand = 0;
        int hands = 0;
        int pointedHit = 0;
        if (cards == null) {
            return 0;
        }
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            if (!same(card, station, fqdn, ip)) {
                continue;
            }
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
        }
        if (pointedHit > 0) {
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

    /** A card this browser report already owns may be refreshed. A hand-made card may not. */
    public static boolean claimed(String externalKey, String identity) {
        String key = externalKey == null ? "" : externalKey;
        String id = identity == null ? "" : identity;
        return !id.isEmpty() && key.equals("tni:" + id);
    }

    private static boolean same(Card card, String station, String fqdn, String ip) {
        if (card == null || card.location || card.id <= 0) {
            return false;
        }
        return named(card.name, station, fqdn) || named(card.watch, station, fqdn) || addressHit(card, ip);
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
