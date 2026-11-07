package com.assetstree.jira.web;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Reads Service Desk request types and creates a customer request.
 * The Service Desk classes are loaded by reflection, the same way as {@link PortalLookup},
 * so the plugin still starts on Jira without Service Management.
 */
public final class PortalDesk {
    private static final String DESK = "com.atlassian.servicedesk.api.ServiceDeskService";
    private static final String TYPES = "com.atlassian.servicedesk.api.requesttype.RequestTypeService";
    private static final String FIELDS = "com.atlassian.servicedesk.api.field.RequestTypeFieldService";
    private static final String REQUESTS = "com.atlassian.servicedesk.api.request.ServiceDeskCustomerRequestService";
    private static final String CHANNEL = "com.atlassian.servicedesk.api.request.CustomerRequestChannelSource";
    private static final String FIELD_ID = "com.atlassian.servicedesk.api.field.FieldId";
    private static final String FIELD_VALUE = "com.atlassian.servicedesk.api.field.FieldInputValue";

    private PortalDesk() {
    }

    public static PortalRequestForm form(ApplicationUser user, Project project) {
        try {
            Loaded loaded = load(user, project);
            if (loaded.failure != null) {
                return loaded.failure;
            }
            Object builder = call(loaded.typeService, "newQueryBuilder");
            call(builder, "serviceDesk", Integer.valueOf(loaded.deskId));
            callOptional(builder, "filterHidden", Boolean.TRUE);
            callOptional(builder, "isValid", Boolean.TRUE);
            Object page = PortalLookup.unwrapResult(call(loaded.typeService, "getRequestTypes", user, call(builder, "build")));
            List<PortalRequestForm.Type> types = new ArrayList<PortalRequestForm.Type>();
            for (Object row : results(page)) {
                if (types.size() >= 40) {
                    break;
                }
                PortalRequestForm.Type type = readType(user, loaded.fieldService, loaded.deskId, row, loaded.assetFields);
                if (type != null) {
                    types.add(type);
                }
            }
            return PortalRequestForm.ready(types);
        } catch (RuntimeException ex) {
            return PortalRequestForm.failed("asset-tree.error.portalRequest", clean(causeMessage(ex)));
        } catch (ReflectiveOperationException ex) {
            return PortalRequestForm.failed("asset-tree.error.portalRequest", clean(causeMessage(ex)));
        }
    }

    public static Created create(ApplicationUser user, Project project, String typeId, Map<String, String> values) {
        try {
            int wanted = parseId(typeId);
            if (wanted <= 0) {
                return Created.failed("asset-tree.error.portalRequest.type", "");
            }
            Loaded loaded = load(user, project);
            if (loaded.failure != null) {
                return Created.failed(loaded.failure.getFailureKey(), loaded.failure.getFailureArg());
            }
            Object requestService = PortalLookup.service(REQUESTS);
            if (requestService == null) {
                return Created.failed("asset-tree.error.portalDesk", "");
            }
            Object type = findType(loaded.typeService, user, loaded.deskId, wanted);
            if (type == null) {
                return Created.failed("asset-tree.error.portalRequest.type", "");
            }
            Object builder = call(requestService, "newCreateBuilder");
            call(builder, "serviceDesk", loaded.desk);
            call(builder, "requestType", type);
            Object channel = channel(requestService.getClass().getClassLoader());
            if (channel != null) {
                callOptional(builder, "customerRequestChannelSource", channel);
            }
            ClassLoader loader = requestService.getClass().getClassLoader();
            Class<?> fieldIdClass = Class.forName(FIELD_ID, false, loader);
            Class<?> valueClass = Class.forName(FIELD_VALUE, false, loader);
            Method withId = fieldIdClass.getMethod("withId", String.class);
            Method withValue = valueClass.getMethod("withValue", Object.class);
            Map<String, String> safe = values == null ? Collections.<String, String>emptyMap() : values;
            for (Map.Entry<String, String> entry : safe.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isEmpty() || entry.getValue() == null) {
                    continue;
                }
                Object fieldId = withId.invoke(null, entry.getKey());
                Object fieldValue = withValue.invoke(null, entry.getValue());
                call(builder, "fieldValue", fieldId, fieldValue);
            }
            Object created = PortalLookup.unwrapResult(call(requestService, "createCustomerRequest", user, call(builder, "build")));
            if (created == null) {
                return Created.failed("asset-tree.error.portalRequest", "");
            }
            Object issueObj = call(created, "getIssue");
            Issue issue = issueObj instanceof Issue ? (Issue) issueObj : null;
            String key = issue == null || issue.getKey() == null ? "" : issue.getKey();
            if (key.isEmpty()) {
                return Created.failed("asset-tree.error.portalRequest", "");
            }
            int portalId = 0;
            Object portal = invokeQuiet(type, "getPortalId");
            if (portal instanceof Number) {
                portalId = ((Number) portal).intValue();
            }
            return Created.ok(key, portalUrl(portalId, key), issue);
        } catch (RuntimeException ex) {
            return Created.failed("asset-tree.error.portalRequest", clean(causeMessage(ex)));
        } catch (ReflectiveOperationException ex) {
            return Created.failed("asset-tree.error.portalRequest", clean(causeMessage(ex)));
        }
    }

    private static Loaded load(ApplicationUser user, Project project) throws ReflectiveOperationException {
        Object deskService = PortalLookup.service(DESK);
        Object typeService = PortalLookup.service(TYPES);
        Object fieldService = PortalLookup.service(FIELDS);
        if (deskService == null || typeService == null || fieldService == null || project == null) {
            return Loaded.failed(PortalRequestForm.failed("asset-tree.error.portalDesk", ""));
        }
        Object desk = PortalLookup.unwrapResult(call(deskService, "getServiceDeskForProject", user, project));
        if (desk == null) {
            return Loaded.failed(PortalRequestForm.failed("asset-tree.error.portalDesk", ""));
        }
        Object id = call(desk, "getId");
        if (!(id instanceof Number)) {
            return Loaded.failed(PortalRequestForm.failed("asset-tree.error.portalDesk", ""));
        }
        return new Loaded(desk, ((Number) id).intValue(), typeService, fieldService, PortalLookup.assetFieldIds(), null);
    }

    private static Object findType(Object typeService, ApplicationUser user, int deskId, int typeId) throws ReflectiveOperationException {
        Object builder = call(typeService, "newQueryBuilder");
        call(builder, "serviceDesk", Integer.valueOf(deskId));
        call(builder, "requestType", Integer.valueOf(typeId));
        Object page = PortalLookup.unwrapResult(call(typeService, "getRequestTypes", user, call(builder, "build")));
        List<Object> rows = results(page);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static PortalRequestForm.Type readType(ApplicationUser user, Object fieldService, int deskId, Object row,
                                                    List<String> assetFields) {
        Object idValue = invokeQuiet(row, "getId");
        if (!(idValue instanceof Number)) {
            return null;
        }
        int id = ((Number) idValue).intValue();
        String name = text(invokeQuiet(row, "getName"));
        if (name.isEmpty()) {
            name = String.valueOf(id);
        }
        String description = text(invokeQuiet(row, "getHelpText"));
        if (description.isEmpty()) {
            description = text(invokeQuiet(row, "getDescription"));
        }
        List<PortalRequestForm.Field> fields = new ArrayList<PortalRequestForm.Field>();
        List<String> attached = new ArrayList<String>();
        try {
            Object builder = call(fieldService, "newQueryBuilder");
            call(builder, "serviceDesk", Integer.valueOf(deskId));
            call(builder, "requestType", Integer.valueOf(id));
            Object meta = PortalLookup.unwrapResult(call(fieldService, "getCustomerRequestCreateMeta", user, call(builder, "build")));
            Object raw = meta == null ? null : invokeQuiet(meta, "requestTypeFields");
            if (raw instanceof List) {
                for (Object field : (List<?>) raw) {
                    if (fields.size() >= 40) {
                        break;
                    }
                    readField(field, assetFields, fields, attached);
                }
            }
        } catch (ReflectiveOperationException ex) {
            fields.clear();
            attached.clear();
        }
        if (fields.isEmpty()) {
            fields.add(new PortalRequestForm.Field("summary", "Summary", true, "text", Collections.<PortalRequestForm.Option>emptyList()));
            fields.add(new PortalRequestForm.Field("description", "Description", false, "textarea", Collections.<PortalRequestForm.Option>emptyList()));
        }
        return new PortalRequestForm.Type(String.valueOf(id), name, description, fields, attached);
    }

    private static void readField(Object field, List<String> assetFields, List<PortalRequestForm.Field> fields, List<String> attached) {
        Object idObj = invokeQuiet(field, "fieldId");
        String id = idObj == null ? "" : text(invokeQuiet(idObj, "value"));
        if (id.isEmpty() || "attachment".equals(id)) {
            return;
        }
        if (assetFields != null && assetFields.contains(id)) {
            attached.add(id);
            return;
        }
        String name = text(invokeQuiet(field, "name"));
        if (name.isEmpty()) {
            name = id;
        }
        boolean required = Boolean.TRUE.equals(invokeQuiet(field, "required"));
        List<PortalRequestForm.Option> options = optionsOf(invokeQuiet(field, "validValues"));
        String kind = "text";
        if (!options.isEmpty()) {
            kind = "select";
        } else if ("description".equals(id) || "environment".equals(id)) {
            kind = "textarea";
        }
        fields.add(new PortalRequestForm.Field(id, name, required, kind, options));
    }

    private static List<PortalRequestForm.Option> optionsOf(Object raw) {
        List<PortalRequestForm.Option> options = new ArrayList<PortalRequestForm.Option>();
        if (!(raw instanceof List)) {
            return options;
        }
        for (Object node : (List<?>) raw) {
            collectOptions(node, "", options);
        }
        return options;
    }

    private static void collectOptions(Object node, String prefix, List<PortalRequestForm.Option> options) {
        if (node == null || options.size() >= 80) {
            return;
        }
        String value = text(invokeQuiet(node, "value"));
        String label = text(invokeQuiet(node, "label"));
        if (label.isEmpty()) {
            label = value;
        }
        String shown = prefix.isEmpty() ? label : prefix + " / " + label;
        Object children = invokeQuiet(node, "children");
        boolean nested = children instanceof List && !((List<?>) children).isEmpty();
        if (!nested) {
            if (!value.isEmpty()) {
                options.add(new PortalRequestForm.Option(value, shown));
            }
            return;
        }
        for (Object child : (List<?>) children) {
            collectOptions(child, shown, options);
        }
    }

    private static Object channel(ClassLoader loader) {
        if (loader == null) {
            return null;
        }
        try {
            Class<?> type = Class.forName(CHANNEL, false, loader);
            return type.getField("PORTAL").get(null);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    /** Customer portal of this project, without a request type. Empty when Service Desk is absent. */
    public static String customerPortal(ApplicationUser user, Project project) {
        try {
            Loaded loaded = load(user, project);
            if (loaded.failure != null || loaded.deskId <= 0) {
                return "";
            }
            int portalId = loaded.deskId;
            Object portal = invokeQuiet(loaded.desk, "getPortalId");
            if (portal instanceof Number && ((Number) portal).intValue() > 0) {
                portalId = ((Number) portal).intValue();
            }
            return portalHome(portalId);
        } catch (RuntimeException ex) {
            return "";
        } catch (ReflectiveOperationException ex) {
            return "";
        }
    }

    private static String portalUrl(int portalId, String key) {
        if (portalId <= 0) {
            return jiraBase() + "/browse/" + key;
        }
        return portalHome(portalId) + "/" + key;
    }

    private static String portalHome(int portalId) {
        if (portalId <= 0) {
            return "";
        }
        return jiraBase() + "/servicedesk/customer/portal/" + portalId;
    }

    private static String jiraBase() {
        String base = "";
        try {
            base = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
        } catch (RuntimeException ex) {
            base = "";
        }
        if (base == null) {
            base = "";
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    private static int parseId(String typeId) {
        if (typeId == null) {
            return 0;
        }
        try {
            return Integer.parseInt(typeId.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> results(Object page) throws ReflectiveOperationException {
        if (page == null) {
            return Collections.emptyList();
        }
        Object raw;
        try {
            raw = call(page, "getResults");
        } catch (NoSuchMethodException ex) {
            List<Object> list = new ArrayList<Object>();
            if (page instanceof Iterable) {
                for (Object item : (Iterable<Object>) page) {
                    list.add(item);
                }
            }
            return list;
        }
        if (raw instanceof List) {
            return (List<Object>) raw;
        }
        return Collections.emptyList();
    }

    private static Object call(Object target, String name, Object... args) throws ReflectiveOperationException {
        if (target == null) {
            throw new NoSuchMethodException(name);
        }
        Method match = null;
        Method[] methods = target.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (!method.getName().equals(name) || method.getParameterTypes().length != args.length) {
                continue;
            }
            if (!fits(method.getParameterTypes(), args)) {
                continue;
            }
            match = method;
            break;
        }
        if (match == null) {
            throw new NoSuchMethodException(target.getClass().getName() + "#" + name);
        }
        return match.invoke(target, args);
    }

    private static void callOptional(Object target, String name, Object arg) throws ReflectiveOperationException {
        try {
            call(target, name, arg);
        } catch (NoSuchMethodException ex) {
            // Older Service Desk builds omit the filter.
        }
    }

    private static boolean fits(Class<?>[] types, Object[] args) {
        for (int i = 0; i < types.length; i++) {
            if (args[i] == null) {
                if (types[i].isPrimitive()) {
                    return false;
                }
                continue;
            }
            Class<?> expected = types[i].isPrimitive() ? boxed(types[i]) : types[i];
            if (expected == null || !expected.isInstance(args[i])) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> boxed(Class<?> type) {
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        return null;
    }

    private static Object invokeQuiet(Object target, String name) {
        try {
            return call(target, name);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    static String clean(String message) {
        if (message == null) {
            return "";
        }
        String text = message.replace('{', ' ').replace('}', ' ').replace('\r', ' ').replace('\n', ' ').trim();
        if (text.length() > 240) {
            return text.substring(0, 240);
        }
        return text;
    }

    private static String causeMessage(Throwable error) {
        Throwable current = error;
        if (current instanceof InvocationTargetException && current.getCause() != null) {
            current = current.getCause();
        }
        return current == null ? "" : current.getMessage();
    }

    public static final class Created {
        private final String key;
        private final String url;
        private final Issue issue;
        private final String failureKey;
        private final String failureArg;

        private Created(String key, String url, Issue issue, String failureKey, String failureArg) {
            this.key = key == null ? "" : key;
            this.url = url == null ? "" : url;
            this.issue = issue;
            this.failureKey = failureKey;
            this.failureArg = failureArg == null ? "" : failureArg;
        }

        static Created ok(String key, String url, Issue issue) {
            return new Created(key, url, issue, null, "");
        }

        static Created failed(String key, String arg) {
            return new Created("", "", null, key, arg);
        }

        public String getKey() {
            return key;
        }

        public String getUrl() {
            return url;
        }

        public Issue getIssue() {
            return issue;
        }

        public String getFailureKey() {
            return failureKey;
        }

        public String getFailureArg() {
            return failureArg;
        }
    }

    private static final class Loaded {
        private final Object desk;
        private final int deskId;
        private final Object typeService;
        private final Object fieldService;
        private final List<String> assetFields;
        private final PortalRequestForm failure;

        private Loaded(Object desk, int deskId, Object typeService, Object fieldService, List<String> assetFields, PortalRequestForm failure) {
            this.desk = desk;
            this.deskId = deskId;
            this.typeService = typeService;
            this.fieldService = fieldService;
            this.assetFields = assetFields == null ? Collections.<String>emptyList() : assetFields;
            this.failure = failure;
        }

        private static Loaded failed(PortalRequestForm failure) {
            return new Loaded(null, 0, null, null, Collections.<String>emptyList(), failure);
        }
    }
}
