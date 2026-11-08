package com.assetstree.jira.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Request types a customer may raise for one asset, and the field values
 * that go to Service Desk. The asset field is filled here and is not shown.
 */
public final class PortalRequestForm {
    private final List<Type> types;
    private final String failureKey;
    private final String failureArg;

    private PortalRequestForm(List<Type> types, String failureKey, String failureArg) {
        this.types = types == null ? new ArrayList<Type>() : types;
        this.failureKey = failureKey;
        this.failureArg = failureArg == null ? "" : failureArg;
    }

    public static PortalRequestForm ready(List<Type> types) {
        return new PortalRequestForm(types, null, "");
    }

    public static PortalRequestForm failed(String key, String arg) {
        return new PortalRequestForm(Collections.<Type>emptyList(), key, arg);
    }

    public List<Type> getTypes() {
        return types;
    }

    public String getFailureKey() {
        return failureKey;
    }

    public String getFailureArg() {
        return failureArg;
    }

    public static Outcome compose(PortalRequestForm form, String typeId, Map<String, String> input, String assetId) {
        if (form == null) {
            return Outcome.problem("asset-tree.error.portalDesk", "");
        }
        if (form.failureKey != null) {
            return Outcome.problem(form.failureKey, form.failureArg);
        }
        String wanted = typeId == null ? "" : typeId.trim();
        Type type = null;
        for (Type candidate : form.types) {
            if (candidate.getId().equals(wanted)) {
                type = candidate;
                break;
            }
        }
        if (type == null) {
            return Outcome.problem("asset-tree.error.portalRequest.type", "");
        }
        Map<String, String> incoming = input == null ? Collections.<String, String>emptyMap() : input;
        Map<String, String> values = new LinkedHashMap<String, String>();
        for (Field field : type.getFields()) {
            String raw = incoming.get(field.getId());
            String value = raw == null ? "" : raw.trim();
            if (field.isRequired() && value.isEmpty()) {
                return Outcome.problem("asset-tree.error.field.required", field.getName());
            }
            if (!value.isEmpty() && "select".equals(field.getKind()) && !field.allows(value)) {
                return Outcome.problem("asset-tree.error.portalRequest.option", field.getName());
            }
            if (value.isEmpty()) {
                continue;
            }
            int limit = "summary".equals(field.getId()) ? 255 : 4000;
            if (value.length() > limit) {
                value = value.substring(0, limit);
            }
            values.put(field.getId(), value);
        }
        String assetValue = assetId == null ? "" : assetId.trim();
        for (String fieldId : type.getAssetFieldIds()) {
            if (fieldId != null && !fieldId.isEmpty() && !assetValue.isEmpty()) {
                values.put(fieldId, assetValue);
            }
        }
        return Outcome.ready(wanted, values);
    }

    public static final class Type {
        private final String id;
        private final String name;
        private final String description;
        private final List<Field> fields;
        private final List<String> assetFieldIds;

        public Type(String id, String name, String description, List<Field> fields, List<String> assetFieldIds) {
            this.id = id == null ? "" : id;
            this.name = name == null ? "" : name;
            this.description = description == null ? "" : description;
            this.fields = fields == null ? new ArrayList<Field>() : new ArrayList<Field>(fields);
            this.assetFieldIds = assetFieldIds == null ? new ArrayList<String>() : new ArrayList<String>(assetFieldIds);
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public List<Field> getFields() {
            return fields;
        }

        public List<String> getAssetFieldIds() {
            return assetFieldIds;
        }
    }

    public static final class Field {
        private final String id;
        private final String name;
        private final boolean required;
        private final String kind;
        private final List<Option> options;

        public Field(String id, String name, boolean required, String kind, List<Option> options) {
            this.id = id == null ? "" : id;
            this.name = name == null ? "" : name;
            this.required = required;
            this.kind = kind == null ? "text" : kind;
            this.options = options == null ? new ArrayList<Option>() : new ArrayList<Option>(options);
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public boolean isRequired() {
            return required;
        }

        public String getKind() {
            return kind;
        }

        public List<Option> getOptions() {
            return options;
        }

        public boolean allows(String value) {
            for (Option option : options) {
                if (option.getValue().equals(value)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static final class Option {
        private final String value;
        private final String label;

        public Option(String value, String label) {
            this.value = value == null ? "" : value;
            this.label = label == null || label.isEmpty() ? this.value : label;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }
    }

    public static final class Outcome {
        private final String typeId;
        private final Map<String, String> values;
        private final String failureKey;
        private final String failureArg;

        private Outcome(String typeId, Map<String, String> values, String failureKey, String failureArg) {
            this.typeId = typeId == null ? "" : typeId;
            this.values = values == null ? new LinkedHashMap<String, String>() : values;
            this.failureKey = failureKey;
            this.failureArg = failureArg == null ? "" : failureArg;
        }

        static Outcome ready(String typeId, Map<String, String> values) {
            return new Outcome(typeId, values, null, "");
        }

        static Outcome problem(String key, String arg) {
            return new Outcome("", new LinkedHashMap<String, String>(), key, arg);
        }

        public String getTypeId() {
            return typeId;
        }

        public Map<String, String> getValues() {
            return values;
        }

        public String getFailureKey() {
            return failureKey;
        }

        public String getFailureArg() {
            return failureArg;
        }
    }
}
