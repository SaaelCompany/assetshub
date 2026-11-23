package com.assetstree.jira.service;

import com.atlassian.sal.api.scheduling.PluginJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/** Check every 15 minutes that moves due equipment and queues the report letter. */
public class ServiceDueJob implements PluginJob {
    private static final Logger log = LoggerFactory.getLogger(ServiceDueJob.class);

    @Override
    public void execute(Map<String, Object> jobDataMap) {
        try {
            AssetBridge.service().applyDuePlans();
        } catch (RuntimeException ex) {
            log.warn("Service dates were not applied", ex);
        }
        try {
            AssetBridge.service().sendScheduledReports();
        } catch (RuntimeException ex) {
            log.warn("Report mail was not sent", ex);
        }
    }
}
