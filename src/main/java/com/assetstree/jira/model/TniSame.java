package com.assetstree.jira.model;

import java.util.List;
import java.util.Set;

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
        public String serial;
        public String inventory;
        public final String typeKey;

        public Card(int id, String name, boolean location, String externalKey, String watch) {
            this(id, name, location, externalKey, watch, "", "");
        }

        public Card(int id, String name, boolean location, String externalKey, String watch,
                    String serial, String inventory) {
            this(id, name, location, externalKey, watch, serial, inventory, "");
        }

        public Card(int id, String name, boolean location, String externalKey, String watch,
                    String serial, String inventory, String typeKey) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.location = location;
            this.externalKey = externalKey == null ? "" : externalKey;
            this.watch = watch == null ? "" : watch;
            this.serial = serial == null ? "" : serial;
            this.inventory = inventory == null ? "" : inventory;
            this.typeKey = typeKey == null ? "" : typeKey;
        }

        /** Keeps a number learned from the row that claimed this card, so the next row with another number stays separate. */
        public void remember(String serial, String inventory) {
            if (this.serial.isEmpty() && serial != null && !serial.trim().isEmpty()) {
                this.serial = serial.trim();
            }
            if (this.inventory.isEmpty() && inventory != null && !inventory.trim().isEmpty()) {
                this.inventory = inventory.trim();
            }
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
        return pick(cards, pointed, host, address, serial, inventory, null);
    }

    /**
     * @param taken cards already claimed by an earlier row; a later row takes the next free card
     *        with the same name, and when every match is claimed the claimed id is returned
     */
    public static int pick(List<Card> cards, int pointed, String host, String address, String serial, String inventory,
                           Set<Integer> taken) {
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
        int blocked = 0;
        if (!(station.isEmpty() && fqdn.isEmpty() && ip.isEmpty())) {
            for (int i = 0; i < cards.size(); i++) {
                Card card = cards.get(i);
                if (!same(card, station, fqdn, ip, serial, inventory)) {
                    continue;
                }
                if (busy(taken, card.id)) {
                    if (blocked == 0) {
                        blocked = card.id;
                    }
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
        if (blocked != 0) {
            return blocked;
        }
        return sameName(cards, pointed, host, serial, inventory, taken);
    }

    /**
     * The same name and the same filled numbers, limited to one equipment type.
     * A printer named like a computer is not that computer.
     */
    public static int byName(List<Card> cards, String typeKey, String name, String serial, String inventory,
                             Set<Integer> taken) {
        return sameName(ofType(cards, typeKey), 0, name, serial, inventory, taken);
    }

    private static List<Card> ofType(List<Card> cards, String typeKey) {
        if (cards == null || typeKey == null || typeKey.isEmpty()) {
            return cards;
        }
        List<Card> same = new java.util.ArrayList<Card>();
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            if (card != null && typeKey.equals(card.typeKey)) {
                same.add(card);
            }
        }
        return same;
    }

    private static int sameName(List<Card> cards, int pointed, String name, String serial, String inventory,
                                Set<Integer> taken) {
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
        int blocked = 0;
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            if (card == null || card.location || card.id == 0 || !wanted.equals(plain(card.name))) {
                continue;
            }
            if (!DuplicateMarks.sameObject(card.serial, serial, card.inventory, inventory)) {
                continue;
            }
            if (busy(taken, card.id)) {
                if (blocked == 0) {
                    blocked = card.id;
                }
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
        int chosen = choose(pointedHit, count, only, hands, hand, oldest);
        return chosen != 0 ? chosen : blocked;
    }

    private static boolean busy(Set<Integer> taken, int id) {
        return taken != null && taken.contains(Integer.valueOf(id));
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
