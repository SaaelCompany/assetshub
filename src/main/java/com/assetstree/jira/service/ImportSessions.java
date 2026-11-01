package com.assetstree.jira.service;

import com.assetstree.jira.dto.ImportResultDto;
import com.assetstree.jira.model.EquipmentSheet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Holds a workbook between chunk requests. Entries expire after half an hour. */
public final class ImportSessions {
    private static final long TTL = 30L * 60L * 1000L;
    private static final int MAX = 8;

    private static final Map<String, Held> HELD = new ConcurrentHashMap<String, Held>();
    private static final Map<String, Slot> REPLAYS = new ConcurrentHashMap<String, Slot>();

    private ImportSessions() {
    }

    public static String keep(List<EquipmentSheet.Sheet> sheets) {
        sweep();
        if (HELD.size() >= MAX) {
            sweep();
        }
        String id = UUID.randomUUID().toString();
        HELD.put(id, new Held(sheets, System.currentTimeMillis() + TTL));
        return id;
    }

    public static List<EquipmentSheet.Sheet> sheets(String id) {
        sweep();
        if (id == null || id.isEmpty()) {
            return null;
        }
        Held held = HELD.get(id);
        if (held == null || held.expires < System.currentTimeMillis()) {
            HELD.remove(id);
            return null;
        }
        return held.sheets;
    }

    public static final class Claim {
        private final Slot slot;
        private final boolean owner;

        private Claim(Slot slot, boolean owner) {
            this.slot = slot;
            this.owner = owner;
        }

        public Slot getSlot() {
            return slot;
        }

        public boolean isOwner() {
            return owner;
        }
    }

    /** Reserves a replay key. The owner is the request that may write. */
    public static Claim claim(String key) {
        sweepReplays();
        if (key == null || key.isEmpty()) {
            return new Claim(new Slot(), true);
        }
        Slot fresh = new Slot();
        Slot previous = REPLAYS.putIfAbsent(key, fresh);
        if (previous == null) {
            return new Claim(fresh, true);
        }
        return new Claim(previous, false);
    }

    public static void forget(String key) {
        if (key != null) {
            REPLAYS.remove(key);
        }
    }

    private static void sweep() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Held>> entries = HELD.entrySet().iterator();
        while (entries.hasNext()) {
            if (entries.next().getValue().expires < now) {
                entries.remove();
            }
        }
        if (HELD.size() > MAX) {
            Iterator<String> keys = HELD.keySet().iterator();
            while (HELD.size() > MAX && keys.hasNext()) {
                keys.next();
                keys.remove();
            }
        }
    }

    private static final class Held {
        private final List<EquipmentSheet.Sheet> sheets;
        private final long expires;

        private Held(List<EquipmentSheet.Sheet> sheets, long expires) {
            this.sheets = sheets == null ? new ArrayList<EquipmentSheet.Sheet>() : sheets;
            this.expires = expires;
        }
    }

    private static void sweepReplays() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Slot>> entries = REPLAYS.entrySet().iterator();
        while (entries.hasNext()) {
            Slot slot = entries.next().getValue();
            if (now - slot.created > TTL) {
                entries.remove();
            }
        }
    }

    public static final class Slot {
        private ImportResultDto result;
        private final long created = System.currentTimeMillis();

        public ImportResultDto getResult() {
            return result;
        }

        public void setResult(ImportResultDto result) {
            this.result = result;
        }

        public boolean isReady() {
            return result != null;
        }
    }
}
