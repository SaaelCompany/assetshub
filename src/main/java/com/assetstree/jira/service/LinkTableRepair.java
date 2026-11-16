package com.assetstree.jira.service;

import com.assetstree.jira.ao.AssetLinkEntity;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.database.DatabaseAccessor;

import net.java.ao.DBParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Makes the old shared link column optional and copies asset-to-asset rows
 * into their own table. Issue rows stay where they are.
 */
final class LinkTableRepair {
    private static final Logger log = LoggerFactory.getLogger(LinkTableRepair.class);

    private LinkTableRepair() {
    }

    static boolean repair(ActiveObjects ao) {
        DatabaseAccessor database = ComponentAccessor.getComponent(DatabaseAccessor.class);
        if (database == null || ao == null) {
            return false;
        }
        final List<String[]> relations = new ArrayList<String[]>();
        try {
            Boolean done = database.runInTransaction(new java.util.function.Function<Connection, Boolean>() {
                @Override
                public Boolean apply(Connection connection) {
                    try {
                        String[] table = locate(connection);
                        if (table == null) {
                            return Boolean.TRUE;
                        }
                        relax(connection, table[0], table[1]);
                        relations.addAll(readRelations(connection, table[0], table[1]));
                        return Boolean.TRUE;
                    } catch (SQLException ex) {
                        log.warn("Request link table was not read", ex);
                        return Boolean.FALSE;
                    }
                }
            });
            if (!Boolean.TRUE.equals(done)) {
                return false;
            }
        } catch (RuntimeException ex) {
            log.warn("Request link table was not repaired", ex);
            return false;
        }
        return store(ao, relations);
    }

    /** Statements that only make LINK_TYPE optional. They do not delete rows. */
    static List<String> statements(String schema, String table) {
        List<String> sql = new ArrayList<String>();
        if (!SchemaReady.identifier(schema) || !SchemaReady.identifier(table)) {
            return sql;
        }
        String qualified = "\"" + schema + "\".\"" + table + "\"";
        String plain = schema + "." + table;
        sql.add("ALTER TABLE " + qualified + " ALTER COLUMN \"LINK_TYPE\" SET NULL");
        sql.add("ALTER TABLE " + qualified + " ALTER COLUMN \"LINK_TYPE\" SET DEFAULT ''");
        sql.add("ALTER TABLE " + plain + " ALTER COLUMN LINK_TYPE SET NULL");
        sql.add("ALTER TABLE " + plain + " ALTER COLUMN LINK_TYPE SET DEFAULT ''");
        sql.addAll(SchemaReady.relaxColumn(table, "LINK_TYPE", true));
        return sql;
    }

    private static String[] locate(Connection connection) throws SQLException {
        PreparedStatement query = connection.prepareStatement(
                "SELECT TABLE_SCHEMA, TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE UPPER(COLUMN_NAME) = 'LINK_TYPE' AND UPPER(TABLE_NAME) LIKE '%AST_LINK'");
        try {
            ResultSet rows = query.executeQuery();
            try {
                if (!rows.next()) {
                    return null;
                }
                String schema = rows.getString(1);
                String table = rows.getString(2);
                if (!SchemaReady.identifier(schema) || !SchemaReady.identifier(table)) {
                    return null;
                }
                return new String[] {schema, table};
            } finally {
                rows.close();
            }
        } finally {
            query.close();
        }
    }

    private static void relax(Connection connection, String schema, String table) {
        List<String> sql = statements(schema, table);
        Statement statement = null;
        try {
            statement = connection.createStatement();
            for (int i = 0; i < sql.size(); i++) {
                try {
                    statement.execute(sql.get(i));
                } catch (SQLException ignored) {
                    // Another dialect owns this database. The next statement still runs.
                }
            }
        } catch (SQLException ex) {
            log.warn("LINK_TYPE was not made optional", ex);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {
                    // The connection closes with the transaction.
                }
            }
        }
    }

    private static List<String[]> readRelations(Connection connection, String schema, String table) throws SQLException {
        List<String[]> found = new ArrayList<String[]>();
        String sql = "SELECT FROM_ID, TO_ID, LINK_TYPE, INBOUND, ATTRS FROM \"" + schema + "\".\"" + table
                + "\" WHERE LINK_TYPE IS NOT NULL AND LINK_TYPE <> '' AND FROM_ID > 0 AND TO_ID > 0";
        Statement statement = connection.createStatement();
        try {
            ResultSet rows = statement.executeQuery(sql);
            try {
                while (rows.next()) {
                    found.add(new String[] {
                            String.valueOf(rows.getInt(1)),
                            String.valueOf(rows.getInt(2)),
                            rows.getString(3),
                            rows.getString(4),
                            rows.getString(5)
                    });
                }
            } finally {
                rows.close();
            }
        } catch (SQLException ex) {
            log.warn("Asset links were not copied yet", ex);
        } finally {
            statement.close();
        }
        return found;
    }

    private static boolean store(ActiveObjects ao, List<String[]> relations) {
        for (int i = 0; i < relations.size(); i++) {
            String[] row = relations.get(i);
            int from;
            int to;
            try {
                from = Integer.parseInt(row[0]);
                to = Integer.parseInt(row[1]);
            } catch (NumberFormatException ex) {
                continue;
            }
            String type = row[2] == null ? "" : row[2].trim();
            if (from <= 0 || to <= 0 || type.isEmpty()) {
                continue;
            }
            try {
                AssetLinkEntity[] present = ao.find(AssetLinkEntity.class,
                        "FROM_ID = ? AND TO_ID = ? AND LINK_TYPE = ?", Integer.valueOf(from), Integer.valueOf(to), type);
                if (present != null && present.length > 0) {
                    continue;
                }
                ao.create(AssetLinkEntity.class,
                        new DBParam("FROM_ID", Integer.valueOf(from)),
                        new DBParam("TO_ID", Integer.valueOf(to)),
                        new DBParam("LINK_TYPE", type),
                        new DBParam("INBOUND", row[3] == null ? "" : row[3]),
                        new DBParam("ATTRS", row[4] == null ? "" : row[4]));
            } catch (RuntimeException ex) {
                log.warn("Could not keep the asset link {} -> {}", Integer.valueOf(from), Integer.valueOf(to), ex);
                return false;
            }
        }
        return true;
    }
}
