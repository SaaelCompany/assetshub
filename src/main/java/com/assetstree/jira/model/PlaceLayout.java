package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * How equipment types sit on one place. A stored line is either a type key
 * (the schema parent still applies) or {@code typeKey@parentKey}. An empty
 * parent after {@code @} keeps the type on the place itself. Sibling order
 * follows the schema until every line is explicit; the first move writes the
 * whole place that way, so later moves stay put.
 */
public final class PlaceLayout {
    public static final String UP = "up";
    public static final String DOWN = "down";
    public static final String NEST = "nest";
    public static final String LIFT = "lift";
    public static final String BEFORE = "before";
    public static final String AFTER = "after";
    public static final String INTO = "into";

    private PlaceLayout() {
    }

    public static final class Fact {
        private final String key;
        private final String schemaParent;

        public Fact(String key, String schemaParent) {
            this.key = key == null ? "" : key.trim();
            this.schemaParent = schemaParent == null ? "" : schemaParent.trim();
        }

        public String getKey() {
            return key;
        }

        public String getSchemaParent() {
            return schemaParent;
        }
    }

    public static final class Slot {
        private final String typeKey;
        private final String parentKey;

        public Slot(String typeKey, String parentKey) {
            this.typeKey = typeKey == null ? "" : typeKey;
            this.parentKey = parentKey == null ? "" : parentKey;
        }

        public String getTypeKey() {
            return typeKey;
        }

        public String getParentKey() {
            return parentKey;
        }
    }

    public static String lineKey(String line) {
        if (line == null) {
            return "";
        }
        String text = line.trim();
        int at = text.indexOf('@');
        if (at >= 0) {
            text = text.substring(0, at).trim();
        }
        return text;
    }

    public static boolean ordered(List<String> lines) {
        boolean any = false;
        if (lines == null) {
            return false;
        }
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            any = true;
            if (line.indexOf('@') < 0) {
                return false;
            }
        }
        return any;
    }

    public static List<Slot> layout(List<String> lines, List<String> pinned, List<Fact> schema) {
        Map<String, Fact> facts = facts(schema);
        List<String> schemaKeys = new ArrayList<String>(facts.keySet());
        Map<String, Integer> linePos = new LinkedHashMap<String, Integer>();
        Map<String, String> explicit = new LinkedHashMap<String, String>();
        int position = 0;
        if (lines != null) {
            for (String line : lines) {
                if (line == null) {
                    continue;
                }
                String text = line.trim();
                if (text.isEmpty()) {
                    continue;
                }
                String key = lineKey(text);
                if (key.isEmpty() || !facts.containsKey(key) || linePos.containsKey(key)) {
                    continue;
                }
                linePos.put(key, Integer.valueOf(position));
                position++;
                int at = text.indexOf('@');
                if (at >= 0) {
                    String parent = text.substring(at + 1).trim();
                    if (parent.equals(key) || !facts.containsKey(parent)) {
                        parent = "";
                    }
                    explicit.put(key, parent);
                }
            }
        }
        Map<String, Boolean> visible = new LinkedHashMap<String, Boolean>();
        if (pinned != null) {
            for (String key : pinned) {
                String trimmed = key == null ? "" : key.trim();
                if (facts.containsKey(trimmed)) {
                    visible.put(trimmed, Boolean.TRUE);
                }
            }
        }
        for (String key : linePos.keySet()) {
            visible.put(key, Boolean.TRUE);
        }
        Map<String, String> raw = new LinkedHashMap<String, String>();
        rememberParents(visible, raw, explicit, facts);
        boolean grew = true;
        int guard = 0;
        while (grew && guard < 8) {
            grew = false;
            guard++;
            List<String> current = new ArrayList<String>(visible.keySet());
            for (String key : current) {
                String parent = raw.get(key);
                if (parent == null || parent.isEmpty() || visible.containsKey(parent)) {
                    continue;
                }
                visible.put(parent, Boolean.TRUE);
                grew = true;
            }
            if (grew) {
                rememberParents(visible, raw, explicit, facts);
            }
        }
        Map<String, String> parentOf = flatten(visible, raw);
        List<String> roots = new ArrayList<String>();
        Map<String, List<String>> children = new LinkedHashMap<String, List<String>>();
        for (String key : schemaKeys) {
            if (!visible.containsKey(key)) {
                continue;
            }
            String parent = parentOf.get(key);
            if (parent == null || parent.isEmpty() || !visible.containsKey(parent)) {
                roots.add(key);
                continue;
            }
            List<String> bucket = children.get(parent);
            if (bucket == null) {
                bucket = new ArrayList<String>();
                children.put(parent, bucket);
            }
            bucket.add(key);
        }
        if (ordered(lines)) {
            sortByLine(roots, linePos, schemaKeys);
            for (List<String> bucket : children.values()) {
                sortByLine(bucket, linePos, schemaKeys);
            }
        }
        List<Slot> slots = new ArrayList<Slot>();
        for (String root : roots) {
            slots.add(new Slot(root, ""));
            List<String> nested = children.get(root);
            if (nested == null) {
                continue;
            }
            for (String child : nested) {
                slots.add(new Slot(child, root));
            }
        }
        return slots;
    }

    /**
     * @return the explicit lines after the move, or null when this type cannot move that way
     */
    public static List<String> move(List<String> lines, List<String> pinned, List<Fact> schema,
                                    String typeKey, String action) {
        String key = typeKey == null ? "" : typeKey.trim();
        String step = action == null ? "" : action.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) {
            return null;
        }
        String verb = step;
        String targetKey = "";
        int colon = step.indexOf(':');
        if (colon > 0) {
            verb = step.substring(0, colon);
            targetKey = step.substring(colon + 1).trim();
        }
        boolean stepMove = UP.equals(verb) || DOWN.equals(verb) || NEST.equals(verb) || LIFT.equals(verb);
        boolean placeMove = (BEFORE.equals(verb) || AFTER.equals(verb) || INTO.equals(verb)) && !targetKey.isEmpty();
        if (!stepMove && !placeMove) {
            return null;
        }
        List<Slot> slots = layout(lines, pinned, schema);
        List<String> roots = new ArrayList<String>();
        Map<String, List<String>> children = new LinkedHashMap<String, List<String>>();
        Map<String, String> parentOf = new LinkedHashMap<String, String>();
        for (Slot slot : slots) {
            if (slot.parentKey.isEmpty()) {
                roots.add(slot.typeKey);
                parentOf.put(slot.typeKey, "");
                continue;
            }
            List<String> bucket = children.get(slot.parentKey);
            if (bucket == null) {
                bucket = new ArrayList<String>();
                children.put(slot.parentKey, bucket);
            }
            bucket.add(slot.typeKey);
            parentOf.put(slot.typeKey, slot.parentKey);
        }
        if (!parentOf.containsKey(key)) {
            return null;
        }
        if (placeMove) {
            return placeAt(roots, children, parentOf, key, verb, targetKey);
        }
        String parent = parentOf.get(key);
        if (UP.equals(verb) || DOWN.equals(verb)) {
            List<String> siblings = parent.isEmpty() ? roots : children.get(parent);
            if (siblings == null) {
                return null;
            }
            int index = siblings.indexOf(key);
            int swap = UP.equals(verb) ? index - 1 : index + 1;
            if (index < 0 || swap < 0 || swap >= siblings.size()) {
                return write(roots, children);
            }
            Collections.swap(siblings, index, swap);
            return write(roots, children);
        }
        if (NEST.equals(verb)) {
            if (!parent.isEmpty()) {
                return null;
            }
            List<String> own = children.get(key);
            if (own != null && !own.isEmpty()) {
                return null;
            }
            int index = roots.indexOf(key);
            if (index <= 0) {
                return null;
            }
            String previous = roots.get(index - 1);
            roots.remove(index);
            List<String> nested = children.get(previous);
            if (nested == null) {
                nested = new ArrayList<String>();
                children.put(previous, nested);
            }
            nested.add(key);
            return write(roots, children);
        }
        if (parent.isEmpty()) {
            return null;
        }
        List<String> nested = children.get(parent);
        if (nested != null) {
            nested.remove(key);
        }
        int at = roots.indexOf(parent);
        if (at < 0) {
            roots.add(key);
        } else {
            roots.add(at + 1, key);
        }
        return write(roots, children);
    }

    private static List<String> placeAt(List<String> roots, Map<String, List<String>> children,
                                        Map<String, String> parentOf, String key, String verb, String targetKey) {
        if (key.equals(targetKey) || !parentOf.containsKey(targetKey)) {
            return null;
        }
        String targetParent = parentOf.get(targetKey);
        if (targetParent == null) {
            targetParent = "";
        }
        List<String> own = children.get(key);
        boolean hasKids = own != null && !own.isEmpty();
        if (INTO.equals(verb)) {
            if (!targetParent.isEmpty() || hasKids) {
                return null;
            }
        } else if (hasKids && !targetParent.isEmpty()) {
            return null;
        }
        roots.remove(key);
        for (List<String> bucket : children.values()) {
            bucket.remove(key);
        }
        if (INTO.equals(verb)) {
            List<String> nested = children.get(targetKey);
            if (nested == null) {
                nested = new ArrayList<String>();
                children.put(targetKey, nested);
            }
            nested.add(key);
            return write(roots, children);
        }
        List<String> siblings = targetParent.isEmpty() ? roots : children.get(targetParent);
        if (siblings == null) {
            siblings = new ArrayList<String>();
            children.put(targetParent, siblings);
        }
        int at = siblings.indexOf(targetKey);
        if (at < 0) {
            siblings.add(key);
        } else {
            if (AFTER.equals(verb)) {
                at = at + 1;
            }
            siblings.add(at, key);
        }
        return write(roots, children);
    }

    public static List<String> addPin(List<String> lines, List<Fact> schema, String typeKey) {
        Map<String, Fact> facts = facts(schema);
        String key = typeKey == null ? "" : typeKey.trim();
        if (!facts.containsKey(key)) {
            return null;
        }
        List<String> next = new ArrayList<String>();
        if (lines != null) {
            next.addAll(lines);
        }
        for (String line : next) {
            if (key.equals(lineKey(line))) {
                return next;
            }
        }
        if (ordered(next)) {
            String parent = facts.get(key).schemaParent;
            if (!facts.containsKey(parent) || parent.equals(key)) {
                parent = "";
            }
            next.add(key + "@" + parent);
        } else {
            next.add(key);
        }
        return next;
    }

    public static List<String> removePin(List<String> lines, String typeKey) {
        String key = typeKey == null ? "" : typeKey.trim();
        List<String> next = new ArrayList<String>();
        if (lines == null) {
            return next;
        }
        for (String line : lines) {
            if (!key.equals(lineKey(line))) {
                next.add(line);
            }
        }
        return next;
    }

    private static void rememberParents(Map<String, Boolean> visible, Map<String, String> raw,
                                        Map<String, String> explicit, Map<String, Fact> facts) {
        for (String key : visible.keySet()) {
            if (explicit.containsKey(key)) {
                raw.put(key, explicit.get(key));
                continue;
            }
            String schemaParent = facts.get(key).schemaParent;
            if (!schemaParent.isEmpty() && !schemaParent.equals(key) && facts.containsKey(schemaParent)) {
                raw.put(key, schemaParent);
            } else {
                raw.put(key, "");
            }
        }
    }

    private static Map<String, String> flatten(Map<String, Boolean> visible, Map<String, String> raw) {
        Map<String, String> parentOf = new LinkedHashMap<String, String>();
        for (String key : visible.keySet()) {
            String parent = raw.get(key);
            if (parent == null || parent.isEmpty() || !visible.containsKey(parent) || parent.equals(key)) {
                parentOf.put(key, "");
            } else {
                parentOf.put(key, parent);
            }
        }
        boolean changed = true;
        int guard = 0;
        while (changed && guard < 8) {
            changed = false;
            guard++;
            for (String key : new ArrayList<String>(parentOf.keySet())) {
                String parent = parentOf.get(key);
                if (parent == null || parent.isEmpty()) {
                    continue;
                }
                String above = parentOf.get(parent);
                if (above != null && !above.isEmpty()) {
                    parentOf.put(key, "");
                    changed = true;
                }
            }
        }
        return parentOf;
    }

    private static Map<String, Fact> facts(List<Fact> schema) {
        Map<String, Fact> facts = new LinkedHashMap<String, Fact>();
        if (schema == null) {
            return facts;
        }
        for (Fact fact : schema) {
            if (fact == null || fact.key.isEmpty() || facts.containsKey(fact.key)) {
                continue;
            }
            facts.put(fact.key, fact);
        }
        return facts;
    }

    private static void sortByLine(List<String> keys, final Map<String, Integer> linePos, final List<String> schemaKeys) {
        Collections.sort(keys, new Comparator<String>() {
            @Override
            public int compare(String left, String right) {
                return Integer.compare(score(left, linePos, schemaKeys), score(right, linePos, schemaKeys));
            }
        });
    }

    private static int score(String key, Map<String, Integer> linePos, List<String> schemaKeys) {
        Integer at = linePos.get(key);
        if (at != null) {
            return at.intValue();
        }
        int schemaAt = schemaKeys.indexOf(key);
        if (schemaAt < 0) {
            schemaAt = schemaKeys.size();
        }
        return 1000000 + schemaAt;
    }

    private static List<String> write(List<String> roots, Map<String, List<String>> children) {
        List<String> lines = new ArrayList<String>();
        for (String root : roots) {
            lines.add(root + "@");
            List<String> nested = children.get(root);
            if (nested == null) {
                continue;
            }
            for (String child : nested) {
                lines.add(child + "@" + root);
            }
        }
        return lines;
    }
}
