package com.assetstree.jira.service;

import com.assetstree.jira.model.PersonContact;
import com.atlassian.crowd.embedded.api.Directory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads job title and phone from the directory Jira already uses.
 * Crowd does not keep those attributes on the user, so the document asks the directory itself.
 */
public final class DirectoryPeople {
    private static final Logger log = LoggerFactory.getLogger(DirectoryPeople.class);
    private static final String LOADER = "com.atlassian.crowd.directory.loader.DirectoryInstanceLoader";
    private static final long REMEMBER = 10L * 60L * 1000L;
    private static final String[] ATTRIBUTES = {
            "title", "jobTitle", "personalTitle", "employeeType", "description",
            "telephoneNumber", "telephone", "mobile",
            "mobileTelephone", "otherMobile", "otherTelephone", "ipPhone", "homePhone",
            "department", "division"
    };
    private static final ConcurrentHashMap<String, Cached> CACHE = new ConcurrentHashMap<String, Cached>();

    private DirectoryPeople() {
    }

    public static final class Found {
        public final String phone;
        public final String title;
        public final String department;

        public Found(String phone, String title, String department) {
            this.phone = phone == null ? "" : phone;
            this.title = title == null ? "" : title;
            this.department = department == null ? "" : department;
        }
    }

    public static Found find(Directory directory, String username, String email) {
        if (directory == null || directory.getId() == null) {
            return null;
        }
        Map<String, String> stored = directory.getAttributes();
        String url = stored == null ? "" : stored.get("ldap.url");
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        String name = username == null ? "" : username.trim();
        String mail = email == null ? "" : email.trim();
        if (name.isEmpty() && mail.isEmpty()) {
            return null;
        }
        String cacheKey = directory.getId() + "\n" + name + "\n" + mail;
        long now = System.currentTimeMillis();
        Cached cached = CACHE.get(cacheKey);
        if (cached != null && cached.until > now) {
            return cached.found;
        }
        Found found = search(directory, stored, name, mail);
        if (found == null) {
            return null;
        }
        if (CACHE.size() > 200) {
            CACHE.clear();
        }
        CACHE.put(cacheKey, new Cached(found, now + REMEMBER));
        return found;
    }

    public static Found fromAttributes(Map<String, String> attributes) {
        return new Found(PersonContact.phone(attributes), PersonContact.title(attributes), PersonContact.department(attributes));
    }

    public static String filter(String usernameAttribute, String username, String email) {
        StringBuilder names = new StringBuilder();
        String attribute = safeAttribute(usernameAttribute);
        addName(names, attribute, username);
        if (!"samaccountname".equalsIgnoreCase(attribute)) {
            addName(names, "sAMAccountName", username);
        }
        addName(names, "userPrincipalName", username);
        addName(names, "mail", email);
        if (username != null && username.indexOf('@') >= 0) {
            addName(names, "mail", username);
        }
        if (names.length() == 0) {
            return "";
        }
        return "(&(|(objectCategory=person)(objectClass=inetOrgPerson)(objectClass=organizationalPerson))"
                + "(!(objectClass=computer))(|" + names + "))";
    }

    public static String searchBase(String userDn, String baseDn) {
        String user = userDn == null ? "" : userDn.trim();
        String base = baseDn == null ? "" : baseDn.trim();
        if (user.isEmpty()) {
            return base;
        }
        if (base.isEmpty() || user.toLowerCase(java.util.Locale.ROOT).endsWith(base.toLowerCase(java.util.Locale.ROOT))) {
            return user;
        }
        return user + "," + base;
    }

    public static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' || c == '*' || c == '(' || c == ')' || c == '\0') {
                out.append('\\');
            }
            out.append(c);
        }
        return out.toString();
    }

    private static Found search(Directory directory, Map<String, String> stored, String username, String email) {
        String query = filter(stored.get("ldap.user.username"), username, email);
        if (query.isEmpty()) {
            return null;
        }
        String[] names = attributeNames(stored);
        Found live = searchLive(directory, query, names);
        if (live != null && !live.title.isEmpty()) {
            return live;
        }
        Found bound = searchBound(directory, stored, query, names);
        if (bound != null && (live == null || !bound.title.isEmpty() || !bound.phone.isEmpty() || !bound.department.isEmpty())) {
            return bound;
        }
        return live != null ? live : bound;
    }

    /** Attribute names the directory itself maps, plus the usual job, phone and department names. */
    static String[] attributeNames(Map<String, String> stored) {
        LinkedHashSet<String> names = new LinkedHashSet<String>();
        Collections.addAll(names, ATTRIBUTES);
        if (stored == null) {
            return names.toArray(new String[names.size()]);
        }
        for (Map.Entry<String, String> entry : stored.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().toLowerCase(Locale.ROOT);
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            if (!value.matches("[A-Za-z][A-Za-z0-9._-]{0,48}")) {
                continue;
            }
            if (key.contains("title") || key.contains("job") || key.contains("phone") || key.contains("mobile")
                    || key.contains("department") || key.contains("employee")) {
                names.add(value);
            }
        }
        return names.toArray(new String[names.size()]);
    }

    /** Uses the directory connection Jira already holds, so the password does not have to be rebuilt. */
    private static Found searchLive(Directory directory, String query, String[] names) {
        Object opened = loader();
        Object remote = opened == null ? null : remote(opened, directory);
        DirContext context = liveContext(remote, 0, Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>()));
        if (context == null) {
            return null;
        }
        try {
            return query(context, "", query, names);
        } catch (RuntimeException ex) {
            log.debug("Directory contact was not read", ex);
            return null;
        } catch (NamingException ex) {
            log.debug("Directory contact was not read", ex);
            return null;
        } finally {
            try {
                context.close();
            } catch (NamingException ignored) {
                // The directory conversation is already finished.
            }
        }
    }

    private static Found searchBound(Directory directory, Map<String, String> stored, String query, String[] names) {
        String base = searchBase(stored.get("ldap.user.dn"), stored.get("ldap.basedn"));
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        ClassLoader system = ClassLoader.getSystemClassLoader();
        LdapContext context = null;
        try {
            if (system != null) {
                Thread.currentThread().setContextClassLoader(system);
            }
            Hashtable<String, String> environment = environment(directory);
            if (environment == null || blank(environment.get(Context.PROVIDER_URL))) {
                return null;
            }
            environment.put(Context.REFERRAL, "ignore");
            if (!environment.containsKey("com.sun.jndi.ldap.connect.timeout")) {
                environment.put("com.sun.jndi.ldap.connect.timeout", "8000");
            }
            if (!environment.containsKey("com.sun.jndi.ldap.read.timeout")) {
                environment.put("com.sun.jndi.ldap.read.timeout", "8000");
            }
            context = new InitialLdapContext(environment, null);
            if (base.isEmpty()) {
                base = namingContext(context);
            }
            return query(context, base, query, names);
        } catch (RuntimeException ex) {
            log.debug("Directory contact was not read", ex);
            return null;
        } catch (NamingException ex) {
            log.debug("Directory contact was not read", ex);
            return null;
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException ignored) {
                    // The directory conversation is already finished.
                }
            }
        }
    }

    private static Found query(DirContext context, String base, String filter, String[] names) throws NamingException {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningAttributes(names);
        controls.setCountLimit(5);
        controls.setTimeLimit(8000);
        NamingEnumeration<SearchResult> found = context.search(base == null ? "" : base, filter, controls);
        try {
            String phone = "";
            String title = "";
            String department = "";
            while (found.hasMore()) {
                Found one = fromResult(found.next());
                if (phone.isEmpty()) {
                    phone = one.phone;
                }
                if (title.isEmpty()) {
                    title = one.title;
                }
                if (department.isEmpty()) {
                    department = one.department;
                }
                if (!phone.isEmpty() && !title.isEmpty()) {
                    break;
                }
            }
            return new Found(phone, title, department);
        } finally {
            found.close();
        }
    }

    private static DirContext liveContext(Object node, int depth, Set<Object> seen) {
        if (node == null || depth > 4 || node instanceof String || node instanceof Number || node instanceof Boolean
                || node instanceof Map || node instanceof Iterable || !seen.add(node)) {
            return null;
        }
        String typeName = node.getClass().getName().toLowerCase(Locale.ROOT);
        if (typeName.contains("contextsource") || typeName.contains("ldapcontext")) {
            DirContext opened = readOnly(node);
            if (opened != null) {
                return opened;
            }
        }
        Class<?> type = node.getClass();
        while (type != null && !type.getName().startsWith("java.")) {
            Field[] fields;
            try {
                fields = type.getDeclaredFields();
            } catch (RuntimeException ex) {
                return null;
            }
            for (int i = 0; i < fields.length; i++) {
                if (Modifier.isStatic(fields[i].getModifiers())) {
                    continue;
                }
                fields[i].setAccessible(true);
                Object value;
                try {
                    value = fields[i].get(node);
                } catch (IllegalAccessException ex) {
                    continue;
                }
                DirContext nested = liveContext(value, depth + 1, seen);
                if (nested != null) {
                    return nested;
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static DirContext readOnly(Object source) {
        try {
            Method method = source.getClass().getMethod("getReadOnlyContext");
            Object value = method.invoke(source);
            if (value instanceof DirContext) {
                return (DirContext) value;
            }
        } catch (RuntimeException ex) {
            log.debug("Directory connection was not read", ex);
        } catch (IllegalAccessException ex) {
            log.debug("Directory connection was not read", ex);
        } catch (InvocationTargetException ex) {
            log.debug("Directory connection was not read", ex);
        } catch (NoSuchMethodException ignored) {
            // This object keeps the directory another way.
        }
        return null;
    }

    private static Found fromResult(SearchResult result) throws NamingException {
        Map<String, String> attributes = new HashMap<String, String>();
        javax.naming.directory.Attributes raw = result.getAttributes();
        if (raw != null) {
            NamingEnumeration<? extends Attribute> all = raw.getAll();
            try {
                while (all.hasMore()) {
                    Attribute attribute = all.next();
                    Object value = attribute.get();
                    if (value != null && !(value instanceof byte[])) {
                        String text = String.valueOf(value).trim();
                        if (!text.isEmpty()) {
                            attributes.put(attribute.getID(), text);
                        }
                    }
                }
            } finally {
                all.close();
            }
        }
        return fromAttributes(attributes);
    }

    private static String namingContext(LdapContext context) throws NamingException {
        javax.naming.directory.Attributes attrs = context.getAttributes("", new String[]{"defaultNamingContext"});
        Attribute attr = attrs == null ? null : attrs.get("defaultNamingContext");
        if (attr == null || attr.get() == null) {
            return "";
        }
        return String.valueOf(attr.get()).trim();
    }

    private static Hashtable<String, String> environment(Directory directory) {
        Object loader = loader();
        if (loader == null) {
            return null;
        }
        Object remote = remote(loader, directory);
        Object mapper = mapper(remote, 0);
        if (mapper == null) {
            return null;
        }
        try {
            Object value = mapper.getClass().getMethod("getEnvironment").invoke(mapper);
            if (!(value instanceof Map)) {
                return null;
            }
            Hashtable<String, String> copy = new Hashtable<String, String>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                String text;
                if (entry.getValue() instanceof char[]) {
                    text = new String((char[]) entry.getValue());
                } else {
                    text = String.valueOf(entry.getValue());
                }
                copy.put(String.valueOf(entry.getKey()), text);
            }
            return copy;
        } catch (RuntimeException ex) {
            log.debug("Directory connection was not read", ex);
            return null;
        } catch (IllegalAccessException ex) {
            log.debug("Directory connection was not read", ex);
            return null;
        } catch (InvocationTargetException ex) {
            log.debug("Directory connection was not read", ex);
            return null;
        } catch (NoSuchMethodException ex) {
            log.debug("Directory connection was not read", ex);
            return null;
        }
    }

    private static Object loader() {
        Bundle bundle = FrameworkUtil.getBundle(DirectoryPeople.class);
        BundleContext context = bundle == null ? null : bundle.getBundleContext();
        if (context == null) {
            return null;
        }
        try {
            ServiceReference<?> reference = context.getServiceReference(LOADER);
            if (reference != null) {
                return context.getService(reference);
            }
            Bundle[] bundles = context.getBundles();
            if (bundles == null) {
                return null;
            }
            for (int i = 0; i < bundles.length; i++) {
                String symbolic = bundles[i].getSymbolicName();
                if (symbolic == null || symbolic.indexOf("crowd") < 0) {
                    continue;
                }
                try {
                    Class<?> type = bundles[i].loadClass(LOADER);
                    ServiceReference<?> found = context.getServiceReference(type);
                    if (found != null) {
                        return context.getService(found);
                    }
                } catch (ClassNotFoundException ignored) {
                    // This bundle does not hold the directory loader.
                } catch (IllegalStateException ignored) {
                    // The bundle is stopping.
                }
            }
        } catch (RuntimeException ex) {
            log.debug("Directory loader was not available", ex);
        }
        return null;
    }

    private static Object remote(Object loader, Directory directory) {
        Method[] methods = loader.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (!"getDirectory".equals(methods[i].getName()) || methods[i].getParameterTypes().length != 1) {
                continue;
            }
            try {
                Object opened = methods[i].invoke(loader, directory);
                if (opened != null) {
                    return opened;
                }
            } catch (IllegalArgumentException ignored) {
                // A different getDirectory takes another directory type.
            } catch (IllegalAccessException ex) {
                log.debug("Directory was not opened", ex);
            } catch (InvocationTargetException ex) {
                log.debug("Directory was not opened", ex);
            }
        }
        for (int i = 0; i < methods.length; i++) {
            if (!"getRawDirectory".equals(methods[i].getName()) || methods[i].getParameterTypes().length != 3) {
                continue;
            }
            try {
                return methods[i].invoke(loader, directory.getId(), directory.getImplementationClass(), directory.getAttributes());
            } catch (RuntimeException ex) {
                log.debug("Directory was not opened", ex);
            } catch (IllegalAccessException ex) {
                log.debug("Directory was not opened", ex);
            } catch (InvocationTargetException ex) {
                log.debug("Directory was not opened", ex);
            }
        }
        return null;
    }

    private static Object mapper(Object node, int depth) {
        if (node == null || depth > 3) {
            return null;
        }
        Class<?> type = node.getClass();
        while (type != null && !type.getName().startsWith("java.")) {
            Field[] fields;
            try {
                fields = type.getDeclaredFields();
            } catch (RuntimeException ex) {
                return null;
            }
            for (int i = 0; i < fields.length; i++) {
                String name = fields[i].getName().toLowerCase(java.util.Locale.ROOT);
                if (name.indexOf("mapper") < 0 && name.indexOf("ldap") < 0 && name.indexOf("directory") < 0) {
                    continue;
                }
                fields[i].setAccessible(true);
                Object value;
                try {
                    value = fields[i].get(node);
                } catch (IllegalAccessException ex) {
                    continue;
                }
                if (value == null || value instanceof String || value instanceof Number || value instanceof Map || value instanceof Boolean) {
                    continue;
                }
                if (hasEnvironment(value)) {
                    return value;
                }
                Object nested = mapper(value, depth + 1);
                if (nested != null) {
                    return nested;
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static boolean hasEnvironment(Object value) {
        try {
            value.getClass().getMethod("getEnvironment");
            return true;
        } catch (NoSuchMethodException ex) {
            return false;
        }
    }

    private static String safeAttribute(String attribute) {
        String text = attribute == null ? "" : attribute.trim();
        if (text.matches("[A-Za-z][A-Za-z0-9._-]*")) {
            return text;
        }
        return "sAMAccountName";
    }

    private static void addName(StringBuilder names, String attribute, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        names.append('(').append(attribute).append('=').append(escape(value.trim())).append(')');
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class Cached {
        private final Found found;
        private final long until;

        private Cached(Found found, long until) {
            this.found = found;
            this.until = until;
        }
    }
}
