package com.assetstree.jira.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DefaultTypes {
    public static final class Seed {
        private final String key;
        private final String label;
        private final String color;
        private final int order;
        private final boolean location;

        public Seed(String key, String label, String color, int order, boolean location) {
            this.key = key;
            this.label = label;
            this.color = color;
            this.order = order;
            this.location = location;
        }

        public String getKey() {
            return key;
        }

        public String getLabel() {
            return label;
        }

        public String getColor() {
            return color;
        }

        public int getOrder() {
            return order;
        }

        public boolean isLocation() {
            return location;
        }
    }

    public static final class FieldSeed {
        private final String baseKey;
        private final String fieldKey;
        private final String labelKey;
        private final String kind;
        private final boolean required;
        private final int order;

        public FieldSeed(String baseKey, String fieldKey, String labelKey, String kind, boolean required, int order) {
            this.baseKey = baseKey;
            this.fieldKey = fieldKey;
            this.labelKey = labelKey;
            this.kind = kind;
            this.required = required;
            this.order = order;
        }

        public String getBaseKey() {
            return baseKey;
        }

        public String getFieldKey() {
            return fieldKey;
        }

        public String getLabelKey() {
            return labelKey;
        }

        public String getKind() {
            return kind;
        }

        public boolean isRequired() {
            return required;
        }

        public int getOrder() {
            return order;
        }
    }

    public static final List<Seed> ALL = Collections.unmodifiableList(Arrays.asList(
            new Seed("warehouse", "Warehouse", "#175CD3", 0, true),
            new Seed("branch", "Branch", "#0E7090", 1, true),
            new Seed("department", "Department", "#6554C0", 2, true),
            new Seed("equipment", "Equipment", "#0F6E56", 3, false)
    ));

    public static final List<FieldSeed> FIELDS = Collections.unmodifiableList(Arrays.asList(
            new FieldSeed("warehouse", "address", "asset-tree.field.address", FieldKinds.TEXT, true, 0),
            new FieldSeed("warehouse", "phone", "asset-tree.field.phone", FieldKinds.TEXT, false, 1),
            new FieldSeed("branch", "address", "asset-tree.field.address", FieldKinds.TEXT, true, 0),
            new FieldSeed("branch", "phone", "asset-tree.field.phone", FieldKinds.TEXT, false, 1),
            new FieldSeed("department", "phone", "asset-tree.field.phone", FieldKinds.TEXT, false, 0),
            new FieldSeed("equipment", "inventory", "asset-tree.field.inventory", FieldKinds.TEXT, false, 0),
            new FieldSeed("equipment", "serial", "asset-tree.field.serial", FieldKinds.TEXT, false, 1)
    ));

    public static final String[] PALETTE = new String[] {
            "#0052CC", "#00875A", "#6554C0", "#FF991F", "#DE350B", "#00A3BF", "#172B4D", "#36B37E"
    };

    private DefaultTypes() {
    }
}
