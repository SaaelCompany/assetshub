package com.assetstree.jira.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * What one person may see inside a project.
 * An empty type list means every type. An empty field list means every field.
 * When several grants apply, the wider one wins: any unrestricted grant opens
 * the rest, and write is wider than read.
 */
public final class AccessScope {
    public static final String WRITE = "write";
    public static final String READ = "read";
    public static final String HIDE = "hide";

    private final boolean allTypes;
    private final Set<String> types;
    private final boolean allFields;
    private final Map<String, String> fields;

    private AccessScope(boolean allTypes, Set<String> types, boolean allFields, Map<String, String> fields) {
        this.allTypes = allTypes;
        this.types = types;
        this.allFields = allFields;
        this.fields = fields;
    }

    public static AccessScope all() {
        return new AccessScope(true, Collections.<String>emptySet(), true, Collections.<String, String>emptyMap());
    }

    public static final class Slice {
        private final String typeScope;
        private final String schemaName;
        private final String fieldScope;

        public Slice(String typeScope, String schemaName, String fieldScope) {
            this.typeScope = typeScope == null ? "" : typeScope;
            this.schemaName = schemaName == null ? "" : schemaName;
            this.fieldScope = fieldScope == null ? "" : fieldScope;
        }

        public String getTypeScope() {
            return typeScope;
        }

        public String getSchemaName() {
            return schemaName;
        }

        public String getFieldScope() {
            return fieldScope;
        }
    }

    public static AccessScope merge(List<Slice> grants, Map<String, Set<String>> schemas) {
        if (grants == null || grants.isEmpty()) {
            return all();
        }
        boolean allTypes = false;
        boolean allFields = false;
        Set<String> types = new LinkedHashSet<String>();
        Map<String, String> fields = new HashMap<String, String>();
        for (Slice grant : grants) {
            if (grant == null) {
                continue;
            }
            Set<String> typed = typeKeys(grant, schemas);
            if (typed == null) {
                allTypes = true;
            } else {
                types.addAll(typed);
            }
            Map<String, String> modes = fieldModes(grant.getFieldScope());
            if (modes == null) {
                allFields = true;
            } else {
                for (Map.Entry<String, String> entry : modes.entrySet()) {
                    String previous = fields.get(entry.getKey());
                    if (wider(entry.getValue(), previous)) {
                        fields.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        if (allTypes && allFields) {
            return all();
        }
        return new AccessScope(allTypes, types, allFields, fields);
    }

    public boolean allowsType(String typeKey, boolean location) {
        if (location || allTypes) {
            return true;
        }
        return typeKey != null && types.contains(typeKey);
    }

    public String fieldMode(String fieldKey) {
        if (allFields) {
            return WRITE;
        }
        if (fieldKey == null) {
            return HIDE;
        }
        String mode = fields.get(fieldKey);
        return mode == null ? HIDE : mode;
    }

    public boolean isAllTypes() {
        return allTypes;
    }

    public boolean isAllFields() {
        return allFields;
    }

    public Set<String> getTypes() {
        return types;
    }

    /** Null means every type. An empty set means no equipment type. */
    static Set<String> typeKeys(Slice grant, Map<String, Set<String>> schemas) {
        String schema = grant.getSchemaName().trim();
        if (!schema.isEmpty()) {
            if (schemas != null && schemas.containsKey(schema)) {
                Set<String> stored = schemas.get(schema);
                return stored == null ? new LinkedHashSet<String>() : new LinkedHashSet<String>(stored);
            }
            return new LinkedHashSet<String>();
        }
        String raw = grant.getTypeScope().trim();
        if (raw.isEmpty()) {
            return null;
        }
        Set<String> keys = new LinkedHashSet<String>();
        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length; i++) {
            String key = parts[i] == null ? "" : parts[i].trim();
            if (!key.isEmpty() && key.length() <= 64) {
                keys.add(key);
            }
        }
        return keys;
    }

    /** Null means every field. */
    static Map<String, String> fieldModes(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        Map<String, String> modes = new HashMap<String, String>();
        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i] == null ? "" : parts[i].trim();
            if (part.isEmpty()) {
                continue;
            }
            int colon = part.indexOf(':');
            String key = colon < 0 ? part : part.substring(0, colon).trim();
            String mode = colon < 0 ? WRITE : part.substring(colon + 1).trim().toLowerCase(Locale.ROOT);
            if (key.isEmpty() || key.length() > 64) {
                continue;
            }
            if (!WRITE.equals(mode) && !READ.equals(mode)) {
                mode = WRITE;
            }
            modes.put(key, mode);
        }
        return modes;
    }

    private static boolean wider(String next, String previous) {
        return rank(next) > rank(previous);
    }

    private static int rank(String mode) {
        if (WRITE.equals(mode)) {
            return 3;
        }
        if (READ.equals(mode)) {
            return 2;
        }
        if (HIDE.equals(mode)) {
            return 1;
        }
        return 0;
    }
}
