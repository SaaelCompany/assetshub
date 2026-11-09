package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Turns a CMDB payload into the same creates and updates the workbook uses.
 * A blank field is left as it is when {@code skipBlank} is set. A value is
 * written only when the caller sent it. Matching is the object key, then the
 * external id, then a host name such as KRUL-IT. A host match keeps the type
 * already on the card.
 */
public final class JsonImport {
    public static final int MAX_ITEMS = 2000;

    private JsonImport() {
    }

    public static final class Item {
        private final String key;
        private final String externalId;
        private final String name;
        private final String type;
        private final String status;
        private final String place;
        private final String custodian;
        private final String issued;
        private final String description;
        private final boolean descriptionSet;
        private final Map<String, String> fields;

        public Item(String key, String externalId, String name, String type, String status, String place,
                    String custodian, String issued, String description, boolean descriptionSet, Map<String, String> fields) {
            this.key = text(key);
            this.externalId = text(externalId);
            this.name = text(name);
            this.type = text(type);
            this.status = text(status);
            this.place = text(place);
            this.custodian = text(custodian);
            this.issued = text(issued);
            this.description = description == null ? "" : description;
            this.descriptionSet = descriptionSet;
            this.fields = fields == null ? new LinkedHashMap<String, String>() : fields;
        }

        private static String text(String value) {
            return value == null ? "" : value.trim();
        }
    }

    public static final class Existing {
        private final int id;
        private final String objectKey;
        private final String externalKey;
        private final String name;
        private final String description;
        private final String typeKey;
        private final String statusKey;
        private final int parentId;
        private final String custodianKey;
        private final String issuedKey;

        public Existing(int id, String objectKey, String externalKey, String name, String description, String typeKey,
                        String statusKey, int parentId, String custodianKey, String issuedKey) {
            this.id = id;
            this.objectKey = objectKey == null ? "" : objectKey;
            this.externalKey = externalKey == null ? "" : externalKey;
            this.name = name == null ? "" : name;
            this.description = description == null ? "" : description;
            this.typeKey = typeKey == null ? "" : typeKey;
            this.statusKey = statusKey == null ? "" : statusKey;
            this.parentId = parentId;
            this.custodianKey = custodianKey == null ? "" : custodianKey;
            this.issuedKey = issuedKey == null ? "" : issuedKey;
        }

        public int getId() {
            return id;
        }

        public String getObjectKey() {
            return objectKey;
        }

        public String getExternalKey() {
            return externalKey;
        }
    }

    public static final class Planned {
        private final EquipmentExchange.Change change;
        private final String externalId;

        Planned(EquipmentExchange.Change change, String externalId) {
            this.change = change;
            this.externalId = externalId == null ? "" : externalId;
        }

        public EquipmentExchange.Change getChange() {
            return change;
        }

        public String getExternalId() {
            return externalId;
        }
    }

    public static final class Plan {
        private final List<Planned> changes;
        private final List<EquipmentExchange.Issue> issues;

        Plan(List<Planned> changes, List<EquipmentExchange.Issue> issues) {
            this.changes = changes;
            this.issues = issues;
        }

        public List<Planned> getChanges() {
            return changes;
        }

        public List<EquipmentExchange.Issue> getIssues() {
            return issues;
        }
    }

    public static Plan plan(List<Item> items, List<Existing> existing, List<EquipmentExchange.Node> nodes,
                            List<EquipmentExchange.TypeRef> types, List<EquipmentExchange.Named> statuses,
                            String defaultStatus, boolean skipBlank, EquipmentExchange.Directory people) {
        List<Planned> changes = new ArrayList<Planned>();
        List<EquipmentExchange.Issue> issues = new ArrayList<EquipmentExchange.Issue>();
        if (items == null || items.isEmpty()) {
            issues.add(new EquipmentExchange.Issue(1, "asset-tree.error.import.empty", ""));
            return new Plan(changes, issues);
        }
        if (items.size() > MAX_ITEMS) {
            issues.add(new EquipmentExchange.Issue(1, "asset-tree.error.import.limit", String.valueOf(MAX_ITEMS)));
            return new Plan(changes, issues);
        }
        Map<String, Existing> byKey = new LinkedHashMap<String, Existing>();
        Map<String, Existing> byExternal = new LinkedHashMap<String, Existing>();
        Map<Integer, Existing> byId = new LinkedHashMap<Integer, Existing>();
        List<TniSame.Card> hosts = new ArrayList<TniSame.Card>();
        Set<String> places = new HashSet<String>();
        if (types != null) {
            for (int i = 0; i < types.size(); i++) {
                EquipmentExchange.TypeRef type = types.get(i);
                if (type != null && type.isLocation() && type.getKey() != null) {
                    places.add(type.getKey());
                }
            }
        }
        if (existing != null) {
            for (Existing row : existing) {
                if (row == null) {
                    continue;
                }
                byId.put(Integer.valueOf(row.id), row);
                hosts.add(new TniSame.Card(row.id, row.name, places.contains(row.typeKey), row.externalKey, ""));
                if (!row.objectKey.isEmpty()) {
                    byKey.put(row.objectKey.toLowerCase(Locale.ROOT), row);
                }
                if (!row.externalKey.isEmpty()) {
                    byExternal.put(row.externalKey.toLowerCase(Locale.ROOT), row);
                }
            }
        }
        Set<Integer> taken = new HashSet<Integer>();
        int[] synthetic = new int[] { -1 };
        int rowNumber = 1;
        for (Item item : items) {
            rowNumber++;
            if (item == null) {
                continue;
            }
            int before = issues.size();
            Existing match = match(item, byKey, byExternal, rowNumber, issues);
            if (issues.size() > before) {
                continue;
            }
            if (match == null && item.key.isEmpty() && item.externalId.isEmpty()) {
                int same = TniSame.pick(hosts, 0, item.name, "");
                if (same < 0 || (same > 0 && taken.contains(Integer.valueOf(same)))) {
                    issues.add(new EquipmentExchange.Issue(rowNumber, "asset-tree.error.import.host", item.name));
                    continue;
                }
                if (same > 0) {
                    match = byId.get(Integer.valueOf(same));
                }
            }
            if (match != null && !taken.add(Integer.valueOf(match.id))) {
                boolean named = item.key.isEmpty() && item.externalId.isEmpty();
                String argument = !item.key.isEmpty() ? item.key : (!item.externalId.isEmpty() ? item.externalId : item.name);
                issues.add(new EquipmentExchange.Issue(rowNumber,
                        named ? "asset-tree.error.import.host" : "asset-tree.error.import.key.duplicate", argument));
                continue;
            }
            if (match != null && match.id < 0) {
                issues.add(new EquipmentExchange.Issue(rowNumber, "asset-tree.error.import.key.duplicate", item.externalId));
                continue;
            }
            EquipmentExchange.TypeRef type = resolveType(item.type, types);
            if (type == null) {
                if (match != null && (item.type.isEmpty() && skipBlank)) {
                    type = typeByKey(match.typeKey, types);
                }
            }
            if (type == null || type.isLocation()) {
                issues.add(new EquipmentExchange.Issue(rowNumber, "asset-tree.error.import.type", item.type));
                continue;
            }
            boolean hostMatch = item.key.isEmpty() && item.externalId.isEmpty() && match != null && match.id > 0;
            if (hostMatch && !match.typeKey.isEmpty()) {
                EquipmentExchange.TypeRef kept = typeByKey(match.typeKey, types);
                if (kept != null && !kept.isLocation()) {
                    type = kept;
                }
            }
            String statusKey = resolveStatus(item.status, statuses);
            if (statusKey == null) {
                if (item.status.isEmpty() && match != null) {
                    statusKey = match.statusKey;
                } else if (item.status.isEmpty()) {
                    statusKey = defaultStatus;
                } else {
                    issues.add(new EquipmentExchange.Issue(rowNumber, "asset-tree.error.import.status", item.status));
                    continue;
                }
            }
            int parentId;
            if (item.place.isEmpty() && match != null && skipBlank) {
                parentId = match.parentId;
            } else if (item.place.isEmpty() && match != null) {
                parentId = match.parentId;
            } else {
                Integer place = EquipmentExchange.findPlace(item.place, nodes, rowNumber, issues);
                if (place == null) {
                    continue;
                }
                parentId = place.intValue();
            }
            String name = item.name;
            if (name.isEmpty()) {
                if (match != null && skipBlank) {
                    name = match.name;
                } else {
                    issues.add(new EquipmentExchange.Issue(rowNumber, "asset-tree.error.import.name", ""));
                    continue;
                }
            }
            String description = null;
            if (item.descriptionSet && !(skipBlank && item.description.trim().isEmpty())) {
                description = item.description;
            } else if (!skipBlank && item.descriptionSet) {
                description = item.description;
            }
            boolean custodianPresent = !(item.custodian.isEmpty() && skipBlank);
            boolean issuedPresent = !(item.issued.isEmpty() && skipBlank);
            String custodianKey = null;
            String issuedKey = null;
            if (custodianPresent) {
                custodianKey = person(item.custodian, people, rowNumber, issues);
                if (!item.custodian.isEmpty() && custodianKey == null) {
                    continue;
                }
            }
            if (issuedPresent) {
                issuedKey = person(item.issued, people, rowNumber, issues);
                if (!item.issued.isEmpty() && issuedKey == null) {
                    continue;
                }
            }
            List<EquipmentExchange.Value> attributes = new ArrayList<EquipmentExchange.Value>();
            boolean fieldError = false;
            for (Map.Entry<String, String> entry : item.fields.entrySet()) {
                String fieldKey = entry.getKey() == null ? "" : entry.getKey().trim();
                String value = entry.getValue() == null ? "" : entry.getValue();
                if (fieldKey.isEmpty()) {
                    continue;
                }
                if (skipBlank && value.trim().isEmpty()) {
                    continue;
                }
                EquipmentExchange.FieldRef field = field(type, fieldKey);
                if (field == null) {
                    continue;
                }
                if (field.isRequired() && value.trim().isEmpty() && match == null) {
                    issues.add(new EquipmentExchange.Issue(rowNumber, "asset-tree.error.import.field.required", field.getLabel()));
                    fieldError = true;
                    break;
                }
                attributes.add(new EquipmentExchange.Value(field.getKey(), value.trim()));
            }
            if (fieldError) {
                continue;
            }
            if (match == null) {
                for (EquipmentExchange.FieldRef field : type.getFields()) {
                    if (!field.isRequired()) {
                        continue;
                    }
                    boolean present = false;
                    for (EquipmentExchange.Value value : attributes) {
                        if (field.getKey().equals(value.getFieldKey()) && !value.getValue().trim().isEmpty()) {
                            present = true;
                        }
                    }
                    if (!present) {
                        issues.add(new EquipmentExchange.Issue(rowNumber, "asset-tree.error.import.field.required", field.getLabel()));
                        fieldError = true;
                        break;
                    }
                }
            }
            if (fieldError) {
                continue;
            }
            Integer existingId = match == null ? null : Integer.valueOf(match.id);
            EquipmentExchange.Change change = new EquipmentExchange.Change(rowNumber, existingId, name, description,
                    type.getKey(), statusKey, parentId, custodianPresent, custodianKey, issuedPresent, issuedKey, attributes);
            String externalId = item.externalId.isEmpty() && match != null ? match.externalKey : item.externalId;
            changes.add(new Planned(change, externalId));
            if (match == null && !externalId.isEmpty()) {
                byExternal.put(externalId.toLowerCase(Locale.ROOT), new Existing(-1, "", externalId, name, "", type.getKey(), statusKey, parentId, "", ""));
            }
            if (match == null && !TniText.shortHost(name).isEmpty()) {
                hosts.add(new TniSame.Card(synthetic[0]--, name, false, "", ""));
            }
        }
        return new Plan(changes, issues);
    }

    private static Existing match(Item item, Map<String, Existing> byKey, Map<String, Existing> byExternal, int row,
                                  List<EquipmentExchange.Issue> issues) {
        Existing byObject = item.key.isEmpty() ? null : byKey.get(item.key.toLowerCase(Locale.ROOT));
        Existing byId = item.externalId.isEmpty() ? null : byExternal.get(item.externalId.toLowerCase(Locale.ROOT));
        if (!item.key.isEmpty() && byObject == null) {
            issues.add(new EquipmentExchange.Issue(row, "asset-tree.error.import.key", item.key));
            return null;
        }
        if (byObject != null && byId != null && byObject.id != byId.id) {
            issues.add(new EquipmentExchange.Issue(row, "asset-tree.error.import.key", item.key));
            return null;
        }
        if (byObject != null) {
            return byObject;
        }
        return byId;
    }

    private static EquipmentExchange.TypeRef resolveType(String raw, List<EquipmentExchange.TypeRef> types) {
        if (raw == null || raw.isEmpty() || types == null) {
            return null;
        }
        EquipmentExchange.TypeRef found = null;
        for (EquipmentExchange.TypeRef type : types) {
            if (type == null || type.isLocation()) {
                continue;
            }
            if (EquipmentExchange.norm(type.getKey()).equals(EquipmentExchange.norm(raw))
                    || EquipmentExchange.norm(type.getLabel()).equals(EquipmentExchange.norm(raw))) {
                if (found != null) {
                    return null;
                }
                found = type;
            }
        }
        return found;
    }

    private static EquipmentExchange.TypeRef typeByKey(String key, List<EquipmentExchange.TypeRef> types) {
        if (types == null) {
            return null;
        }
        for (EquipmentExchange.TypeRef type : types) {
            if (type != null && key.equals(type.getKey())) {
                return type;
            }
        }
        return null;
    }

    private static String resolveStatus(String raw, List<EquipmentExchange.Named> statuses) {
        if (raw == null || raw.isEmpty() || statuses == null) {
            return null;
        }
        for (EquipmentExchange.Named status : statuses) {
            if (status == null) {
                continue;
            }
            if (EquipmentExchange.norm(status.getKey()).equals(EquipmentExchange.norm(raw))
                    || EquipmentExchange.norm(status.getLabel()).equals(EquipmentExchange.norm(raw))) {
                return status.getKey();
            }
        }
        return null;
    }

    private static EquipmentExchange.FieldRef field(EquipmentExchange.TypeRef type, String key) {
        for (EquipmentExchange.FieldRef field : type.getFields()) {
            if (field.getKey().equals(key) || EquipmentExchange.norm(field.getLabel()).equals(EquipmentExchange.norm(key))) {
                return field;
            }
        }
        return null;
    }

    private static String person(String raw, EquipmentExchange.Directory people, int row, List<EquipmentExchange.Issue> issues) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        if (people == null) {
            issues.add(new EquipmentExchange.Issue(row, "asset-tree.error.import.user", raw));
            return null;
        }
        EquipmentExchange.Person found = people.find(raw);
        if (found == null || found.isMissing()) {
            issues.add(new EquipmentExchange.Issue(row, "asset-tree.error.import.user", raw));
            return null;
        }
        if (found.isAmbiguous()) {
            issues.add(new EquipmentExchange.Issue(row, "asset-tree.error.import.user.ambiguous", raw));
            return null;
        }
        return found.getKey() == null ? "" : found.getKey();
    }
}
