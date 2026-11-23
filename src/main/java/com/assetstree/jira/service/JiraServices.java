package com.assetstree.jira.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * Looks up Active Objects for this plugin.
 * The service is an OSGi {@link org.osgi.framework.ServiceFactory}: the instance
 * is bound to the calling bundle. A lookup through Jira's own bundle does not
 * see this plugin's entities.
 */
public final class JiraServices {
    private static volatile ActiveObjects cached;

    private JiraServices() {
    }

    public static ActiveObjects activeObjects() {
        ActiveObjects current = cached;
        if (current != null) {
            return current;
        }
        synchronized (JiraServices.class) {
            if (cached == null) {
                cached = lookup();
            }
            return cached;
        }
    }

    private static ActiveObjects lookup() {
        Bundle bundle = FrameworkUtil.getBundle(JiraServices.class);
        if (bundle == null) {
            throw new IllegalStateException("Asset Tree is not running as an OSGi bundle");
        }
        BundleContext context = bundle.getBundleContext();
        if (context == null) {
            throw new IllegalStateException("Asset Tree bundle is not active");
        }
        ServiceReference<?> reference = context.getServiceReference(ActiveObjects.class.getName());
        if (reference == null) {
            throw new IllegalStateException("Active Objects is not available");
        }
        Object service = context.getService(reference);
        if (!(service instanceof ActiveObjects)) {
            throw new IllegalStateException("Active Objects service cannot be used from Asset Tree");
        }
        return (ActiveObjects) service;
    }
}
