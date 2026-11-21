package com.assetstree.jira.model;

/**
 * Access kinds stored on a project group.
 * A place is a branch, department, or room. A type is an object type.
 * An object is the equipment itself. Assets is every kind inside one project.
 * Project administrator has that full access, including settings and modules
 * except TNI, and only inside the one project the grant names.
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
    /** Full rights inside one project. Settings and modules, except TNI. */
    public static final String PROJECT_ADMIN = "projectAdmin";
    public static final String ADMIN = "admin";
    /** Opens reports and nothing else when it is the only kind. */
    public static final String REPORTS = "reports";
    /** Opens the floor plan and nothing else when it is the only kind. */
    public static final String MAP = "map";
    /** Opens schemes and leaves the drawing unchanged. */
    public static final String SCHEME = "scheme";
    /** Creates schemes and changes the drawing, including delete. */
    public static final String SCHEME_EDIT = "schemeEdit";
    /** Equipment actions. A stored "object" still allows every one of these. */
    public static final String OBJ_CREATE = "objCreate";
    public static final String OBJ_EDIT = "objEdit";
    public static final String OBJ_MOVE = "objMove";
    public static final String OBJ_REMOVE = "objRemove";
    /** Comments and files on a card. */
    public static final String NOTE = "note";
    public static final String SHARE = "share";
    /** Lifecycle fields and write-off. */
    public static final String LIFE = "life";
    public static final String PLACE_ADD = "placeAdd";
    public static final String PLACE_EDIT = "placeEdit";
    public static final String PLACE_DEL = "placeDel";
    public static final String STATUS = "status";
    public static final String EXCHANGE = "exchange";
    public static final String REPORT_EDIT = "reportEdit";
    public static final String PLAN = "plan";
    public static final String PLAN_EDIT = "planEdit";
    public static final String PORTAL = "portal";
    /** Manage the access list without the rest of the assets bundle. */
    public static final String GRANTS = "grants";
    public static final String ALL_PROJECTS = "*";

    public static final String CREATE = "create";
    public static final String EDIT = "edit";
    public static final String MOVE = "move";
    public static final String REMOVE = "remove";
    public static final String COMMENT = "comment";
    public static final String SCHEMA = "schema";
    public static final String ACCESS = "access";

    private static final String[] ORDER = new String[] {
            VIEW, PLACES, TYPES, OBJECT, ASSETS, PROJECT_ADMIN, ADMIN, REPORTS, MAP, SCHEME, SCHEME_EDIT
    };
    private static final String[] FINE = new String[] {
            OBJ_CREATE, OBJ_EDIT, OBJ_MOVE, OBJ_REMOVE, NOTE, SHARE, LIFE,
            PLACE_ADD, PLACE_EDIT, PLACE_DEL, STATUS, EXCHANGE,
            REPORT_EDIT, PLAN, PLAN_EDIT, PORTAL, GRANTS
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
        if (PROJECT_ADMIN.equals(level) || "projectadmin".equals(level)) {
            return normalize(PROJECT_ADMIN);
        }
        return "";
    }

    public static String normalize(String raw) {
        java.util.Map<String, String> chosen = new java.util.LinkedHashMap<String, String>();
        if (raw != null) {
            String[] parts = raw.split(",");
            for (int i = 0; i < parts.length; i++) {
                addToken(chosen, parts[i] == null ? "" : parts[i].trim());
            }
        }
        if (chosen.isEmpty()) {
            return "";
        }
        if (chosen.containsKey(ADMIN) || chosen.containsKey(PROJECT_ADMIN)) {
            putBare(chosen, ASSETS);
        }
        if (chosen.containsKey(ASSETS)) {
            putBare(chosen, PLACES);
            putBare(chosen, TYPES);
            putBare(chosen, OBJECT);
            putBare(chosen, SCHEME);
            putBare(chosen, SCHEME_EDIT);
        }
        if (chosen.containsKey(SCHEME_EDIT)) {
            putBare(chosen, SCHEME);
        }
        if (chosen.containsKey(PLACES) || chosen.containsKey(TYPES) || chosen.containsKey(OBJECT)
                || chosen.containsKey(ASSETS) || chosen.containsKey(ADMIN) || chosen.containsKey(PROJECT_ADMIN)
                || opensTree(chosen)) {
            putBare(chosen, VIEW);
        }
        StringBuilder builder = new StringBuilder();
        appendChosen(builder, chosen, ORDER);
        appendChosen(builder, chosen, FINE);
        return builder.toString();
    }

    /**
     * The short column keeps the older broad kinds. Fine actions live in the
     * unlimited rights column when the full list no longer fits.
     */
    public static String coarse(String caps) {
        String normalized = normalize(caps);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ORDER.length; i++) {
            if (!contains(normalized, ORDER[i])) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(ORDER[i]);
        }
        String value = builder.toString();
        return value.length() <= 80 ? value : value.substring(0, 80);
    }

    /**
     * Where this one action applies. A stored suffix wins. An older broad kind
     * uses the grant's single reach. Reading actions stay on the whole project.
     */
    public static String actionReach(String caps, String action, String fallback) {
        if (action == null || action.isEmpty() || !allows(caps, action)) {
            return "";
        }
        String explicit = "";
        String normalized = normalize(caps);
        String[] parts = normalized.split(",");
        for (int i = 0; i < parts.length; i++) {
            if (!action.equals(tokenBase(parts[i]))) {
                continue;
            }
            String suffix = tokenReach(parts[i]);
            if ("own".equals(suffix) || "all".equals(suffix)) {
                explicit = suffix;
            }
        }
        if (!explicit.isEmpty()) {
            return explicit;
        }
        if (!bounded(action)) {
            return "all";
        }
        return "own".equals(fallback) ? "own" : "all";
    }

    /**
     * True when the stored kinds allow this action. An older broad kind still
     * allows the actions it allowed before. A fine kind does not widen into
     * the other actions of that family.
     */
    public static boolean allows(String caps, String action) {
        if (has(caps, action)) {
            return true;
        }
        String normalized = normalize(caps);
        if (contains(normalized, action)) {
            return true;
        }
        if (OBJ_CREATE.equals(action) || OBJ_EDIT.equals(action) || OBJ_MOVE.equals(action)
                || OBJ_REMOVE.equals(action) || SHARE.equals(action) || LIFE.equals(action)) {
            return has(caps, OBJECT);
        }
        if (NOTE.equals(action)) {
            return has(caps, OBJECT) || has(caps, PLACES);
        }
        if (PLACE_ADD.equals(action) || PLACE_EDIT.equals(action) || PLACE_DEL.equals(action)) {
            return has(caps, PLACES);
        }
        if (STATUS.equals(action) || PORTAL.equals(action)) {
            return has(caps, TYPES);
        }
        if (EXCHANGE.equals(action) || PLAN_EDIT.equals(action)) {
            return has(caps, OBJECT);
        }
        if (PLAN.equals(action)) {
            return has(caps, VIEW);
        }
        if (REPORT_EDIT.equals(action)) {
            return has(caps, ASSETS) || has(caps, PROJECT_ADMIN) || has(caps, ADMIN);
        }
        if (GRANTS.equals(action) || ACCESS.equals(action)) {
            return has(caps, ASSETS) || contains(normalized, GRANTS);
        }
        return false;
    }

    /** Writes on a card. View, reports, the map and scheme reading stay project-wide. */
    public static boolean bounded(String cap) {
        return OBJECT.equals(cap) || PLACES.equals(cap)
                || OBJ_CREATE.equals(cap) || OBJ_EDIT.equals(cap) || OBJ_MOVE.equals(cap) || OBJ_REMOVE.equals(cap)
                || NOTE.equals(cap) || SHARE.equals(cap) || LIFE.equals(cap)
                || PLACE_ADD.equals(cap) || PLACE_EDIT.equals(cap) || PLACE_DEL.equals(cap)
                || CREATE.equals(cap) || EDIT.equals(cap) || MOVE.equals(cap) || REMOVE.equals(cap) || COMMENT.equals(cap);
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
            return contains(normalized, SCHEME_EDIT) || contains(normalized, ASSETS)
                    || contains(normalized, ADMIN) || contains(normalized, PROJECT_ADMIN);
        }
        if (PROJECT_ADMIN.equals(cap)) {
            return contains(normalized, PROJECT_ADMIN);
        }
        return contains(normalized, cap);
    }

    public static String levelOf(String caps) {
        if (has(caps, ADMIN)) {
            return ADMIN;
        }
        if (has(caps, PROJECT_ADMIN)) {
            return PROJECT_ADMIN;
        }
        if (has(caps, ASSETS)) {
            return ASSETS;
        }
        if (has(caps, PLACES) || has(caps, TYPES) || has(caps, OBJECT) || fineEdit(caps)) {
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

    private static void addToken(java.util.Map<String, String> chosen, String raw) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        String reach = tokenReach(raw);
        String token = tokenBase(raw);
        if (CREATE.equals(token) || EDIT.equals(token) || MOVE.equals(token)
                || REMOVE.equals(token) || COMMENT.equals(token)) {
            putBare(chosen, PLACES);
            putBare(chosen, OBJECT);
            return;
        }
        if (SCHEMA.equals(token)) {
            putBare(chosen, TYPES);
            return;
        }
        if (ACCESS.equals(token)) {
            putBare(chosen, ASSETS);
            return;
        }
        if (!known(token)) {
            return;
        }
        if (!chosen.containsKey(token) || !reach.isEmpty()) {
            chosen.put(token, reach);
        }
    }

    private static void putBare(java.util.Map<String, String> chosen, String token) {
        if (!chosen.containsKey(token)) {
            chosen.put(token, "");
        }
    }

    private static boolean known(String token) {
        for (int i = 0; i < ORDER.length; i++) {
            if (ORDER[i].equals(token)) {
                return true;
            }
        }
        for (int i = 0; i < FINE.length; i++) {
            if (FINE[i].equals(token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean opensTree(java.util.Map<String, String> chosen) {
        return chosen.containsKey(OBJ_CREATE) || chosen.containsKey(OBJ_EDIT) || chosen.containsKey(OBJ_MOVE)
                || chosen.containsKey(OBJ_REMOVE) || chosen.containsKey(NOTE) || chosen.containsKey(SHARE)
                || chosen.containsKey(LIFE) || chosen.containsKey(PLACE_ADD) || chosen.containsKey(PLACE_EDIT)
                || chosen.containsKey(PLACE_DEL) || chosen.containsKey(STATUS) || chosen.containsKey(EXCHANGE)
                || chosen.containsKey(PLAN_EDIT) || chosen.containsKey(PORTAL) || chosen.containsKey(GRANTS);
    }

    private static boolean fineEdit(String caps) {
        String normalized = normalize(caps);
        return contains(normalized, OBJ_CREATE) || contains(normalized, OBJ_EDIT)
                || contains(normalized, OBJ_MOVE) || contains(normalized, OBJ_REMOVE)
                || contains(normalized, NOTE) || contains(normalized, PLACE_ADD)
                || contains(normalized, PLACE_EDIT) || contains(normalized, PLACE_DEL);
    }

    private static void appendChosen(StringBuilder builder, java.util.Map<String, String> chosen, String[] order) {
        for (int i = 0; i < order.length; i++) {
            if (!chosen.containsKey(order[i])) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(order[i]);
            String reach = chosen.get(order[i]);
            if ("own".equals(reach) || "all".equals(reach)) {
                builder.append(':').append(reach);
            }
        }
    }

    private static boolean contains(String caps, String cap) {
        if (caps == null || cap == null) {
            return false;
        }
        String[] parts = caps.split(",");
        for (int i = 0; i < parts.length; i++) {
            if (cap.equals(tokenBase(parts[i]))) {
                return true;
            }
        }
        return false;
    }

    private static String tokenBase(String token) {
        if (token == null) {
            return "";
        }
        int colon = token.indexOf(':');
        return colon > 0 ? token.substring(0, colon) : token;
    }

    private static String tokenReach(String token) {
        if (token == null) {
            return "";
        }
        int colon = token.indexOf(':');
        if (colon <= 0 || colon >= token.length() - 1) {
            return "";
        }
        String suffix = token.substring(colon + 1);
        return "own".equals(suffix) || "all".equals(suffix) ? suffix : "";
    }
}
