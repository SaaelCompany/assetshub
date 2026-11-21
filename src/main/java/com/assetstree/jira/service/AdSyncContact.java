package com.assetstree.jira.service;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads the job title, phone and department that Active Directory Attributes Sync
 * already shows on a request. Crowd does not keep those attributes on the user.
 */
public final class AdSyncContact {
    private static final Logger log = LoggerFactory.getLogger(AdSyncContact.class);
    private static final String PLUGIN = "com.intenso.jira.plugins.ad-integration-manager";
    private static final String MANAGER = "com.intenso.jira.plugins.admanager.api.UserAttributeManager";
    private static final String CONNECTIONS = "com.intenso.jira.plugins.admanager.api.ConnectionManager";
    private static final String LEGACY = "com.intenso.jira.plugins.admanager.util.ActiveDirectoryAttributeManager";
    private static final long REMEMBER = 10L * 60L * 1000L;
    private static final long REMEMBER_EMPTY = 60L * 1000L;
    private static final ConcurrentHashMap<String, Cached> CACHE = new ConcurrentHashMap<String, Cached>();

    private AdSyncContact() {
    }

    public static void remember(Map<String, String> contact, String userKey, String username, long directoryId) {
        if (contact == null) {
            return;
        }
        Map<String, String> found = attributes(userKey, username, directoryId);
        for (Map.Entry<String, String> entry : found.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || value == null || value.trim().isEmpty() || contact.containsKey(key)) {
                continue;
            }
            contact.put(key, value.trim());
        }
    }

    static Map<String, String> attributes(String userKey, String username, long directoryId) {
        String keyName = text(userKey);
        String name = text(username);
        if (keyName.isEmpty() && name.isEmpty()) {
            return Collections.emptyMap();
        }
        String cacheKey = directoryId + "\n" + keyName + "\n" + name;
        long now = System.currentTimeMillis();
        Cached cached = CACHE.get(cacheKey);
        if (cached != null && cached.until > now) {
            return cached.values;
        }
        Map<String, String> found = read(keyName, name, directoryId);
        if (CACHE.size() > 400) {
            CACHE.clear();
        }
        CACHE.put(cacheKey, new Cached(found, now + (found.isEmpty() ? REMEMBER_EMPTY : REMEMBER)));
        return found;
    }

    /** Keeps a value under the name shown in Jira and under the Active Directory attribute name. */
    static void keep(Map<String, String> out, String jiraName, String adName, String value) {
        String text = value == null ? "" : value.trim();
        if (out == null || text.isEmpty()) {
            return;
        }
        put(out, jiraName, text);
        put(out, adName, text);
    }

    private static Map<String, String> read(String userKey, String username, long directoryId) {
        try {
            Object manager = component(MANAGER);
            String mapMethod = "getAttributes";
            if (manager == null) {
                manager = component(LEGACY);
                mapMethod = "getUserAttributes";
            }
            if (manager == null) {
                return Collections.emptyMap();
            }
            Object connections = component(CONNECTIONS);
            if (connections == null) {
                connections = manager;
            }
            List<Object> ordered = order(connections, directoryId);
            if (ordered.isEmpty() && connections != null) {
                ordered = order(connections, 0L);
            }
            Map<String, String> out = new LinkedHashMap<String, String>();
            if (ordered.isEmpty()) {
                return out;
            }
            for (int i = 0; i < ordered.size(); i++) {
                Object connection = ordered.get(i);
                String connectionName = text(call(connection, "getName"));
                if (connectionName.isEmpty()) {
                    continue;
                }
                merge(out, mapOf(call(manager, mapMethod, String.class, userKey, String.class, connectionName)));
                if (!userKey.equals(username)) {
                    merge(out, mapOf(call(manager, mapMethod, String.class, username, String.class, connectionName)));
                }
                alias(out, connection, manager, userKey, username, connectionName);
            }
            return out;
        } catch (RuntimeException ex) {
            log.debug("AD attributes were not read", ex);
            return Collections.emptyMap();
        }
    }

    private static void alias(Map<String, String> out, Object connection, Object manager, String userKey, String username, String connectionName) {
        Object fields = call(connection, "getUserFields");
        if (!(fields instanceof Iterable)) {
            return;
        }
        for (Object field : (Iterable<?>) fields) {
            if (field == null) {
                continue;
            }
            String jiraName = text(call(field, "getNameInJira"));
            String adName = text(call(field, "getNameInAd"));
            String value = out.get(jiraName);
            if (value == null || value.trim().isEmpty()) {
                value = text(call(manager, "getAttribute", String.class, first(userKey, username), String.class, connectionName, String.class, jiraName));
            }
            keep(out, jiraName, adName, value);
        }
    }

    private static List<Object> order(Object connectionManager, long directoryId) {
        List<Object> matching = new ArrayList<Object>();
        List<Object> rest = new ArrayList<Object>();
        if (connectionManager == null) {
            return matching;
        }
        Object list = call(connectionManager, "getConnections");
        if (!(list instanceof Iterable)) {
            return matching;
        }
        for (Object connection : (Iterable<?>) list) {
            if (connection == null || !enabled(connection)) {
                continue;
            }
            if (directoryId > 0 && sameDirectory(connection, directoryId)) {
                matching.add(connection);
            } else if (directoryId <= 0) {
                matching.add(connection);
            } else {
                rest.add(connection);
            }
        }
        if (directoryId > 0) {
            matching.addAll(rest);
        }
        return matching;
    }

    private static boolean sameDirectory(Object connection, long directoryId) {
        Object value = call(connection, "getDirectoryId");
        return value != null && String.valueOf(directoryId).equals(String.valueOf(value).trim());
    }

    private static boolean enabled(Object connection) {
        Object value = call(connection, "isEnabled");
        if (value == null) {
            value = call(connection, "getEnabled");
        }
        return !(value instanceof Boolean) || ((Boolean) value).booleanValue();
    }

    private static void merge(Map<String, String> out, Map<String, String> incoming) {
        for (Map.Entry<String, String> entry : incoming.entrySet()) {
            put(out, entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> mapOf(Object value) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        if (!(value instanceof Map)) {
            return out;
        }
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
            if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof String)) {
                continue;
            }
            put(out, (String) entry.getKey(), (String) entry.getValue());
        }
        return out;
    }

    private static void put(Map<String, String> out, String key, String value) {
        String name = text(key);
        String text = text(value);
        if (name.isEmpty() || text.isEmpty() || out.containsKey(name)) {
            return;
        }
        out.put(name, text);
    }

    private static Object component(String className) {
        Class<?> type = load(className);
        if (type == null) {
            return null;
        }
        try {
            return ComponentAccessor.getOSGiComponentInstanceOfType(type);
        } catch (RuntimeException ex) {
            log.debug("AD sync component was not available", ex);
            return null;
        }
    }

    private static Class<?> load(String className) {
        try {
            Plugin plugin = ComponentAccessor.getPluginAccessor().getEnabledPlugin(PLUGIN);
            if (plugin != null && plugin.getClassLoader() != null) {
                try {
                    return plugin.getClassLoader().loadClass(className);
                } catch (ClassNotFoundException ignored) {
                    // An older build keeps the same API on the shared plugin loader.
                }
            }
            return ComponentAccessor.getPluginAccessor().getClassLoader().loadClass(className);
        } catch (ClassNotFoundException ex) {
            return null;
        } catch (RuntimeException ex) {
            log.debug("AD sync component was not available", ex);
            return null;
        }
    }

    private static Object call(Object target, String name) {
        if (target == null || name == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(name).invoke(target);
        } catch (NoSuchMethodException ex) {
            return null;
        } catch (IllegalAccessException ex) {
            log.debug("AD attributes were not read", ex);
            return null;
        } catch (InvocationTargetException ex) {
            log.debug("AD attributes were not read", ex);
            return null;
        } catch (RuntimeException ex) {
            log.debug("AD attributes were not read", ex);
            return null;
        }
    }

    private static Object call(Object target, String name, Class<?> firstType, Object first, Class<?> secondType, Object second) {
        return call(target, name, new Class<?>[] {firstType, secondType}, new Object[] {first, second});
    }

    private static Object call(Object target, String name, Class<?> firstType, Object first, Class<?> secondType, Object second, Class<?> thirdType, Object third) {
        return call(target, name, new Class<?>[] {firstType, secondType, thirdType}, new Object[] {first, second, third});
    }

    private static Object call(Object target, String name, Class<?>[] types, Object[] args) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(name, types);
            return method.invoke(target, args);
        } catch (NoSuchMethodException ex) {
            return null;
        } catch (IllegalAccessException ex) {
            log.debug("AD attributes were not read", ex);
            return null;
        } catch (InvocationTargetException ex) {
            log.debug("AD attributes were not read", ex);
            return null;
        } catch (RuntimeException ex) {
            log.debug("AD attributes were not read", ex);
            return null;
        }
    }

    private static String first(String preferred, String fallback) {
        return preferred == null || preferred.isEmpty() ? fallback : preferred;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static final class Cached {
        private final Map<String, String> values;
        private final long until;

        private Cached(Map<String, String> values, long until) {
            this.values = values;
            this.until = until;
        }
    }
}
