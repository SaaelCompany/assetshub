package com.assetstree.jira.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.assetstree.jira.ao.SchemeEntity;
import com.assetstree.jira.dto.SchemeDocumentDto;
import com.assetstree.jira.model.AssetException;
import net.java.ao.DBParam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class SchemeDocuments {
    public static final int MAX_SCHEMES = 40;
    public static final int MAX_BODY = 1500000;

    private SchemeDocuments() {
    }

    public static String nameOf(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            return "Scheme";
        }
        if (name.length() > 80) {
            return name.substring(0, 80);
        }
        return name;
    }

    public static String bodyOf(String raw) {
        if (raw == null) {
            throw new AssetException(400, "asset-tree.error.scheme");
        }
        String body = raw.trim();
        if (body.isEmpty() || body.charAt(0) != '{' || body.length() > MAX_BODY) {
            throw new AssetException(400, "asset-tree.error.scheme");
        }
        return body;
    }

    public static List<SchemeDocumentDto> list(ActiveObjects ao, String projectKey) {
        List<SchemeDocumentDto> found = new ArrayList<SchemeDocumentDto>();
        SchemeEntity[] rows = ao.find(SchemeEntity.class);
        if (rows != null) {
            for (SchemeEntity row : rows) {
                if (projectKey.equals(row.getProjectKey())) {
                    found.add(summary(row));
                }
            }
        }
        Collections.sort(found, new Comparator<SchemeDocumentDto>() {
            @Override
            public int compare(SchemeDocumentDto left, SchemeDocumentDto right) {
                return left.getId() - right.getId();
            }
        });
        return found;
    }

    public static SchemeDocumentDto one(ActiveObjects ao, String projectKey, int schemeId) {
        return full(require(ao, projectKey, schemeId));
    }

    public static SchemeDocumentDto create(ActiveObjects ao, String projectKey, SchemeDocumentDto draft) {
        String name = nameOf(draft == null ? null : draft.getName());
        String body = bodyOf(draft == null ? null : draft.getBody());
        if (list(ao, projectKey).size() >= MAX_SCHEMES) {
            throw new AssetException(400, "asset-tree.error.scheme");
        }
        long now = System.currentTimeMillis();
        SchemeEntity row = ao.create(SchemeEntity.class, new DBParam("PROJECT_KEY", projectKey), new DBParam("NAME", name));
        row.setBody(body);
        row.setUpdatedAt(now);
        row.save();
        return summary(row);
    }

    public static SchemeDocumentDto save(ActiveObjects ao, String projectKey, int schemeId, SchemeDocumentDto draft) {
        SchemeEntity row = require(ao, projectKey, schemeId);
        row.setName(nameOf(draft == null ? null : draft.getName()));
        row.setBody(bodyOf(draft == null ? null : draft.getBody()));
        row.setUpdatedAt(System.currentTimeMillis());
        row.save();
        return summary(row);
    }

    public static void delete(ActiveObjects ao, String projectKey, int schemeId) {
        ao.delete(require(ao, projectKey, schemeId));
    }

    private static SchemeEntity require(ActiveObjects ao, String projectKey, int schemeId) {
        SchemeEntity[] rows = ao.find(SchemeEntity.class);
        if (rows != null) {
            for (SchemeEntity row : rows) {
                if (row.getID() == schemeId && projectKey.equals(row.getProjectKey())) {
                    return row;
                }
            }
        }
        throw new AssetException(404, "asset-tree.error.notFound");
    }

    private static SchemeDocumentDto summary(SchemeEntity row) {
        SchemeDocumentDto dto = new SchemeDocumentDto();
        dto.setId(row.getID());
        dto.setName(row.getName());
        dto.setUpdatedAt(row.getUpdatedAt());
        return dto;
    }

    private static SchemeDocumentDto full(SchemeEntity row) {
        SchemeDocumentDto dto = summary(row);
        dto.setBody(row.getBody());
        return dto;
    }
}
