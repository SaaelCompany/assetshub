package com.assetstree.jira.model;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Access kinds stored on a project group.
 * A place is a branch, department, or room. A type is an object type.
 * An object is the equipment itself. Assets is every kind inside one project.
 * Administrator covers every project.
 * Older rows used view, create, edit, move, schema, and access. Those expand
 * to the same kind of work they allowed before.
 */
public final class GrantCaps {
    public static final String VIEW = "view";
    public static final String PLACES = "places";
    public static final String TYPES = "types";
    public static final String OBJECT = "object";
    public static final String ASSETS = "assets";
    public static final String ADMIN = "admin";
    /** Opens reports and nothing else when it is the only kind. */
    public static final String REPORTS = "reports";
    /** Opens the floor plan and nothing else when it is the only kind. */
    public static final String MAP = "map";
    /** Opens schemes and leaves the drawing unchanged. */
    public static final String SCHEME = "scheme";
    /** Creates schemes and changes the drawing, including delete. */
    public static final String SCHEME_EDIT = "schemeEdit";
    public static final String ALL_PROJECTS = "*";

    public static final String CREATE = "create";
    public static final String EDIT = "edit";
    public static final String MOVE = "move";
    public static final String REMOVE = "remove";
    public static final String COMMENT = "comment";
    public static final String SCHEMA = "schema";
    public static final String ACCESS = "access";

    private static final String[] ORDER = new String[] {
            VIEW, PLACES, TYPES, OBJECT, ASSETS, ADMIN, REPORTS, MAP, SCHEME, SCHEME_EDIT
    };

    private GrantCaps() {
    }

    public static String fromLevel(String level) {
        if ("manage".equals(level)) {
            return normalize(ASSETS);
        }
        if ("edit".equals(level)) {
            return normalize(PLACES + "," + OBJECT);
        }
        if ("view".equals(level) || VIEW.equals(level)) {
            return VIEW;
        }
        if (REPORTS.equals(level)) {
            return REPORTS;
        }
        if (MAP.equals(level)) {
            return MAP;
        }
        if (SCHEME.equals(level)) {
            return SCHEME;
        }
        if (SCHEME_EDIT.equals(level) || "schemeedit".equals(level)) {
            return normalize(SCHEME_EDIT);
        }
        return "";
    }

    public static String normalize(String raw) {
        Set<String> chosen = new LinkedHashSet<String>();
        if (raw != null) {
            String[] parts = raw.split(",");
            for (int i = 0; i < parts.length; i++) {
                addToken(chosen, parts[i] == null ? "" : parts[i].trim());
            }
        }
        if (chosen.isEmpty()) {
            return "";
        }
        if (chosen.contains(ADMIN)) {
            chosen.add(ASSETS);
        }
        if (chosen.contains(ASSETS)) {
            chosen.add(PLACES);
            chosen.add(TYPES);
            chosen.add(OBJECT);
            chosen.add(SCHEME);
            chosen.add(SCHEME_EDIT);
        }
        if (chosen.contains(SCHEME_EDIT)) {
            chosen.add(SCHEME);
        }
        if (chosen.contains(PLACES) || chosen.contains(TYPES) || chosen.contains(OBJECT)
                || chosen.contains(ASSETS) || chosen.contains(ADMIN)) {
            chosen.add(VIEW);
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ORDER.length; i++) {
            if (!chosen.contains(ORDER[i])) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(ORDER[i]);
        }
        return builder.toString();
    }

    public static boolean has(String caps, String cap) {
        if (caps == null || cap == null || cap.isEmpty()) {
            return false;
        }
        String normalized = normalize(caps);
        if (SCHEMA.equals(cap)) {
            return contains(normalized, TYPES);
        }
        if (ACCESS.equals(cap)) {
            return contains(normalized, ASSETS);
        }
        if (CREATE.equals(cap) || EDIT.equals(cap) || MOVE.equals(cap) || REMOVE.equals(cap) || COMMENT.equals(cap)) {
            return contains(normalized, PLACES) || contains(normalized, OBJECT);
        }
        if (REPORTS.equals(cap)) {
            return contains(normalized, REPORTS) || contains(normalized, VIEW);
        }
        if (MAP.equals(cap)) {
            return contains(normalized, MAP) || contains(normalized, VIEW);
        }
        if (SCHEME.equals(cap)) {
            return contains(normalized, SCHEME) || contains(normalized, SCHEME_EDIT)
                    || contains(normalized, VIEW) || contains(normalized, MAP)
                    || contains(normalized, ASSETS) || contains(normalized, ADMIN);
        }
        if (SCHEME_EDIT.equals(cap)) {
            return contains(normalized, SCHEME_EDIT) || contains(normalized, ASSETS) || contains(normalized, ADMIN);
        }
        return contains(normalized, cap);
    }

    public static String levelOf(String caps) {
        if (has(caps, ADMIN)) {
            return ADMIN;
        }
        if (has(caps, ASSETS)) {
            return ASSETS;
        }
        if (has(caps, PLACES) || has(caps, TYPES) || has(caps, OBJECT)) {
            return "edit";
        }
        if (contains(normalize(caps), VIEW)) {
            return VIEW;
        }
        if (contains(normalize(caps), REPORTS)) {
            return REPORTS;
        }
        if (contains(normalize(caps), MAP)) {
            return MAP;
        }
        if (contains(normalize(caps), SCHEME_EDIT)) {
            return SCHEME_EDIT;
        }
        if (contains(normalize(caps), SCHEME)) {
            return SCHEME;
        }
        return VIEW;
    }

    private static void addToken(Set<String> chosen, String token) {
        if (VIEW.equals(token) || PLACES.equals(token) || TYPES.equals(token)
                || OBJECT.equals(token) || ASSETS.equals(token) || ADMIN.equals(token)
                || REPORTS.equals(token) || MAP.equals(token)
                || SCHEME.equals(token) || SCHEME_EDIT.equals(token)) {
            chosen.add(token);
            return;
        }
        if (CREATE.equals(token) || EDIT.equals(token) || MOVE.equals(token)
                || REMOVE.equals(token) || COMMENT.equals(token)) {
            chosen.add(PLACES);
            chosen.add(OBJECT);
            return;
        }
        if (SCHEMA.equals(token)) {
            chosen.add(TYPES);
            return;
        }
        if (ACCESS.equals(token)) {
            chosen.add(ASSETS);
        }
    }

    private static boolean contains(String caps, String cap) {
        String[] parts = caps.split(",");
        for (int i = 0; i < parts.length; i++) {
            if (cap.equals(parts[i])) {
                return true;
            }
        }
        return false;
    }
}
