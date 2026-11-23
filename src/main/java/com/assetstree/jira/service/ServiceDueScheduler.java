package com.assetstree.jira.service;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.sal.api.scheduling.PluginScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs the service-date check every 15 minutes.
 * A scheduled report goes out in that window, and a missed day is sent on the next run.
 * The asset list also applies dates when someone opens it, so a missing scheduler
 * does not leave a due object unchanged on screen.
 */
public final class ServiceDueScheduler {
    static final String JOB = "com.assetstree.jira.service-due";
    private static final long QUARTER = 15L * 60L * 1000L;
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
                log.warn("Service dates will run on the fallback timer");
                startFallback();
                return;
            }
            try {
                scheduler.unscheduleJob(JOB);
            } catch (RuntimeException ignored) {
                // The job is not scheduled on the first start.
            }
            scheduler.scheduleJob(JOB, ServiceDueJob.class, new HashMap<String, Object>(),
                    new Date(System.currentTimeMillis() + 60000L), QUARTER);
        } catch (RuntimeException ex) {
            log.warn("Service dates were not scheduled, starting the fallback timer", ex);
            startFallback();
        }
    }

    /** Same 15-minute pass as {@link ServiceDueJob} when the Jira scheduler is down. */
    static void startFallback() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                sleep(60000L);
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        new ServiceDueJob().execute(Collections.<String, Object>emptyMap());
                    } catch (RuntimeException ex) {
                        log.warn("Service dates were not applied by the fallback timer", ex);
                    }
                    sleep(QUARTER);
                }
            }
        }, "asset-tree-service-due");
        thread.setDaemon(true);
        thread.start();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
