package com.assetstree.jira.model;

import com.assetstree.jira.dto.AssetTypeDto;
import com.assetstree.jira.dto.FieldDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One level: a subtype shows its parent's fields. The child's own key wins. */
public final class FieldInheritance {
    private FieldInheritance() {
    }

    public static void apply(List<AssetTypeDto> types) {
        if (types == null || types.isEmpty()) {
            return;
        }
        Map<String, AssetTypeDto> byKey = new LinkedHashMap<String, AssetTypeDto>();
        for (int i = 0; i < types.size(); i++) {
            AssetTypeDto type = types.get(i);
            if (type != null && type.getTypeKey() != null && !type.getTypeKey().isEmpty()) {
                byKey.put(type.getTypeKey(), type);
            }
        }
        for (int i = 0; i < types.size(); i++) {
            AssetTypeDto type = types.get(i);
            if (type == null || type.getParentTypeKey() == null || type.getParentTypeKey().trim().isEmpty()) {
                continue;
            }
            AssetTypeDto parent = byKey.get(type.getParentTypeKey().trim());
            if (parent == null || parent == type) {
                continue;
            }
            List<FieldDto> own = type.getFields() == null ? new ArrayList<FieldDto>() : type.getFields();
            List<FieldDto> parentFields = parent.getFields() == null
                    ? Collections.<FieldDto>emptyList() : parent.getFields();
            List<FieldDto> merged = new ArrayList<FieldDto>();
            int position = 0;
            for (int f = 0; f < parentFields.size(); f++) {
                FieldDto field = parentFields.get(f);
                if (field == null || field.isInherited() || field.getFieldKey() == null || field.getFieldKey().isEmpty()) {
                    continue;
                }
                if (owns(own, field.getFieldKey())) {
                    continue;
                }
                FieldDto copy = copyOf(field);
                copy.setInherited(true);
                String from = parent.getLabel();
                copy.setInheritedFrom(from == null || from.isEmpty() ? parent.getTypeKey() : from);
                copy.setPosition(position);
                position++;
                merged.add(copy);
            }
            for (int f = 0; f < own.size(); f++) {
                FieldDto field = own.get(f);
                if (field == null || field.isInherited()) {
                    continue;
                }
                field.setInherited(false);
                field.setPosition(position);
                position++;
                merged.add(field);
            }
            type.setFields(merged);
        }
    }

    private static boolean owns(List<FieldDto> fields, String key) {
        for (int i = 0; i < fields.size(); i++) {
            FieldDto field = fields.get(i);
            if (field != null && !field.isInherited() && key.equals(field.getFieldKey())) {
                return true;
            }
        }
        return false;
    }

    private static FieldDto copyOf(FieldDto field) {
        FieldDto copy = new FieldDto();
        copy.setFieldKey(field.getFieldKey());
        copy.setLabel(field.getLabel());
        copy.setKind(field.getKind());
        copy.setRequired(field.isRequired());
        copy.setShowOnIssue(field.isShowOnIssue());
        copy.setPosition(field.getPosition());
        List<String> options = field.getOptions() == null
                ? Collections.<String>emptyList() : field.getOptions();
        copy.setOptions(new ArrayList<String>(options));
        return copy;
    }
}
