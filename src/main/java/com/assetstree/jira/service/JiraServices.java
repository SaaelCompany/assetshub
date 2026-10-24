package com.assetstree.jira.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

final class JiraServices {
    private JiraServices() {
    }

    static ActiveObjects activeObjects() {
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
