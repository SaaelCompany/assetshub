package com.assetstree.jira.service;

import com.assetstree.jira.model.AssetException;
import com.assetstree.jira.model.TniComputer;
import com.assetstree.jira.model.TniDirectory;

import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/** Reads computer accounts from Active Directory. The filter is fixed and referrals stay off. */
public final class TniLdap {
    private static final String FILTER = "(objectCategory=computer)";
    private static final String[] ATTRIBUTES = {
            "cn", "dNSHostName", "operatingSystem", "operatingSystemVersion",
            "distinguishedName", "description", "objectGUID", "userAccountControl"
    };

    private TniLdap() {
    }

    public static List<TniComputer> read(String host, int port, boolean ssl, String base, String account, String password) {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, (ssl ? "ldaps://" : "ldap://") + host + ":" + port);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, account);
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put(Context.REFERRAL, "ignore");
        env.put("com.sun.jndi.ldap.connect.timeout", "8000");
        env.put("com.sun.jndi.ldap.read.timeout", "20000");
        env.put("java.naming.ldap.attributes.binary", "objectGUID");
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        ClassLoader system = ClassLoader.getSystemClassLoader();
        if (system != null) {
            Thread.currentThread().setContextClassLoader(system);
        }
        LdapContext context = null;
        try {
            context = new InitialLdapContext(env, null);
            return pages(context, base);
        } catch (AuthenticationException ex) {
            throw new AssetException(401, TniDirectory.AUTH);
        } catch (CommunicationException ex) {
            throw new AssetException(502, TniDirectory.CONNECT);
        } catch (NamingException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            if (message.indexOf("error code 49") >= 0) {
                throw new AssetException(401, TniDirectory.AUTH);
            }
            if (message.indexOf("error code 32") >= 0 || message.indexOf("name not found") >= 0) {
                throw new AssetException(400, TniDirectory.BASE);
            }
            throw new AssetException(502, TniDirectory.CONNECT);
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

    private static List<TniComputer> pages(LdapContext context, String base) throws NamingException {
        List<TniComputer> list = new ArrayList<TniComputer>();
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningAttributes(ATTRIBUTES);
        controls.setTimeLimit(20000);
        byte[] cookie = null;
        try {
            do {
                context.setRequestControls(new Control[]{
                        new PagedResultsControl(500, cookie, Control.NONCRITICAL)
                });
                NamingEnumeration<SearchResult> found = context.search(base, FILTER, controls);
                try {
                    while (found.hasMore() && list.size() < TniDirectory.MAX_ROWS) {
                        TniComputer computer = computer(found.next());
                        if (computer != null) {
                            list.add(computer);
                        }
                    }
                } finally {
                    found.close();
                }
                cookie = cookie(context.getResponseControls());
            } while (cookie != null && cookie.length > 0 && list.size() < TniDirectory.MAX_ROWS);
        } catch (IOException ex) {
            throw new AssetException(502, TniDirectory.CONNECT);
        }
        return list;
    }

    private static byte[] cookie(Control[] controls) {
        if (controls == null) {
            return null;
        }
        for (int i = 0; i < controls.length; i++) {
            if (controls[i] instanceof PagedResultsResponseControl) {
                return ((PagedResultsResponseControl) controls[i]).getCookie();
            }
        }
        return null;
    }

    private static TniComputer computer(SearchResult result) throws NamingException {
        String guid = "";
        Attribute raw = result.getAttributes() == null ? null : result.getAttributes().get("objectGUID");
        if (raw != null && raw.get() instanceof byte[]) {
            guid = TniDirectory.guid((byte[]) raw.get());
        }
        String dns = text(result, "dNSHostName");
        String name = text(result, "cn");
        String identity = !guid.isEmpty() ? guid : dns.toLowerCase();
        if (identity.isEmpty()) {
            identity = name.toLowerCase();
        }
        int uac = 0;
        String flag = text(result, "userAccountControl");
        if (!flag.isEmpty()) {
            try {
                uac = Integer.parseInt(flag.trim());
            } catch (NumberFormatException ignored) {
                uac = 0;
            }
        }
        return TniDirectory.computer("ad", identity, name,
                text(result, "operatingSystem"), text(result, "operatingSystemVersion"),
                "", "", "", "", "", "", text(result, "distinguishedName"), text(result, "description"),
                (uac & 2) == 2);
    }

    private static String text(SearchResult result, String name) throws NamingException {
        if (result.getAttributes() == null) {
            return "";
        }
        Attribute attribute = result.getAttributes().get(name);
        if (attribute == null || attribute.get() == null) {
            return "";
        }
        return String.valueOf(attribute.get());
    }
}
