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

import net.java.ao.DatabaseProvider;
import net.java.ao.EntityManager;
import net.java.ao.RawEntity;
import net.java.ao.schema.NameConverters;
import net.java.ao.schema.SchemaGenerator;
import net.java.ao.schema.ddl.DDLAction;
import net.java.ao.schema.ddl.DDLActionType;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLTable;
import net.java.ao.schema.ddl.SQLAction;
import net.java.ao.schema.ddl.SchemaReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Adds tables and columns that an upgraded database is missing.
 * Existing rows stay. Columns that are already present are not rewritten,
 * and statements that drop a table or a column are refused.
 */
public final class SchemaReady {
    private static final Logger log = LoggerFactory.getLogger(SchemaReady.class);
    private static final long RETRY_MS = 60000L;
    private static final Class<?>[] ENTITIES = new Class<?>[] {
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
            IdempotencyEntity.class
    };
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
            } catch (Throwable ex) {
                log.warn("Asset schema was not ready", ex);
            }
            try {
                addMissing(ao);
                ready = true;
                failedAt = 0L;
            } catch (Throwable ex) {
                failedAt = now;
                log.warn("Asset schema was not updated", ex);
            }
        }
    }

    public static synchronized void force() {
        ready = false;
        failedAt = 0L;
    }

    /** Only brand-new tables and brand-new columns. Never a drop or an in-place rewrite. */
    static boolean keeps(DDLActionType type) {
        return type == DDLActionType.CREATE || type == DDLActionType.ALTER_ADD_COLUMN;
    }

    /** A required column can be added to a table that already has rows. */
    static void prepareAdd(DDLField field) {
        if (field == null || !field.isNotNull() || field.getDefaultValue() != null) {
            return;
        }
        int jdbc = field.getJdbcType();
        if (jdbc == Types.BOOLEAN || jdbc == Types.BIT) {
            field.setDefaultValue(Boolean.FALSE);
        } else if (numeric(jdbc)) {
            field.setDefaultValue(Integer.valueOf(0));
        } else {
            field.setNotNull(false);
        }
    }

    /** A statement that would erase rows or a column that already holds them. */
    static boolean removes(String sql) {
        if (sql == null) {
            return false;
        }
        String upper = sql.toUpperCase(Locale.ROOT);
        return upper.contains("DROP TABLE") || upper.contains("DROP COLUMN")
                || upper.contains("TRUNCATE") || upper.contains("DELETE FROM");
    }

    private static boolean numeric(int jdbc) {
        return jdbc == Types.TINYINT || jdbc == Types.SMALLINT || jdbc == Types.INTEGER
                || jdbc == Types.BIGINT || jdbc == Types.NUMERIC || jdbc == Types.DECIMAL
                || jdbc == Types.FLOAT || jdbc == Types.REAL || jdbc == Types.DOUBLE;
    }

    private static void addMissing(ActiveObjects ao) throws Exception {
        EntityManager manager = entityManager(ao);
        if (manager == null) {
            throw new IllegalStateException("Active Objects schema is not reachable");
        }
        DatabaseProvider provider = manager.getProvider();
        NameConverters names = manager.getNameConverters();
        DDLTable[] desired = new DDLTable[ENTITIES.length];
        for (int i = 0; i < ENTITIES.length; i++) {
            desired[i] = describe(provider, names, ENTITIES[i]);
        }
        DDLTable[] current = SchemaReader.readSchema(provider, names, manager.getSchemaConfiguration());
        if (current == null) {
            current = new DDLTable[0];
        }
        DDLAction[] actions = SchemaReader.diffSchema(
                provider.getTypeManager(), desired, current, provider.isCaseSensitive());
        if (actions == null || actions.length == 0) {
            return;
        }
        actions = SchemaReader.sortTopologically(actions);
        Connection connection = provider.getConnection();
        Statement statement = null;
        boolean failed = false;
        try {
            statement = connection.createStatement();
            Set<String> done = new HashSet<String>();
            for (int i = 0; i < actions.length; i++) {
                DDLAction action = actions[i];
                if (action == null || !keeps(action.getActionType())) {
                    continue;
                }
                if (action.getActionType() == DDLActionType.ALTER_ADD_COLUMN) {
                    prepareAdd(action.getField());
                }
                try {
                    apply(provider, names, connection, statement, action, done);
                } catch (Throwable ex) {
                    rollback(connection);
                    DDLField field = action.getField();
                    if (action.getActionType() == DDLActionType.ALTER_ADD_COLUMN && field != null
                            && field.isNotNull() && !numeric(field.getJdbcType())
                            && field.getJdbcType() != Types.BOOLEAN && field.getJdbcType() != Types.BIT) {
                        field.setNotNull(false);
                        field.setDefaultValue(null);
                        try {
                            apply(provider, names, connection, statement, action, done);
                            continue;
                        } catch (Throwable again) {
                            rollback(connection);
                            ex = again;
                        }
                    }
                    failed = true;
                    log.warn("Could not add {}", action.getActionType(), ex);
                }
            }
        } finally {
            if (statement != null) {
                statement.close();
            }
            connection.close();
        }
        if (failed) {
            throw new IllegalStateException("Some asset columns were not added");
        }
    }

    @SuppressWarnings("unchecked")
    private static DDLTable describe(DatabaseProvider provider, NameConverters names, Class<?> type) {
        return SchemaGenerator.parseInterface(provider, names, (Class<? extends RawEntity<?>>) type);
    }

    private static void apply(DatabaseProvider provider, NameConverters names, Connection connection,
                              Statement statement, DDLAction action, Set<String> done) throws Exception {
        Iterable<SQLAction> sql = provider.renderAction(names, action);
        if (sql == null) {
            return;
        }
        for (SQLAction item : sql) {
            if (item == null || item.getStatement() == null) {
                continue;
            }
            String text = item.getStatement();
            if (removes(text)) {
                log.warn("Refused a statement that would remove stored values: {}", text);
                continue;
            }
            try {
                provider.executeUpdateForAction(statement, item, done);
                commit(connection);
            } catch (Throwable ex) {
                rollback(connection);
                if (essential(text)) {
                    if (ex instanceof Exception) {
                        throw (Exception) ex;
                    }
                    throw new IllegalStateException(ex);
                }
                log.warn("Skipped a schema extra that is not required to read saved rows", ex);
            }
        }
    }

    private static boolean essential(String sql) {
        String upper = sql.toUpperCase(Locale.ROOT);
        return upper.contains("CREATE TABLE") || upper.contains("ADD COLUMN");
    }

    private static void commit(Connection connection) {
        try {
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (Throwable ex) {
            log.warn("Asset schema change was not committed", ex);
        }
    }

    private static void rollback(Connection connection) {
        try {
            if (!connection.getAutoCommit()) {
                connection.rollback();
            }
        } catch (Throwable ignored) {
            // The next statement still runs on a clean connection when the database allows it.
        }
    }

    private static EntityManager entityManager(ActiveObjects ao) {
        return findManager(ao, new IdentityHashMap<Object, Boolean>(), 0);
    }

    private static EntityManager findManager(Object obj, IdentityHashMap<Object, Boolean> seen, int depth) {
        if (obj == null || depth > 6 || seen.containsKey(obj)) {
            return null;
        }
        seen.put(obj, Boolean.TRUE);
        if (obj instanceof EntityManager) {
            return (EntityManager) obj;
        }
        if (Proxy.isProxyClass(obj.getClass())) {
            EntityManager found = findManager(Proxy.getInvocationHandler(obj), seen, depth + 1);
            if (found != null) {
                return found;
            }
        }
        Class<?> type = obj.getClass();
        while (type != null && type != Object.class) {
            Field[] fields = type.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                if (Modifier.isStatic(fields[i].getModifiers())) {
                    continue;
                }
                try {
                    fields[i].setAccessible(true);
                    Object value = fields[i].get(obj);
                    if (!interesting(value)) {
                        continue;
                    }
                    EntityManager found = findManager(value, seen, depth + 1);
                    if (found != null) {
                        return found;
                    }
                } catch (Throwable ignored) {
                    // The next field may still hold the schema manager.
                }
            }
            type = type.getSuperclass();
        }
        Method[] methods = obj.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getParameterTypes().length != 0) {
                continue;
            }
            String name = methods[i].getName();
            if (!"delegate".equals(name) && !"claim".equals(name) && !"getDelegate".equals(name)) {
                continue;
            }
            try {
                Object value = methods[i].invoke(obj);
                EntityManager found = findManager(value, seen, depth + 1);
                if (found != null) {
                    return found;
                }
            } catch (Throwable ignored) {
                // Another delegate method may be the one that is ready.
            }
        }
        return null;
    }

    private static boolean interesting(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return false;
        }
        if (value instanceof EntityManager) {
            return true;
        }
        String name = value.getClass().getName();
        return name.contains("activeobjects") || name.contains("net.java.ao") || name.contains("Promise")
                || name.contains("InvocationHandler");
    }
}
