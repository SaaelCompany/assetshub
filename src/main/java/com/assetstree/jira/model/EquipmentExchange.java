package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Turns a spreadsheet of equipment into creates and updates.
 * A blank key creates an object. A key already in the project updates it.
 * Places and types must already exist: the file does not invent the tree.
 */
public final class EquipmentExchange {
    public static final int MAX_ROWS = 2000;

    private static final Pattern NUMBER = Pattern.compile("^-?\\d{1,12}(\\.\\d{1,4})?$");
    private static final String[] ROLES = {"key", "name", "type", "status", "place", "custodian", "description"};

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

        public Node(int id, Integer parentId, String name, String objectKey, boolean location) {
            this.id = id;
            this.parentId = parentId;
            this.name = name;
            this.objectKey = objectKey;
            this.location = location;
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
        private final List<Value> attributes;

        Change(int row, Integer existingId, String name, String description, String typeKey, String statusKey,
               int parentId, boolean custodianPresent, String custodianKey, List<Value> attributes) {
            this.row = row;
            this.existingId = existingId;
            this.name = name;
            this.description = description;
            this.typeKey = typeKey;
            this.statusKey = statusKey;
            this.parentId = parentId;
            this.custodianPresent = custodianPresent;
            this.custodianKey = custodianKey;
            this.attributes = attributes;
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

        public List<Value> getAttributes() {
            return attributes;
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

        Plan(List<Change> changes, List<Issue> issues) {
            this.changes = changes;
            this.issues = issues;
        }

        public List<Change> getChanges() {
            return changes;
        }

        public List<Issue> getIssues() {
            return issues;
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
        List<Change> changes = new ArrayList<Change>();
        List<Issue> issues = new ArrayList<Issue>();
        if (sheets == null || sheets.isEmpty()) {
            issues.add(new Issue(1, "asset-tree.error.import.empty", ""));
            return new Plan(changes, issues);
        }
        int rows = 0;
        for (EquipmentSheet.Sheet sheet : sheets) {
            if (sheet != null) {
                rows += sheet.getRecords().size();
            }
        }
        if (rows > MAX_ROWS) {
            issues.add(new Issue(1, "asset-tree.error.import.limit", String.valueOf(MAX_ROWS)));
            return new Plan(changes, issues);
        }
        Map<String, Integer> seenKeys = new LinkedHashMap<String, Integer>();
        boolean anyData = false;
        for (EquipmentSheet.Sheet sheet : sheets) {
            if (sheet == null || (sheet.getHeaders().isEmpty() && sheet.getRecords().isEmpty())) {
                continue;
            }
            TypeRef bound = uniqueSheetType(sheet.getName(), types);
            EquipmentSheet.Sheet prepared = bound == null ? sheet : withSheetType(sheet, bound, labels, issues);
            if (prepared.getRecords().isEmpty()) {
                continue;
            }
            anyData = true;
            Plan part = plan(prepared, types, statuses, nodes, defaultStatus, labels, people, seenKeys);
            issues.addAll(stamp(sheet.getName(), part.getIssues()));
            changes.addAll(part.getChanges());
        }
        if (!anyData && issues.isEmpty()) {
            issues.add(new Issue(1, "asset-tree.error.import.empty", ""));
        }
        return new Plan(changes, issues);
    }

    public static Plan plan(EquipmentSheet.Sheet sheet, List<TypeRef> types, List<Named> statuses, List<Node> nodes,
                            String defaultStatus, Map<String, String> labels, Directory people,
                            Map<String, Integer> seenKeys) {
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
        Map<String, Integer> keys = seenKeys == null ? new LinkedHashMap<String, Integer>() : seenKeys;
        for (EquipmentSheet.Record record : sheet.getRecords()) {
            Change change = readRow(record, roleColumn, fieldColumns, headers, types, statuses, nodes, byKey,
                    keys, defaultStatus, people, issues);
            if (change != null) {
                changes.add(change);
            }
        }
        return new Plan(changes, issues);
    }

    private static Change readRow(EquipmentSheet.Record record, int[] roleColumn, List<Integer> fieldColumns,
                                  List<String> headers, List<TypeRef> types, List<Named> statuses, List<Node> nodes,
                                  Map<String, Node> byKey, Map<String, Integer> seenKeys, String defaultStatus,
                                  Directory people, List<Issue> issues) {
        int row = record.getRow();
        String key = text(record, roleColumn[0]);
        String name = text(record, roleColumn[1]);
        String typeName = text(record, roleColumn[2]);
        String statusName = text(record, roleColumn[3]);
        String place = text(record, roleColumn[4]);
        boolean custodianPresent = roleColumn[5] >= 0;
        String custodian = text(record, roleColumn[5]);
        String description = roleColumn[6] < 0 ? null : record.cell(roleColumn[6]);
        if (name.isEmpty() && key.isEmpty() && typeName.isEmpty() && place.isEmpty()) {
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
        String statusKey = matchStatus(statusName, statuses, defaultStatus);
        if (statusKey == null) {
            issues.add(new Issue(row, "asset-tree.error.import.status", statusName));
            return null;
        }
        Integer parentId = matchPlace(place, nodes, row, issues);
        if (parentId == null) {
            return null;
        }
        if (existing != null && TreeLogic.wouldCycle(parents(nodes), existing.getId(), parentId)) {
            issues.add(new Issue(row, "asset-tree.error.import.cycle", place));
            return null;
        }
        if (TreeLogic.depth(parents(nodes), parentId.intValue()) + 1 >= TreeLogic.MAX_DEPTH) {
            issues.add(new Issue(row, "asset-tree.error.import.depth", place));
            return null;
        }
        String custodianKey = null;
        if (!custodian.isEmpty()) {
            Person person = people == null ? Person.missing() : people.find(custodian);
            if (person == null || person.isMissing() || person.isBlank()) {
                issues.add(new Issue(row, "asset-tree.error.import.user", custodian));
                return null;
            }
            if (person.isAmbiguous()) {
                issues.add(new Issue(row, "asset-tree.error.import.user.ambiguous", custodian));
                return null;
            }
            custodianKey = person.getKey();
        }
        List<Value> attributes = new ArrayList<Value>();
        Map<String, String> filled = new LinkedHashMap<String, String>();
        for (Integer column : fieldColumns) {
            String header = headers.get(column.intValue());
            String raw = record.cell(column.intValue());
            FieldRef field = matchField(header, type);
            if (field == null) {
                if (!raw.trim().isEmpty()) {
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
        return new Change(row, existing == null ? null : Integer.valueOf(existing.getId()), name.trim(),
                description, type.getKey(), statusKey, parentId.intValue(), custodianPresent, custodianKey, attributes);
    }

    private static Integer matchPlace(String place, List<Node> nodes, int row, List<Issue> issues) {
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
            List<Node> matches = new ArrayList<Node>();
            for (Node node : nodes) {
                if (!node.isLocation() || !same(node.getName(), name)) {
                    continue;
                }
                if ((parent == null && node.getParentId() == null) || (parent != null && parent.equals(node.getParentId()))) {
                    matches.add(node);
                }
            }
            if (matches.isEmpty()) {
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
        alias(roles, "description", "description", "описание");
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
}
