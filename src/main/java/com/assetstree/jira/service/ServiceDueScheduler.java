package com.assetstree.jira.service;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.sal.api.scheduling.PluginScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs the service-date check every hour.
 * The asset list also applies dates when someone opens it, so a missing scheduler
 * does not leave a due object unchanged on screen.
 */
public final class ServiceDueScheduler {
    static final String JOB = "com.assetstree.jira.service-due";
    private static final long HOUR = 60L * 60L * 1000L;
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final Logger log = LoggerFactory.getLogger(ServiceDueScheduler.class);

    private ServiceDueScheduler() {
    }

    public static void ensureStarted() {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        try {
            PluginScheduler scheduler = ComponentAccessor.getOSGiComponentInstanceOfType(PluginScheduler.class);
            if (scheduler == null) {
                STARTED.set(false);
                log.warn("Service dates will run when the asset list is opened");
                return;
            }
            try {
                scheduler.unscheduleJob(JOB);
            } catch (RuntimeException ignored) {
                // The job is not scheduled on the first start.
            }
            scheduler.scheduleJob(JOB, ServiceDueJob.class, new HashMap<String, Object>(),
                    new Date(System.currentTimeMillis() + 60000L), HOUR);
        } catch (RuntimeException ex) {
            STARTED.set(false);
            log.warn("Service dates were not scheduled", ex);
        }
    }
}
