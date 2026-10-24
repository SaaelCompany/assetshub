package com.assetstree.jira.web;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the asset custom fields and the Jira project behind a Service Desk portal.
 * The portal classes are loaded by reflection so the plugin still starts on Jira
 * without Service Management.
 */
public final class PortalLookup {
    private static final Pattern PORTAL_ID = Pattern.compile("/portal/(\\d+)");
    private static final String PORTAL_SERVICE = "com.atlassian.servicedesk.api.portal.PortalService";
    private static volatile Object cachedService;

    private PortalLookup() {
    }

    public static List<String> assetFieldIds() {
        List<String> ids = new ArrayList<String>();
        CustomFieldManager manager = ComponentAccessor.getCustomFieldManager();
        if (manager == null) {
            return ids;
        }
        for (CustomField field : manager.getCustomFieldObjects()) {
            if (field == null || field.getCustomFieldType() == null || field.getId() == null) {
                continue;
            }
            String key = field.getCustomFieldType().getKey();
            if (key != null && key.endsWith(":asset-cf")) {
                ids.add(field.getId());
            }
        }
        return ids;
    }

    public static String fieldIdList() {
        StringBuilder builder = new StringBuilder();
        for (String id : assetFieldIds()) {
            if (id == null || !id.startsWith("customfield_")) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(id);
        }
        return builder.toString();
    }

    public static int portalId(HttpServletRequest request) {
        if (request == null) {
            return 0;
        }
        return portalIdFromUri(request.getRequestURI());
    }

    public static int portalIdFromUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return 0;
        }
        Matcher matcher = PORTAL_ID.matcher(uri);
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public static String projectKeyForPortal(ApplicationUser user, int portalId) {
        if (portalId <= 0) {
            return null;
        }
        try {
            Object service = portalService();
            if (service == null) {
                return null;
            }
            Object portal = findPortal(service, service.getClass(), user, portalId);
            if (portal == null) {
                return null;
            }
            String key = projectKey(portal);
            if (key != null && !key.isEmpty()) {
                return key;
            }
            Long projectId = projectId(portal);
            if (projectId == null) {
                return null;
            }
            Project project = ComponentAccessor.getProjectManager().getProjectObj(projectId);
            return project == null ? null : project.getKey();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static Object portalService() {
        Object cached = cachedService;
        if (cached != null) {
            return cached;
        }
        Object found = osgiService(PORTAL_SERVICE);
        if (found != null) {
            cachedService = found;
        }
        return found;
    }

    private static Object osgiService(String className) {
        Object local = serviceFrom(className, PortalLookup.class.getClassLoader());
        if (local != null) {
            return local;
        }
        PluginAccessor accessor = ComponentAccessor.getPluginAccessor();
        if (accessor == null) {
            return null;
        }
        Collection<Plugin> plugins = accessor.getEnabledPlugins();
        if (plugins == null) {
            return null;
        }
        for (Plugin plugin : plugins) {
            if (plugin == null || plugin.getKey() == null || plugin.getKey().indexOf("servicedesk") < 0) {
                continue;
            }
            Object found = serviceFrom(className, plugin.getClassLoader());
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static Object serviceFrom(String className, ClassLoader loader) {
        if (loader == null) {
            return null;
        }
        try {
            Class<?> type = Class.forName(className, false, loader);
            return ComponentAccessor.getOSGiComponentInstanceOfType(type);
        } catch (ClassNotFoundException ex) {
            return null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static Object findPortal(Object service, Class<?> type, ApplicationUser user, int portalId) {
        Method[] methods = type.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (!"getPortalForId".equals(method.getName())) {
                continue;
            }
            Object[] args = arguments(method.getParameterTypes(), user, portalId);
            if (args == null) {
                continue;
            }
            try {
                Object portal = unwrapResult(method.invoke(service, args));
                if (portal != null) {
                    return portal;
                }
            } catch (ReflectiveOperationException ex) {
                // Try the next overload. Service Desk wraps a missing portal in an error value.
            }
        }
        return null;
    }

    private static Object[] arguments(Class<?>[] params, ApplicationUser user, int portalId) {
        if (params.length == 1) {
            Object id = convert(params[0], portalId);
            return id == null ? null : new Object[]{id};
        }
        if (params.length == 2) {
            if (user != null && !params[0].isInstance(user)) {
                return null;
            }
            Object id = convert(params[1], portalId);
            return id == null ? null : new Object[]{user, id};
        }
        return null;
    }

    private static Object convert(Class<?> type, int portalId) {
        if (type == Integer.class || type == int.class) {
            return Integer.valueOf(portalId);
        }
        if (type == Long.class || type == long.class) {
            return Long.valueOf(portalId);
        }
        if (type == String.class) {
            return String.valueOf(portalId);
        }
        try {
            return type.getConstructor(long.class).newInstance(Long.valueOf(portalId));
        } catch (ReflectiveOperationException ex) {
            // Not a long wrapper.
        }
        try {
            return type.getMethod("valueOf", long.class).invoke(null, Long.valueOf(portalId));
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    static Object unwrapResult(Object value) throws ReflectiveOperationException {
        if (value == null) {
            return null;
        }
        if (has(value, "isRight")) {
            if (!Boolean.TRUE.equals(call(value, "isRight"))) {
                return null;
            }
            if (has(value, "right")) {
                Object projection = call(value, "right");
                return projection == null ? null : call(projection, "get");
            }
            if (has(value, "getOrNull")) {
                return call(value, "getOrNull");
            }
        }
        if (has(value, "isValid")) {
            if (!Boolean.TRUE.equals(call(value, "isValid"))) {
                return null;
            }
            if (has(value, "getReturnedValue")) {
                return call(value, "getReturnedValue");
            }
            if (has(value, "get")) {
                return call(value, "get");
            }
        }
        return value;
    }

    private static boolean has(Object value, String name) {
        try {
            value.getClass().getMethod(name);
            return true;
        } catch (NoSuchMethodException ex) {
            return false;
        }
    }

    private static Object call(Object value, String name) throws ReflectiveOperationException {
        return value.getClass().getMethod(name).invoke(value);
    }

    private static String projectKey(Object portal) {
        Object project = invoke(portal, "getProject");
        if (project instanceof String) {
            return (String) project;
        }
        if (project != null) {
            Object key = invoke(project, "getKey");
            if (key instanceof String) {
                return (String) key;
            }
        }
        Object key = invoke(portal, "getProjectKey");
        return key instanceof String ? (String) key : null;
    }

    private static Long projectId(Object portal) {
        Object id = invoke(portal, "getProjectId");
        if (id instanceof Number) {
            return Long.valueOf(((Number) id).longValue());
        }
        return null;
    }

    private static Object invoke(Object target, String name) {
        try {
            return target.getClass().getMethod(name).invoke(target);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }
}
