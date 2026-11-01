package com.assetstree.jira.service;

import com.assetstree.jira.ao.AssetActivityEntity;
import com.assetstree.jira.ao.AssetAttributeEntity;
import com.assetstree.jira.ao.AssetCommentEntity;
import com.assetstree.jira.ao.AssetCounterEntity;
import com.assetstree.jira.ao.AssetEntity;
import com.assetstree.jira.ao.AssetFieldEntity;
import com.assetstree.jira.ao.AssetFileEntity;
import com.assetstree.jira.ao.AssetIssueLinkEntity;
import com.assetstree.jira.ao.AssetLinkEntity;
import com.assetstree.jira.ao.AssetPeerEntity;
import com.assetstree.jira.ao.AssetShareEntity;
import com.assetstree.jira.ao.AssetTypeEntity;
import com.assetstree.jira.ao.CardTemplateEntity;
import com.assetstree.jira.ao.FloorEntity;
import com.assetstree.jira.ao.FlowGraphEntity;
import com.assetstree.jira.ao.FlowRuleEntity;
import com.assetstree.jira.ao.IdempotencyEntity;
import com.assetstree.jira.ao.InventoryMarkEntity;
import com.assetstree.jira.ao.LifecycleEntity;
import com.assetstree.jira.ao.PinEntity;
import com.assetstree.jira.ao.PortalRuleConditionEntity;
import com.assetstree.jira.ao.PortalRuleEntity;
import com.assetstree.jira.ao.ProjectAssetKeyEntity;
import com.assetstree.jira.ao.ProjectGrantEntity;
import com.assetstree.jira.ao.ProjectStatusEntity;
import com.assetstree.jira.ao.ProjectViewEntity;
import com.assetstree.jira.ao.RoomEntity;
import com.assetstree.jira.ao.SavedReportEntity;
import com.assetstree.jira.ao.ServicePlanEntity;
import com.assetstree.jira.ao.StaffDutyEntity;
import com.assetstree.jira.ao.TypeSchemaEntity;
import com.assetstree.jira.ao.WebhookEntity;
import com.atlassian.activeobjects.external.ActiveObjects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Adds columns that a copied database is missing. Active Objects only alters
 * tables while the plugin starts, so an upgraded jar can fail the asset list
 * until {@code migrate} runs.
 */
public final class SchemaReady {
    private static final Logger log = LoggerFactory.getLogger(SchemaReady.class);
    private static final long RETRY_MS = 60000L;
    private static volatile boolean ready;
    private static volatile long failedAt;

    private SchemaReady() {
    }

    public static void ensure(ActiveObjects ao) {
        if (ao == null || ready) {
            return;
        }
        synchronized (SchemaReady.class) {
            if (ready) {
                return;
            }
            long now = System.currentTimeMillis();
            if (failedAt != 0L && now - failedAt < RETRY_MS) {
                return;
            }
            try {
                if (ao.moduleMetaData() != null && !ao.moduleMetaData().isInitialized()) {
                    ao.moduleMetaData().awaitInitialization(30L, TimeUnit.SECONDS);
                }
            } catch (Exception ex) {
                log.warn("Asset schema was not ready", ex);
            }
            try {
                ao.migrate(
                        AssetEntity.class,
                        AssetAttributeEntity.class,
                        AssetIssueLinkEntity.class,
                        AssetPeerEntity.class,
                        AssetLinkEntity.class,
                        AssetTypeEntity.class,
                        AssetCounterEntity.class,
                        ProjectAssetKeyEntity.class,
                        ProjectViewEntity.class,
                        AssetFieldEntity.class,
                        InventoryMarkEntity.class,
                        AssetCommentEntity.class,
                        AssetFileEntity.class,
                        AssetShareEntity.class,
                        AssetActivityEntity.class,
                        ProjectStatusEntity.class,
                        ProjectGrantEntity.class,
                        PortalRuleEntity.class,
                        PortalRuleConditionEntity.class,
                        FlowRuleEntity.class,
                        FlowGraphEntity.class,
                        ServicePlanEntity.class,
                        StaffDutyEntity.class,
                        FloorEntity.class,
                        RoomEntity.class,
                        PinEntity.class,
                        SavedReportEntity.class,
                        LifecycleEntity.class,
                        CardTemplateEntity.class,
                        TypeSchemaEntity.class,
                        WebhookEntity.class,
                        IdempotencyEntity.class);
                ready = true;
                failedAt = 0L;
            } catch (RuntimeException ex) {
                failedAt = now;
                log.warn("Asset schema was not updated", ex);
            }
        }
    }

    public static synchronized void force() {
        ready = false;
        failedAt = 0L;
    }
}
