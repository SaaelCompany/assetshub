package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Turns a spreadsheet of equipment into creates and updates.
 * A blank key creates an object. A key already in the project updates it.
 * A host name such as KRUL-IT updates the computer that already has that name.
 * Places and types must already exist: the file does not invent the tree.
 */
public final class EquipmentExchange {
    /** One workbook, across every sheet. A larger book is refused. */
    public static final int MAX_ROWS = 20000;
    /** Rows written by one request when the client asks for chunks. */
    public static final int CHUNK_ROWS = 400;

    private static final Pattern NUMBER = Pattern.compile("^-?\\d{1,12}(\\.\\d{1,4})?$");
    private static final String[] ROLES = {
            "key", "name", "type", "status", "place", "custodian", "issued", "description",
            "serviceKind", "serviceName", "serviceDate", "serviceEvery", "serviceUnit", "serviceWho",
            "lifePurchased", "lifeSupplier", "lifeDocument", "lifePrice", "lifeCurrency", "lifeCommissioned",
            "lifeWarranty", "lifeWarrantyService", "lifeContractor", "lifeContractNo", "lifeContractFrom",
            "lifeContractUntil", "lifeMonths", "lifeResidual"
    };
    private static final String[] LIFE_FIELDS = {
            "purchased", "supplier", "document", "price", "currency", "commissioned",
            "warrantyUntil", "warrantyService", "contractor", "contractNo", "contractFrom",
            "contractUntil", "lifeMonths", "residual"
    };
    private static final int LIFE_FIRST = 14;
    private static final int ROLE_KEY = 0;
    private static final int ROLE_SERVICE_KIND = 8;
    private static final int ROLE_SERVICE_NAME = 9;
    private static final int ROLE_SERVICE_DATE = 10;
    private static final int ROLE_SERVICE_EVERY = 11;
    private static final int ROLE_SERVICE_UNIT = 12;
    private static final int ROLE_SERVICE_WHO = 13;

    private EquipmentExchange() {
    }

    public interface Directory {
        Person find(String raw);
    }

    public static final class Person {
        private final String key;
        private final boolean ambiguous;

        private Person(String key, boolean ambiguous) {
            this.key = key;
            this.ambiguous = ambiguous;
        }

        public static Person blank() {
            return new Person(null, false);
        }

        public static Person found(String key) {
            return new Person(key, false);
        }

        public static Person missing() {
            return new Person("", false);
        }

        public static Person ambiguous() {
            return new Person("", true);
        }

        public String getKey() {
            return key;
        }

        public boolean isBlank() {
            return key == null;
        }

        public boolean isMissing() {
            return !ambiguous && key != null && key.isEmpty();
        }

        public boolean isAmbiguous() {
            return ambiguous;
        }
    }

    public static final class FieldRef {
        private final String key;
        private final String label;
        private final String kind;
        private final boolean required;
        private final String options;

        public FieldRef(String key, String label, String kind, boolean required) {
            this(key, label, kind, required, "");
        }

        public FieldRef(String key, String label, String kind, boolean required, String options) {
            this.key = key;
            this.label = label;
            this.kind = kind;
            this.required = required;
            this.options = options == null ? "" : options;
        }

        public String getKey() {
            return key;
        }

        public String getLabel() {
            return label;
        }

        public String getKind() {
            return kind;
        }

        public boolean isRequired() {
            return required;
        }

        public String getOptions() {
            return options;
        }
    }

    public static final class TypeRef {
        private final String key;
        private final String label;
        private final boolean location;
        private final List<FieldRef> fields;

        public TypeRef(String key, String label, boolean location, List<FieldRef> fields) {
            this.key = key;
            this.label = label;
            this.location = location;
            this.fields = fields == null ? new ArrayList<FieldRef>() : fields;
        }

        public String getKey() {
            return key;
        }

        public String getLabel() {
            return label;
        }

        public boolean isLocation() {
            return location;
        }

        public List<FieldRef> getFields() {
            return fields;
        }
    }

    public static final class Named {
        private final String key;
        private final String label;

        public Named(String key, String label) {
            this.key = key;
            this.label = label;
        }

        public String getKey() {
            return key;
        }

        public String getLabel() {
            return label;
        }
    }

    public static final class Node {
        private final int id;
        private final Integer parentId;
        private final String name;
        private final String objectKey;
        private final boolean location;

        private final String typeKey;

        public Node(int id, Integer parentId, String name, String objectKey, boolean location) {
            this(id, parentId, name, objectKey, location, "");
        }

        public Node(int id, Integer parentId, String name, String objectKey, boolean location, String typeKey) {
            this.id = id;
            this.parentId = parentId;
            this.name = name;
            this.objectKey = objectKey;
            this.location = location;
            this.typeKey = typeKey == null ? "" : typeKey;
        }

        public int getId() {
            return id;
        }

        public Integer getParentId() {
            return parentId;
        }

        public String getName() {
            return name;
        }

        public String getObjectKey() {
            return objectKey;
        }

        public boolean isLocation() {
            return location;
        }

        public String getTypeKey() {
            return typeKey;
        }
    }

    public static final class Value {
        private final String fieldKey;
        private final String value;

        Value(String fieldKey, String value) {
            this.fieldKey = fieldKey;
            this.value = value;
        }

        public String getFieldKey() {
            return fieldKey;
        }

        public String getValue() {
            return value;
        }
    }

    public static final class Change {
        private final int row;
        private final Integer existingId;
        private final String name;
        private final String description;
        private final String typeKey;
        private final String statusKey;
        private final int parentId;
        private final boolean custodianPresent;
        private final String custodianKey;
        private final boolean issuedPresent;
        private final String issuedKey;
        private final List<Value> attributes;
        private final Service service;
        private final LifeBook.Change lifecycle;

        Change(int row, Integer existingId, String name, String description, String typeKey, String statusKey,
               int parentId, boolean custodianPresent, String custodianKey, boolean issuedPresent, String issuedKey,
               List<Value> attributes) {
            this(row, existingId, name, description, typeKey, statusKey, parentId, custodianPresent, custodianKey,
                    issuedPresent, issuedKey, attributes, null, null);
        }

        Change(int row, Integer existingId, String name, String description, String typeKey, String statusKey,
               int parentId, boolean custodianPresent, String custodianKey, boolean issuedPresent, String issuedKey,
               List<Value> attributes, Service service) {
            this(row, existingId, name, description, typeKey, statusKey, parentId, custodianPresent, custodianKey,
                    issuedPresent, issuedKey, attributes, service, null);
        }

        Change(int row, Integer existingId, String name, String description, String typeKey, String statusKey,
               int parentId, boolean custodianPresent, String custodianKey, boolean issuedPresent, String issuedKey,
               List<Value> attributes, Service service, LifeBook.Change lifecycle) {
            this.row = row;
            this.existingId = existingId;
            this.name = name;
            this.description = description;
            this.typeKey = typeKey;
            this.statusKey = statusKey;
            this.parentId = parentId;
            this.custodianPresent = custodianPresent;
            this.custodianKey = custodianKey;
            this.issuedPresent = issuedPresent;
            this.issuedKey = issuedKey;
            this.attributes = attributes;
            this.service = service;
            this.lifecycle = lifecycle;
        }

        public int getRow() {
            return row;
        }

        public Integer getExistingId() {
            return existingId;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getTypeKey() {
            return typeKey;
        }

        public String getStatusKey() {
            return statusKey;
        }

        public int getParentId() {
            return parentId;
        }

        public boolean isCustodianPresent() {
            return custodianPresent;
        }

        public String getCustodianKey() {
            return custodianKey;
        }

        public boolean isIssuedPresent() {
            return issuedPresent;
        }

        public String getIssuedKey() {
            return issuedKey;
        }

        public List<Value> getAttributes() {
            return attributes;
        }

        public Service getService() {
            return service;
        }

        public LifeBook.Change getLifecycle() {
            return lifecycle;
        }
    }

    /** One maintenance row from the object line or from the service sheet. */
    public static final class Service {
        private final int row;
        private final String sheet;
        private final String objectKey;
        private final String kind;
        private final String name;
        private final String lastDone;
        private final int everyCount;
        private final String everyUnit;
        private final String assigneeKey;

        public Service(int row, String sheet, String objectKey, String kind, String name, String lastDone,
                       int everyCount, String everyUnit, String assigneeKey) {
            this.row = row;
            this.sheet = sheet == null ? "" : sheet;
            this.objectKey = objectKey == null ? "" : objectKey;
            this.kind = kind == null ? "" : kind;
            this.name = name == null ? "" : name;
            this.lastDone = lastDone == null ? "" : lastDone;
            this.everyCount = everyCount;
            this.everyUnit = everyUnit == null ? "" : everyUnit;
            this.assigneeKey = assigneeKey == null ? "" : assigneeKey;
        }

        public int getRow() {
            return row;
        }

        public String getSheet() {
            return sheet;
        }

        public String getObjectKey() {
            return objectKey;
        }

        public String getKind() {
            return kind;
        }

        public String getName() {
            return name;
        }

        public String getLastDone() {
            return lastDone;
        }

        public int getEveryCount() {
            return everyCount;
        }

        public String getEveryUnit() {
            return everyUnit;
        }

        public String getAssigneeKey() {
            return assigneeKey;
        }
    }

    public static final class Issue {
        private final int row;
        private final String messageKey;
        private final String argument;
        private final String sheet;

        Issue(int row, String messageKey, String argument) {
            this(row, messageKey, argument, "");
        }

        Issue(int row, String messageKey, String argument, String sheet) {
            this.row = row;
            this.messageKey = messageKey;
            this.argument = argument == null ? "" : argument;
            this.sheet = sheet == null ? "" : sheet;
        }

        public int getRow() {
            return row;
        }

        public String getMessageKey() {
            return messageKey;
        }

        public String getArgument() {
            return argument;
        }

        public String getSheet() {
            return sheet;
        }
    }

    public static final class Plan {
        private final List<Change> changes;
        private final List<Issue> issues;
        private final List<Service> services;
        private final List<LifeBook.Change> lifecycles;

        Plan(List<Change> changes, List<Issue> issues) {
            this(changes, issues, new ArrayList<Service>());
        }

        Plan(List<Change> changes, List<Issue> issues, List<Service> services) {
            this(changes, issues, services, new ArrayList<LifeBook.Change>());
        }

        Plan(List<Change> changes, List<Issue> issues, List<Service> services, List<LifeBook.Change> lifecycles) {
            this.changes = changes;
            this.issues = issues;
            this.services = services == null ? new ArrayList<Service>() : services;
            this.lifecycles = lifecycles == null ? new ArrayList<LifeBook.Change>() : lifecycles;
        }

        public List<Change> getChanges() {
            return changes;
        }

        public List<Issue> getIssues() {
            return issues;
        }

        public List<Service> getServices() {
            return services;
        }

        public List<LifeBook.Change> getLifecycles() {
            return lifecycles;
        }
    }

    public static Plan plan(EquipmentSheet.Sheet sheet, List<TypeRef> types, List<Named> statuses, List<Node> nodes,
                            String defaultStatus, Map<String, String> labels, Directory people) {
        return plan(sheet, types, statuses, nodes, defaultStatus, labels, people, null);
    }

    /**
     * Reads every worksheet. A sheet whose name is one equipment type uses that
     * type when the type cell is empty, and only that type's columns. A sheet
     * name that is not a type, including the older single sheet "Equipment",
     * keeps the type written in each row. A sheet with only a header is a
     * blank template and is skipped. An empty book is still an error.
     */
    public static Plan planBook(List<EquipmentSheet.Sheet> sheets, List<TypeRef> types, List<Named> statuses,
                                List<Node> nodes, String defaultStatus, Map<String, String> labels, Directory people) {
        return planBook(sheets, types, statuses, nodes, defaultStatus, labels, people, false, null);
    }

    /**
     * Same as {@link #planBook(List, List, List, List, String, Map, Directory)}.
     * When {@code service} is true, maintenance columns on an object row and a
     * sheet that has a work-kind column and no name column become plans.
     * When it is false those columns are ignored and that sheet is skipped,
     * so a book saved while maintenance was on still loads.
     * Lifecycle columns follow the same rule when {@code life} is true.
     */
    public static Plan planBook(List<EquipmentSheet.Sheet> sheets, List<TypeRef> types, List<Named> statuses,
                                List<Node> nodes, String defaultStatus, Map<String, String> labels, Directory people,
                                boolean service, List<Named> kinds) {
        return planBook(sheets, types, statuses, nodes, defaultStatus, labels, people, service, kinds, false);
    }

    public static Plan planBook(List<EquipmentSheet.Sheet> sheets, List<TypeRef> types, List<Named> statuses,
                                List<Node> nodes, String defaultStatus, Map<String, String> labels, Directory people,
                                boolean service, List<Named> kinds, boolean life) {
        List<Change> changes = new ArrayList<Change>();
        List<Issue> issues = new ArrayList<Issue>();
        List<Service> services = new ArrayList<Service>();
        List<LifeBook.Change> lifecycles = new ArrayList<LifeBook.Change>();
        if (sheets == null || sheets.isEmpty()) {
            issues.add(new Issue(1, "asset-tree.error.import.empty", ""));
            return new Plan(changes, issues, services);
        }
        int rows = 0;
        for (EquipmentSheet.Sheet sheet : sheets) {
            if (sheet != null) {
                rows += sheet.getRecords().size();
            }
        }
        if (rows > MAX_ROWS) {
            issues.add(new Issue(1, "asset-tree.error.import.limit", String.valueOf(MAX_ROWS)));
            return new Plan(changes, issues, services);
        }
        Map<String, Integer> seenKeys = new LinkedHashMap<String, Integer>();
        List<Node> known = new ArrayList<Node>();
        if (nodes != null) {
            known.addAll(nodes);
        }
        int[] made = new int[] { -1 };
        boolean anyData = false;
        for (EquipmentSheet.Sheet sheet : sheets) {
            if (sheet == null || (sheet.getHeaders().isEmpty() && sheet.getRecords().isEmpty())) {
                continue;
            }
            if (serviceSheet(sheet, labels)) {
                if (!service || sheet.getRecords().isEmpty()) {
                    continue;
                }
                anyData = true;
                List<Issue> partIssues = new ArrayList<Issue>();
                readServiceSheet(sheet, known, people, kinds, labels, services, partIssues);
                issues.addAll(stamp(sheet.getName(), partIssues));
                continue;
            }
            if (lifeSheet(sheet, labels)) {
                if (!life || sheet.getRecords().isEmpty()) {
                    continue;
                }
                anyData = true;
                List<Issue> partIssues = new ArrayList<Issue>();
                readLifeSheet(sheet, known, labels, lifecycles, partIssues);
                issues.addAll(stamp(sheet.getName(), partIssues));
                continue;
            }
            TypeRef bound = uniqueSheetType(sheet.getName(), types);
            EquipmentSheet.Sheet prepared = bound == null ? sheet : withSheetType(sheet, bound, labels, issues);
            if (prepared.getRecords().isEmpty()) {
                continue;
            }
            anyData = true;
            Plan part = plan(prepared, types, statuses, known, defaultStatus, labels, people, seenKeys, service, kinds, life);
            issues.addAll(stamp(sheet.getName(), part.getIssues()));
            changes.addAll(part.getChanges());
            lifecycles.addAll(part.getLifecycles());
            for (int i = 0; i < part.getChanges().size(); i++) {
                Change change = part.getChanges().get(i);
                if (change.getExistingId() == null && !TniText.shortHost(change.getName()).isEmpty()) {
                    known.add(new Node(made[0]--, change.getParentId(), change.getName(), "", false, change.getTypeKey()));
                }
            }
        }
        if (!anyData && issues.isEmpty()) {
            issues.add(new Issue(1, "asset-tree.error.import.empty", ""));
        }
        return new Plan(changes, issues, services, lifecycles);
    }

    public static Plan plan(EquipmentSheet.Sheet sheet, List<TypeRef> types, List<Named> statuses, List<Node> nodes,
                            String defaultStatus, Map<String, String> labels, Directory people,
                            Map<String, Integer> seenKeys) {
        return plan(sheet, types, statuses, nodes, defaultStatus, labels, people, seenKeys, false, null);
    }

    public static Plan plan(EquipmentSheet.Sheet sheet, List<TypeRef> types, List<Named> statuses, List<Node> nodes,
                            String defaultStatus, Map<String, String> labels, Directory people,
                            Map<String, Integer> seenKeys, boolean service, List<Named> kinds) {
        return plan(sheet, types, statuses, nodes, defaultStatus, labels, people, seenKeys, service, kinds, false);
    }

    public static Plan plan(EquipmentSheet.Sheet sheet, List<TypeRef> types, List<Named> statuses, List<Node> nodes,
                            String defaultStatus, Map<String, String> labels, Directory people,
                            Map<String, Integer> seenKeys, boolean service, List<Named> kinds, boolean life) {
        List<Change> changes = new ArrayList<Change>();
        List<Issue> issues = new ArrayList<Issue>();
        if (sheet == null || sheet.getHeaders().isEmpty()) {
            issues.add(new Issue(1, "asset-tree.error.import.header", ""));
            return new Plan(changes, issues);
        }
        if (sheet.getRecords().size() > MAX_ROWS) {
            issues.add(new Issue(1, "asset-tree.error.import.limit", String.valueOf(MAX_ROWS)));
            return new Plan(changes, issues);
        }
        Map<String, String> roles = roles(labels);
        List<String> headers = sheet.getHeaders();
        int[] roleColumn = new int[ROLES.length];
        for (int i = 0; i < roleColumn.length; i++) {
            roleColumn[i] = -1;
        }
        List<Integer> fieldColumns = new ArrayList<Integer>();
        boolean headerBroken = false;
        for (int column = 0; column < headers.size(); column++) {
            String header = headers.get(column);
            String role = roles.get(norm(header));
            if (role != null) {
                int slot = roleSlot(role);
                if (slot >= 0 && roleColumn[slot] >= 0) {
                    issues.add(new Issue(1, "asset-tree.error.import.column", header));
                    headerBroken = true;
                } else if (slot >= 0) {
                    roleColumn[slot] = column;
                }
            } else if (header != null && !header.trim().isEmpty()) {
                fieldColumns.add(Integer.valueOf(column));
            }
        }
        if (roleColumn[1] < 0) {
            issues.add(new Issue(1, "asset-tree.error.import.header", ""));
            headerBroken = true;
        }
        if (headerBroken) {
            return new Plan(changes, issues);
        }
        if (sheet.getRecords().isEmpty()) {
            issues.add(new Issue(1, "asset-tree.error.import.empty", ""));
            return new Plan(changes, issues);
        }
        Map<String, Node> byKey = new LinkedHashMap<String, Node>();
        for (Node node : nodes) {
            if (node.getObjectKey() != null && !node.getObjectKey().isEmpty()) {
                byKey.put(norm(node.getObjectKey()), node);
            }
        }
        // One place index and one parent map for the whole sheet. Matching a path
        // used to scan every asset once per segment of every row.
        Map<String, List<Node>> places = indexPlaces(nodes);
        Map<Integer, Integer> parentById = parents(nodes);
        Map<String, Integer> keys = seenKeys == null ? new LinkedHashMap<String, Integer>() : seenKeys;
        List<TniSame.Card> hosts = new ArrayList<TniSame.Card>();
        Map<Integer, Node> byId = new HashMap<Integer, Node>();
        if (nodes != null) {
            for (int i = 0; i < nodes.size(); i++) {
                Node node = nodes.get(i);
                if (node == null) {
                    continue;
                }
                byId.put(Integer.valueOf(node.getId()), node);
                hosts.add(new TniSame.Card(node.getId(), node.getName(), node.isLocation(), "", ""));
            }
        }
        Set<Integer> taken = new HashSet<Integer>();
        int low = 0;
        if (nodes != null) {
            for (int i = 0; i < nodes.size(); i++) {
                Node node = nodes.get(i);
                if (node != null && node.getId() < low) {
                    low = node.getId();
                }
            }
        }
        int[] synthetic = new int[] { low - 1 };
        for (EquipmentSheet.Record record : sheet.getRecords()) {
            Change change = readRow(record, roleColumn, fieldColumns, headers, types, statuses, places, parentById, byKey,
                    keys, defaultStatus, people, issues, service, kinds, hosts, byId, taken, synthetic, life);
            if (change != null) {
                changes.add(change);
            }
        }
        return new Plan(changes, issues);
    }

    private static Change readRow(EquipmentSheet.Record record, int[] roleColumn, List<Integer> fieldColumns,
                                  List<String> headers, List<TypeRef> types, List<Named> statuses,
                                  Map<String, List<Node>> places, Map<Integer, Integer> parentById,
                                  Map<String, Node> byKey, Map<String, Integer> seenKeys, String defaultStatus,
                                  Directory people, List<Issue> issues, boolean service, List<Named> kinds,
                                  List<TniSame.Card> hosts, Map<Integer, Node> byId, Set<Integer> taken, int[] synthetic,
                                  boolean life) {
        int row = record.getRow();
        String key = text(record, roleColumn[0]);
        String name = text(record, roleColumn[1]);
        String typeName = text(record, roleColumn[2]);
        String statusName = text(record, roleColumn[3]);
        String place = text(record, roleColumn[4]);
        boolean custodianPresent = roleColumn[5] >= 0;
        String custodian = text(record, roleColumn[5]);
        boolean issuedPresent = roleColumn[6] >= 0;
        String issued = text(record, roleColumn[6]);
        String description = roleColumn[7] < 0 ? null : record.cell(roleColumn[7]);
        if (name.isEmpty() && key.isEmpty() && typeName.isEmpty() && place.isEmpty()
                && !(service && serviceTouched(record, roleColumn))) {
            return null;
        }
        if (name.isEmpty()) {
            issues.add(new Issue(row, "asset-tree.error.import.name", ""));
            return null;
        }
        if (name.length() > AssetValidator.MAX_NAME) {
            issues.add(new Issue(row, "asset-tree.error.name.length", ""));
            return null;
        }
        if (description != null && description.length() > AssetValidator.MAX_DESCRIPTION) {
            issues.add(new Issue(row, "asset-tree.error.description.length", ""));
            return null;
        }
        Node existing = null;
        boolean keepType = false;
        if (!key.isEmpty()) {
            String marker = norm(key);
            if (seenKeys.containsKey(marker)) {
                issues.add(new Issue(row, "asset-tree.error.import.key.duplicate", key));
                return null;
            }
            seenKeys.put(marker, Integer.valueOf(row));
            existing = byKey.get(marker);
            if (existing == null) {
                issues.add(new Issue(row, "asset-tree.error.import.key", key));
                return null;
            }
            if (existing.isLocation()) {
                issues.add(new Issue(row, "asset-tree.error.import.key.place", key));
                return null;
            }
        } else if (hosts != null) {
            int same = TniSame.pick(hosts, 0, name, "");
            if (same < 0 || (same > 0 && taken != null && taken.contains(Integer.valueOf(same)))) {
                issues.add(new Issue(row, "asset-tree.error.import.host", name));
                return null;
            }
            if (same > 0 && byId != null) {
                existing = byId.get(Integer.valueOf(same));
                keepType = existing != null && !existing.getTypeKey().isEmpty();
                if (existing != null && !existing.getObjectKey().isEmpty()) {
                    String marker = norm(existing.getObjectKey());
                    if (seenKeys.containsKey(marker)) {
                        issues.add(new Issue(row, "asset-tree.error.import.host", name));
                        return null;
                    }
                    seenKeys.put(marker, Integer.valueOf(row));
                }
            }
        }
        if (existing != null && taken != null) {
            taken.add(Integer.valueOf(existing.getId()));
        }
        TypeRef type = matchType(typeName, types);
        if (type == null) {
            issues.add(new Issue(row, typeName.isEmpty() ? "asset-tree.error.import.type" : "asset-tree.error.import.type", typeName));
            return null;
        }
        if (type.isLocation()) {
            issues.add(new Issue(row, "asset-tree.error.import.type.place", typeName));
            return null;
        }
        if (countTypes(typeName, types) > 1) {
            issues.add(new Issue(row, "asset-tree.error.import.type.ambiguous", typeName));
            return null;
        }
        if (keepType) {
            TypeRef kept = typeByKey(existing.getTypeKey(), types);
            if (kept != null && !kept.isLocation()) {
                type = kept;
            }
        }
        String statusKey = matchStatus(statusName, statuses, defaultStatus);
        if (statusKey == null) {
            issues.add(new Issue(row, "asset-tree.error.import.status", statusName));
            return null;
        }
        Integer parentId = matchPlace(place, places, row, issues);
        if (parentId == null) {
            return null;
        }
        if (existing != null && TreeLogic.wouldCycle(parentById, existing.getId(), parentId)) {
            issues.add(new Issue(row, "asset-tree.error.import.cycle", place));
            return null;
        }
        if (TreeLogic.depth(parentById, parentId.intValue()) + 1 >= TreeLogic.MAX_DEPTH) {
            issues.add(new Issue(row, "asset-tree.error.import.depth", place));
            return null;
        }
        String custodianKey = resolvePerson(custodian, people, row, issues);
        if (!custodian.isEmpty() && custodianKey == null) {
            return null;
        }
        String issuedKey = resolvePerson(issued, people, row, issues);
        if (!issued.isEmpty() && issuedKey == null) {
            return null;
        }
        List<Value> attributes = new ArrayList<Value>();
        Map<String, String> filled = new LinkedHashMap<String, String>();
        for (Integer column : fieldColumns) {
            String header = headers.get(column.intValue());
            String raw = record.cell(column.intValue());
            FieldRef field = matchField(header, type);
            if (field == null) {
                if (!raw.trim().isEmpty() && !keepType) {
                    issues.add(new Issue(row, "asset-tree.error.import.field", header));
                    return null;
                }
                continue;
            }
            String value = raw.trim();
            if (FieldKinds.NUMBER.equals(field.getKind()) && !value.isEmpty()) {
                value = value.replace(',', '.');
                if (!NUMBER.matcher(value).matches()) {
                    issues.add(new Issue(row, "asset-tree.error.import.number", field.getLabel()));
                    return null;
                }
            }
            if (FieldKinds.DATE.equals(field.getKind()) && !value.isEmpty()) {
                String canonical = FieldDates.canonical(value);
                if (canonical == null) {
                    issues.add(new Issue(row, "asset-tree.error.import.date", field.getLabel()));
                    return null;
                }
                value = canonical;
            }
            if (FieldChoices.handles(field.getKind()) && !value.isEmpty()) {
                String normalized = FieldChoices.canonicalValue(field.getKind(), field.getOptions(), value);
                if (normalized == null) {
                    issues.add(new Issue(row, FieldChoices.errorKey(field.getKind()), field.getLabel()));
                    return null;
                }
                value = normalized;
            }
            if (FieldKinds.USER.equals(field.getKind()) && !value.isEmpty()) {
                Person person = people == null ? Person.missing() : people.find(value);
                if (person == null || person.isMissing() || person.isBlank()) {
                    issues.add(new Issue(row, "asset-tree.error.import.user", value));
                    return null;
                }
                if (person.isAmbiguous()) {
                    issues.add(new Issue(row, "asset-tree.error.import.user.ambiguous", value));
                    return null;
                }
                value = person.getKey();
            }
            if (value.length() > AssetValidator.MAX_ATTR_VALUE) {
                issues.add(new Issue(row, "asset-tree.error.attribute.value.length", ""));
                return null;
            }
            filled.put(field.getKey(), value);
        }
        for (FieldRef field : type.getFields()) {
            if (!filled.containsKey(field.getKey())) {
                if (existing == null && field.isRequired()) {
                    issues.add(new Issue(row, "asset-tree.error.import.field.required", field.getLabel()));
                    return null;
                }
                continue;
            }
            String value = filled.get(field.getKey());
            if (field.isRequired() && value.trim().isEmpty()) {
                issues.add(new Issue(row, "asset-tree.error.import.field.required", field.getLabel()));
                return null;
            }
            attributes.add(new Value(field.getKey(), value));
        }
        Service wish = null;
        if (service && serviceTouched(record, roleColumn)) {
            wish = readService(record, roleColumn, headers, byKey, people, kinds, false, issues);
            if (wish == null) {
                return null;
            }
        }
        LifeBook.Change lifecycle = life ? readLifecycle(record, roleColumn, key) : null;
        if (existing == null && hosts != null && synthetic != null && !TniText.shortHost(name).isEmpty()) {
            hosts.add(new TniSame.Card(synthetic[0]--, name.trim(), false, "", ""));
        }
        return new Change(row, existing == null ? null : Integer.valueOf(existing.getId()), name.trim(),
                description, type.getKey(), statusKey, parentId.intValue(), custodianPresent, custodianKey,
                issuedPresent, issuedKey, attributes, wish, lifecycle);
    }

    private static Map<String, List<Node>> indexPlaces(List<Node> nodes) {
        Map<String, List<Node>> index = new HashMap<String, List<Node>>();
        if (nodes == null) {
            return index;
        }
        for (Node node : nodes) {
            if (node == null || !node.isLocation()) {
                continue;
            }
            String key = placeKey(node.getParentId(), node.getName());
            List<Node> bucket = index.get(key);
            if (bucket == null) {
                bucket = new ArrayList<Node>();
                index.put(key, bucket);
            }
            bucket.add(node);
        }
        return index;
    }

    private static String placeKey(Integer parentId, String name) {
        Integer parent = TreeLogic.normalizeParent(parentId);
        String parentKey = parent == null ? "" : String.valueOf(parent.intValue());
        return parentKey + "\n" + norm(name);
    }

    private static Integer matchPlace(String place, Map<String, List<Node>> places, int row, List<Issue> issues) {
        if (place.trim().isEmpty()) {
            issues.add(new Issue(row, "asset-tree.error.import.place.required", ""));
            return null;
        }
        String[] parts = place.split("/");
        Integer parent = null;
        for (String part : parts) {
            String name = part.trim();
            if (name.isEmpty()) {
                continue;
            }
            List<Node> matches = places == null ? null : places.get(placeKey(parent, name));
            if (matches == null || matches.isEmpty()) {
                issues.add(new Issue(row, "asset-tree.error.import.place", place.trim()));
                return null;
            }
            if (matches.size() > 1) {
                issues.add(new Issue(row, "asset-tree.error.import.place.ambiguous", name));
                return null;
            }
            parent = Integer.valueOf(matches.get(0).getId());
        }
        if (parent == null) {
            issues.add(new Issue(row, "asset-tree.error.import.place.required", ""));
            return null;
        }
        return parent;
    }

    private static TypeRef uniqueSheetType(String name, List<TypeRef> types) {
        if (name == null || name.trim().isEmpty() || types == null) {
            return null;
        }
        List<TypeRef> hits = new ArrayList<TypeRef>();
        for (TypeRef type : types) {
            if (type.isLocation()) {
                continue;
            }
            if (same(type.getLabel(), name) || same(type.getKey(), name) || same(WorkbookSheet.sheetTitle(type.getLabel()), name)) {
                hits.add(type);
            }
        }
        return hits.size() == 1 ? hits.get(0) : null;
    }

    private static EquipmentSheet.Sheet withSheetType(EquipmentSheet.Sheet sheet, TypeRef bound, Map<String, String> labels,
                                                      List<Issue> issues) {
        int typeColumn = columnFor(sheet.getHeaders(), labels, "type");
        List<EquipmentSheet.Record> records = new ArrayList<EquipmentSheet.Record>();
        for (EquipmentSheet.Record record : sheet.getRecords()) {
            String current = typeColumn < 0 ? "" : text(record, typeColumn);
            if (!current.isEmpty() && !same(current, bound.getLabel()) && !same(current, bound.getKey())) {
                issues.add(new Issue(record.getRow(), "asset-tree.error.import.sheet.type", current, sheet.getName()));
                continue;
            }
            if (current.isEmpty()) {
                records.add(typed(record, typeColumn, bound.getLabel()));
            } else {
                records.add(record);
            }
        }
        if (typeColumn >= 0) {
            return new EquipmentSheet.Sheet(sheet.getName(), sheet.getHeaders(), records);
        }
        List<String> headers = new ArrayList<String>(sheet.getHeaders());
        String typeHeader = labels == null ? null : labels.get("type");
        headers.add(typeHeader == null || typeHeader.trim().isEmpty() ? "Type" : typeHeader);
        return new EquipmentSheet.Sheet(sheet.getName(), headers, records);
    }

    private static EquipmentSheet.Record typed(EquipmentSheet.Record record, int column, String type) {
        boolean append = column < 0;
        int width = record.width();
        int size = append ? width + 1 : Math.max(width, column + 1);
        List<String> cells = new ArrayList<String>();
        for (int i = 0; i < size; i++) {
            if (append && i == width) {
                cells.add(type);
            } else if (!append && i == column) {
                cells.add(type);
            } else {
                cells.add(record.cell(i));
            }
        }
        return new EquipmentSheet.Record(record.getRow(), cells);
    }

    private static int columnFor(List<String> headers, Map<String, String> labels, String role) {
        Map<String, String> roles = roles(labels);
        if (headers == null) {
            return -1;
        }
        for (int column = 0; column < headers.size(); column++) {
            if (role.equals(roles.get(norm(headers.get(column))))) {
                return column;
            }
        }
        return -1;
    }

    private static List<Issue> stamp(String sheet, List<Issue> issues) {
        List<Issue> stamped = new ArrayList<Issue>();
        String name = sheet == null ? "" : sheet;
        for (Issue issue : issues) {
            if (issue.getSheet() != null && !issue.getSheet().isEmpty()) {
                stamped.add(issue);
            } else {
                stamped.add(new Issue(issue.getRow(), issue.getMessageKey(), issue.getArgument(), name));
            }
        }
        return stamped;
    }

    private static TypeRef typeByKey(String key, List<TypeRef> types) {
        if (key == null || key.isEmpty() || types == null) {
            return null;
        }
        for (int i = 0; i < types.size(); i++) {
            TypeRef type = types.get(i);
            if (type != null && key.equals(type.getKey())) {
                return type;
            }
        }
        return null;
    }

    private static TypeRef matchType(String typeName, List<TypeRef> types) {
        TypeRef found = null;
        for (TypeRef type : types) {
            if (type.isLocation()) {
                continue;
            }
            if (same(type.getLabel(), typeName) || same(type.getKey(), typeName)) {
                if (found == null) {
                    found = type;
                }
            }
        }
        if (found != null) {
            return found;
        }
        for (TypeRef type : types) {
            if (type.isLocation() && (same(type.getLabel(), typeName) || same(type.getKey(), typeName))) {
                return type;
            }
        }
        return null;
    }

    private static int countTypes(String typeName, List<TypeRef> types) {
        int count = 0;
        for (TypeRef type : types) {
            if (!type.isLocation() && (same(type.getLabel(), typeName) || same(type.getKey(), typeName))) {
                count++;
            }
        }
        return count;
    }

    private static String matchStatus(String statusName, List<Named> statuses, String defaultStatus) {
        if (statusName.isEmpty()) {
            for (Named status : statuses) {
                if (same(status.getKey(), defaultStatus)) {
                    return status.getKey();
                }
            }
            return statuses.isEmpty() ? null : statuses.get(0).getKey();
        }
        for (Named status : statuses) {
            if (same(status.getLabel(), statusName) || same(status.getKey(), statusName)) {
                return status.getKey();
            }
        }
        return null;
    }

    private static FieldRef matchField(String header, TypeRef type) {
        for (FieldRef field : type.getFields()) {
            if (same(field.getLabel(), header) || same(field.getKey(), header)) {
                return field;
            }
        }
        return null;
    }

    private static Map<Integer, Integer> parents(List<Node> nodes) {
        Map<Integer, Integer> map = new LinkedHashMap<Integer, Integer>();
        for (Node node : nodes) {
            map.put(Integer.valueOf(node.getId()), node.getParentId());
        }
        return map;
    }

    private static String resolvePerson(String raw, Directory people, int row, List<Issue> issues) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        Person person = people == null ? Person.missing() : people.find(raw);
        if (person == null || person.isMissing() || person.isBlank()) {
            issues.add(new Issue(row, "asset-tree.error.import.user", raw));
            return null;
        }
        if (person.isAmbiguous()) {
            issues.add(new Issue(row, "asset-tree.error.import.user.ambiguous", raw));
            return null;
        }
        return person.getKey();
    }

    private static String text(EquipmentSheet.Record record, int column) {
        if (column < 0) {
            return "";
        }
        String value = record.cell(column);
        return value == null ? "" : value.trim();
    }

    private static int roleSlot(String role) {
        for (int i = 0; i < ROLES.length; i++) {
            if (ROLES[i].equals(role)) {
                return i;
            }
        }
        return -1;
    }

    private static Map<String, String> roles(Map<String, String> labels) {
        Map<String, String> roles = new LinkedHashMap<String, String>();
        alias(roles, "key", "key", "ключ");
        alias(roles, "name", "name", "название", "имя");
        alias(roles, "type", "type", "тип");
        alias(roles, "status", "status", "статус");
        alias(roles, "place", "place", "площадка", "место", "расположение");
        alias(roles, "custodian", "custodian", "ответственный", "мол");
        alias(roles, "issued", "issued", "issued to", "кому выдано", "выдано");
        alias(roles, "description", "description", "описание");
        alias(roles, "serviceKind", "service: kind", "обслуживание: вид");
        alias(roles, "serviceName", "service: work", "обслуживание: работа");
        alias(roles, "serviceDate", "service: date", "обслуживание: дата");
        alias(roles, "serviceEvery", "service: interval", "обслуживание: интервал");
        alias(roles, "serviceUnit", "service: period", "обслуживание: период");
        alias(roles, "serviceWho", "service: assignee", "обслуживание: исполнитель");
        alias(roles, "lifePurchased", "lifecycle: purchase date", "жизненный цикл: дата покупки");
        alias(roles, "lifeSupplier", "lifecycle: supplier", "жизненный цикл: поставщик");
        alias(roles, "lifeDocument", "lifecycle: document", "жизненный цикл: документ");
        alias(roles, "lifePrice", "lifecycle: cost", "жизненный цикл: цена");
        alias(roles, "lifeCurrency", "lifecycle: currency", "жизненный цикл: валюта");
        alias(roles, "lifeCommissioned", "lifecycle: commissioning", "жизненный цикл: дата ввода");
        alias(roles, "lifeWarranty", "lifecycle: warranty until", "жизненный цикл: гарантия до");
        alias(roles, "lifeWarrantyService", "lifecycle: warranty service", "жизненный цикл: гарантийный сервис");
        alias(roles, "lifeContractor", "lifecycle: contractor", "жизненный цикл: подрядчик");
        alias(roles, "lifeContractNo", "lifecycle: contract number", "жизненный цикл: номер договора");
        alias(roles, "lifeContractFrom", "lifecycle: contract starts", "жизненный цикл: начало договора");
        alias(roles, "lifeContractUntil", "lifecycle: contract ends", "жизненный цикл: окончание договора");
        alias(roles, "lifeMonths", "lifecycle: useful life", "жизненный цикл: срок службы");
        alias(roles, "lifeResidual", "lifecycle: residual value", "жизненный цикл: остаточная стоимость");
        if (labels != null) {
            for (Map.Entry<String, String> entry : labels.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                    roles.put(norm(entry.getValue()), entry.getKey());
                }
            }
        }
        return roles;
    }

    private static void alias(Map<String, String> roles, String role, String... names) {
        for (String name : names) {
            roles.put(norm(name), role);
        }
    }

    public static String norm(String value) {
        if (value == null) {
            return "";
        }
        String text = value.trim().toLowerCase(Locale.ROOT).replace('ё', 'е');
        StringBuilder builder = new StringBuilder();
        boolean space = false;
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (Character.isWhitespace(current)) {
                space = builder.length() > 0;
                continue;
            }
            if (space) {
                builder.append(' ');
                space = false;
            }
            builder.append(current);
        }
        return builder.toString();
    }

    private static boolean same(String left, String right) {
        return norm(left).equals(norm(right));
    }

    private static boolean lifeSheet(EquipmentSheet.Sheet sheet, Map<String, String> labels) {
        Map<String, String> known = roles(labels);
        boolean life = false;
        boolean name = false;
        boolean key = false;
        List<String> headers = sheet.getHeaders();
        for (int i = 0; i < headers.size(); i++) {
            String role = known.get(norm(headers.get(i)));
            if (role != null && role.startsWith("life")) {
                life = true;
            }
            if ("name".equals(role)) {
                name = true;
            }
            if ("key".equals(role)) {
                key = true;
            }
        }
        return life && key && !name;
    }

    private static void readLifeSheet(EquipmentSheet.Sheet sheet, List<Node> nodes, Map<String, String> labels,
                                      List<LifeBook.Change> lifecycles, List<Issue> issues) {
        int[] roleColumn = roleColumns(sheet.getHeaders(), labels, issues);
        if (roleColumn == null) {
            return;
        }
        if (roleColumn[ROLE_KEY] < 0) {
            issues.add(new Issue(1, "asset-tree.error.import.header", ""));
            return;
        }
        Map<String, Node> byKey = new LinkedHashMap<String, Node>();
        if (nodes != null) {
            for (Node node : nodes) {
                if (node.getObjectKey() != null && !node.getObjectKey().isEmpty()) {
                    byKey.put(norm(node.getObjectKey()), node);
                }
            }
        }
        for (EquipmentSheet.Record record : sheet.getRecords()) {
            String key = text(record, roleColumn[ROLE_KEY]);
            LifeBook.Change change = readLifecycle(record, roleColumn, key);
            if (change == null) {
                continue;
            }
            if (key.isEmpty() || byKey.get(norm(key)) == null || byKey.get(norm(key)).isLocation()) {
                issues.add(new Issue(record.getRow(), "asset-tree.error.import.key", key));
                continue;
            }
            lifecycles.add(change);
        }
    }

    private static LifeBook.Change readLifecycle(EquipmentSheet.Record record, int[] roleColumn, String key) {
        Map<String, String> fields = new LinkedHashMap<String, String>();
        for (int i = 0; i < LIFE_FIELDS.length; i++) {
            int column = LIFE_FIRST + i;
            if (column >= roleColumn.length) {
                break;
            }
            String value = text(record, roleColumn[column]);
            if (!value.isEmpty()) {
                fields.put(LIFE_FIELDS[i], value);
            }
        }
        if (fields.isEmpty()) {
            return null;
        }
        return new LifeBook.Change(record.getRow(), key == null ? "" : key, fields);
    }

    private static boolean serviceSheet(EquipmentSheet.Sheet sheet, Map<String, String> labels) {
        Map<String, String> known = roles(labels);
        boolean kind = false;
        boolean name = false;
        List<String> headers = sheet.getHeaders();
        for (int i = 0; i < headers.size(); i++) {
            String role = known.get(norm(headers.get(i)));
            if ("serviceKind".equals(role)) {
                kind = true;
            }
            if ("name".equals(role)) {
                name = true;
            }
        }
        return kind && !name;
    }

    private static void readServiceSheet(EquipmentSheet.Sheet sheet, List<Node> nodes, Directory people,
                                         List<Named> kinds, Map<String, String> labels, List<Service> services,
                                         List<Issue> issues) {
        int[] roleColumn = roleColumns(sheet.getHeaders(), labels, issues);
        if (roleColumn == null) {
            return;
        }
        if (roleColumn[ROLE_KEY] < 0 || roleColumn[ROLE_SERVICE_KIND] < 0) {
            issues.add(new Issue(1, "asset-tree.error.import.header", ""));
            return;
        }
        Map<String, Node> byKey = new LinkedHashMap<String, Node>();
        if (nodes != null) {
            for (Node node : nodes) {
                if (node.getObjectKey() != null && !node.getObjectKey().isEmpty()) {
                    byKey.put(norm(node.getObjectKey()), node);
                }
            }
        }
        for (EquipmentSheet.Record record : sheet.getRecords()) {
            if (!serviceTouched(record, roleColumn)) {
                continue;
            }
            Service wish = readService(record, roleColumn, sheet.getHeaders(), byKey, people, kinds, true, issues);
            if (wish != null) {
                services.add(new Service(wish.getRow(), sheet.getName(), wish.getObjectKey(), wish.getKind(),
                        wish.getName(), wish.getLastDone(), wish.getEveryCount(), wish.getEveryUnit(), wish.getAssigneeKey()));
            }
        }
    }

    private static int[] roleColumns(List<String> headers, Map<String, String> labels, List<Issue> issues) {
        int[] roleColumn = new int[ROLES.length];
        for (int i = 0; i < roleColumn.length; i++) {
            roleColumn[i] = -1;
        }
        Map<String, String> known = roles(labels);
        boolean broken = false;
        for (int column = 0; column < headers.size(); column++) {
            String header = headers.get(column);
            String role = known.get(norm(header));
            int slot = role == null ? -1 : roleSlot(role);
            if (slot < 0) {
                continue;
            }
            if (roleColumn[slot] >= 0) {
                issues.add(new Issue(1, "asset-tree.error.import.column", header));
                broken = true;
            } else {
                roleColumn[slot] = column;
            }
        }
        return broken ? null : roleColumn;
    }

    private static boolean serviceTouched(EquipmentSheet.Record record, int[] roleColumn) {
        return !text(record, roleColumn[ROLE_SERVICE_KIND]).isEmpty()
                || !text(record, roleColumn[ROLE_SERVICE_NAME]).isEmpty()
                || !text(record, roleColumn[ROLE_SERVICE_DATE]).isEmpty()
                || !text(record, roleColumn[ROLE_SERVICE_EVERY]).isEmpty()
                || !text(record, roleColumn[ROLE_SERVICE_UNIT]).isEmpty()
                || !text(record, roleColumn[ROLE_SERVICE_WHO]).isEmpty();
    }

    /**
     * Reads one maintenance wish. Returns null when the row is incomplete;
     * the reason is already in {@code issues}. A sheet row also needs an
     * existing object key. An object row leaves the key empty.
     */
    private static Service readService(EquipmentSheet.Record record, int[] roleColumn, List<String> headers,
                                       Map<String, Node> byKey, Directory people, List<Named> kinds,
                                       boolean requireKey, List<Issue> issues) {
        int row = record.getRow();
        String kindRaw = text(record, roleColumn[ROLE_SERVICE_KIND]);
        String nameRaw = text(record, roleColumn[ROLE_SERVICE_NAME]);
        String dateRaw = text(record, roleColumn[ROLE_SERVICE_DATE]);
        String everyRaw = text(record, roleColumn[ROLE_SERVICE_EVERY]);
        String unitRaw = text(record, roleColumn[ROLE_SERVICE_UNIT]);
        String whoRaw = text(record, roleColumn[ROLE_SERVICE_WHO]);
        String objectKey = "";
        if (requireKey) {
            String key = text(record, roleColumn[ROLE_KEY]);
            if (key.isEmpty()) {
                issues.add(new Issue(row, "asset-tree.error.import.service.key", key));
                return null;
            }
            Node node = byKey.get(norm(key));
            if (node == null) {
                issues.add(new Issue(row, "asset-tree.error.import.service.key", key));
                return null;
            }
            if (node.isLocation()) {
                issues.add(new Issue(row, "asset-tree.error.import.key.place", key));
                return null;
            }
            objectKey = node.getObjectKey();
        }
        if (kindRaw.isEmpty() || dateRaw.isEmpty() || unitRaw.isEmpty()) {
            issues.add(new Issue(row, "asset-tree.error.import.service", ""));
            return null;
        }
        String kind = serviceKind(kindRaw, kinds);
        if (kind.isEmpty()) {
            issues.add(new Issue(row, "asset-tree.error.import.service.kind", kindRaw));
            return null;
        }
        String unit = serviceUnit(unitRaw);
        if (unit.isEmpty()) {
            issues.add(new Issue(row, "asset-tree.error.import.service.unit", unitRaw));
            return null;
        }
        if (everyRaw.isEmpty() && !"once".equals(unit)) {
            issues.add(new Issue(row, "asset-tree.error.import.service", ""));
            return null;
        }
        int count;
        if (everyRaw.isEmpty()) {
            count = 1;
        } else {
            try {
                count = Integer.parseInt(everyRaw);
            } catch (NumberFormatException ex) {
                issues.add(new Issue(row, "asset-tree.error.import.number", headerAt(headers, roleColumn[ROLE_SERVICE_EVERY])));
                return null;
            }
        }
        if (!ServiceDue.countAllowed(count, unit)) {
            issues.add(new Issue(row, "asset-tree.error.import.service", ""));
            return null;
        }
        String lastDone = FieldDates.canonical(dateRaw);
        if (lastDone == null) {
            issues.add(new Issue(row, "asset-tree.error.import.date", headerAt(headers, roleColumn[ROLE_SERVICE_DATE])));
            return null;
        }
        if (lastDone.isEmpty()) {
            issues.add(new Issue(row, "asset-tree.error.import.service", ""));
            return null;
        }
        String work = nameRaw.isEmpty() ? kindRaw : nameRaw;
        if (work.isEmpty() || work.length() > 80) {
            issues.add(new Issue(row, "asset-tree.error.import.service", ""));
            return null;
        }
        String assignee = "";
        if (!whoRaw.isEmpty()) {
            assignee = resolvePerson(whoRaw, people, row, issues);
            if (assignee == null) {
                return null;
            }
        }
        return new Service(row, "", objectKey, kind, work, lastDone, count, unit, assignee);
    }

    private static String headerAt(List<String> headers, int column) {
        if (headers == null || column < 0 || column >= headers.size() || headers.get(column) == null) {
            return "";
        }
        return headers.get(column);
    }

    static String serviceKind(String raw, List<Named> kinds) {
        String marker = norm(raw);
        if (marker.isEmpty()) {
            return "";
        }
        if (kinds != null) {
            for (Named kind : kinds) {
                if (kind == null) {
                    continue;
                }
                if (marker.equals(norm(kind.getKey())) || marker.equals(norm(kind.getLabel()))) {
                    return kind.getKey();
                }
            }
        }
        String alias = kindAlias(marker);
        if (alias.isEmpty()) {
            return "";
        }
        if (kinds == null || kinds.isEmpty()) {
            return alias;
        }
        for (Named kind : kinds) {
            if (kind != null && alias.equals(kind.getKey())) {
                return alias;
            }
        }
        return "";
    }

    private static String kindAlias(String marker) {
        if (marker.equals("to") || marker.equals("то") || marker.equals("maintenance")) {
            return "to";
        }
        if (marker.equals("kts") || marker.equals("ктс") || marker.equals("inspection")) {
            return "kts";
        }
        if (marker.equals("verify") || marker.equals("поверка") || marker.equals("verification")) {
            return "verify";
        }
        if (marker.equals("staff") || marker.equals("work") || marker.equals("работа")) {
            return "staff";
        }
        if (marker.equals("event") || marker.equals("мероприятие")) {
            return "event";
        }
        if (marker.equals("update") || marker.equals("обновление")) {
            return "update";
        }
        return "";
    }

    static String serviceUnit(String raw) {
        String marker = norm(raw);
        while (marker.endsWith(".")) {
            marker = marker.substring(0, marker.length() - 1).trim();
        }
        if (marker.isEmpty()) {
            return "";
        }
        if (oneOf(marker, "once", "разово", "разовая", "разовое", "без повтора", "does not repeat", "one time")) {
            return "once";
        }
        if (oneOf(marker, "day", "days", "дн", "день", "дня", "дней", "сут", "сутки")) {
            return "day";
        }
        if (oneOf(marker, "week", "weeks", "wk", "wks", "нед", "неделя", "недели", "недель")) {
            return "week";
        }
        if (oneOf(marker, "month", "months", "мес", "месяц", "месяца", "месяцев")) {
            return "month";
        }
        if (oneOf(marker, "year", "years", "yr", "yrs", "г", "год", "года", "лет")) {
            return "year";
        }
        return "";
    }

    private static boolean oneOf(String marker, String... words) {
        for (String word : words) {
            if (marker.equals(word)) {
                return true;
            }
        }
        return false;
    }

    public static int countRows(List<EquipmentSheet.Sheet> sheets) {
        int rows = 0;
        if (sheets == null) {
            return 0;
        }
        for (EquipmentSheet.Sheet sheet : sheets) {
            if (sheet != null && sheet.getRecords() != null) {
                rows += sheet.getRecords().size();
            }
        }
        return rows;
    }

    /**
     * A slice of data rows, headers kept on every sheet that still has rows.
     * {@code next} is the offset of the following slice. {@code done} is true
     * when this slice reaches the end.
     */
    public static Window window(List<EquipmentSheet.Sheet> sheets, int offset, int limit) {
        int from = offset < 0 ? 0 : offset;
        int size = limit <= 0 ? CHUNK_ROWS : limit;
        List<EquipmentSheet.Sheet> source = sheets == null ? new ArrayList<EquipmentSheet.Sheet>() : sheets;
        int total = countRows(source);
        List<EquipmentSheet.Sheet> sliced = new ArrayList<EquipmentSheet.Sheet>();
        int seen = 0;
        int taken = 0;
        for (EquipmentSheet.Sheet sheet : source) {
            if (sheet == null || sheet.getRecords() == null) {
                continue;
            }
            List<EquipmentSheet.Record> records = new ArrayList<EquipmentSheet.Record>();
            for (EquipmentSheet.Record record : sheet.getRecords()) {
                if (seen >= from && taken < size) {
                    records.add(copyRecord(record));
                    taken++;
                }
                seen++;
            }
            if (!records.isEmpty()) {
                sliced.add(new EquipmentSheet.Sheet(sheet.getName(), new ArrayList<String>(sheet.getHeaders()), records));
            }
        }
        int next = from + taken;
        return new Window(sliced, total, next, next >= total);
    }

    public static Integer findPlace(String place, List<Node> nodes, int row, List<Issue> issues) {
        return matchPlace(place, indexPlaces(nodes), row, issues);
    }

    private static EquipmentSheet.Record copyRecord(EquipmentSheet.Record record) {
        List<String> cells = new ArrayList<String>();
        int width = record == null ? 0 : record.width();
        for (int i = 0; i < width; i++) {
            cells.add(record.cell(i));
        }
        return new EquipmentSheet.Record(record == null ? 0 : record.getRow(), cells);
    }

    public static final class Window {
        private final List<EquipmentSheet.Sheet> sheets;
        private final int total;
        private final int next;
        private final boolean done;

        Window(List<EquipmentSheet.Sheet> sheets, int total, int next, boolean done) {
            this.sheets = sheets;
            this.total = total;
            this.next = next;
            this.done = done;
        }

        public List<EquipmentSheet.Sheet> getSheets() {
            return sheets;
        }

        public int getTotal() {
            return total;
        }

        public int getNext() {
            return next;
        }

        public boolean isDone() {
            return done;
        }
    }
}
