package com.assetstree.jira.service;

import com.assetstree.jira.ao.AssetIssueLinkEntity;
import com.assetstree.jira.model.AssetException;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.database.DatabaseAccessor;

import net.java.ao.DBParam;
import net.java.ao.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Writes a request link when the shared legacy table still has required
 * columns that Active Objects does not fill in.
 */
final class IssueLinkStore {
    private static final Logger log = LoggerFactory.getLogger(IssueLinkStore.class);

    static final class Column {
        String name;
        int type;
        boolean required;
        boolean identity;
        boolean hasDefault;

        Column(String name, int type, boolean required, boolean identity, boolean hasDefault) {
            this.name = name;
            this.type = type;
            this.required = required;
            this.identity = identity;
            this.hasDefault = hasDefault;
        }
    }

    private IssueLinkStore() {
    }

    /** Request-link columns, including the legacy required link type. */
    static DBParam[] parameters(int assetId, long issueId, String status) {
        return new DBParam[] {
                new DBParam("ASSET_ID", Integer.valueOf(assetId)),
                new DBParam("ISSUE_ID", Long.valueOf(issueId)),
                new DBParam("STATUS_BEFORE", status == null ? "" : status),
                new DBParam("LINK_TYPE", "")
        };
    }

    static void write(final ActiveObjects ao, final int assetId, final long issueId, final String status) {
        DatabaseAccessor database = ComponentAccessor.getComponent(DatabaseAccessor.class);
        if (database == null) {
            throw new AssetException(500, "asset-tree.error.link.failed", "database");
        }
        try {
            database.runInTransaction(new Function<Connection, Boolean>() {
                @Override
                public Boolean apply(Connection connection) {
                    try {
                        store(connection, ao, assetId, issueId, status);
                    } catch (SQLException ex) {
                        throw new AssetException(500, "asset-tree.error.link.failed", reason(ex));
                    }
                    return Boolean.TRUE;
                }
            });
        } catch (AssetException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            AssetException cause = causeOf(ex);
            if (cause != null) {
                throw cause;
            }
            log.error("ASSET-LINK direct insert failed for asset {} and issue {}", Integer.valueOf(assetId), Long.valueOf(issueId), ex);
            throw new AssetException(500, "asset-tree.error.link.failed", reason(ex));
        }
    }

    private static void store(Connection connection, ActiveObjects ao, int assetId, long issueId, String status) throws SQLException {
        String table = tableName(connection, ao);
        if (table == null) {
            throw new AssetException(500, "asset-tree.error.link.failed", "table");
        }
        String quote = connection.getMetaData().getIdentifierQuoteString();
        String product = connection.getMetaData().getDatabaseProductName();
        boolean oracle = product != null && product.toUpperCase(Locale.ROOT).contains("ORACLE");
        String plain = table.indexOf('.') < 0 ? table : table.substring(table.lastIndexOf('.') + 1);
        String schema = table.indexOf('.') < 0 ? null : table.substring(0, table.lastIndexOf('.'));
        List<Column> columns = readColumns(connection, schema, plain);
        if (columns.isEmpty()) {
            throw new AssetException(500, "asset-tree.error.link.failed", "columns " + table);
        }
        if (present(connection, quote, table, columns, assetId, issueId)) {
            throw new AssetException(409, "asset-tree.error.issue.duplicate");
        }
        try {
            insert(connection, quote, table, values(columns, assetId, issueId, status, null, oracle));
        } catch (SQLException ex) {
            if (!missingIdentity(ex)) {
                throw ex;
            }
            long next = nextId(connection, quote, table, named(columns, "ID"));
            insert(connection, quote, table, values(columns, assetId, issueId, status, Long.valueOf(next), oracle));
        }
    }

    private static String tableName(Connection connection, ActiveObjects ao) throws SQLException {
        EntityManager manager = SchemaReady.managerOf(ao);
        if (manager != null) {
            String named = manager.getNameConverters().getTableNameConverter().getName(AssetIssueLinkEntity.class);
            if (named != null && !named.trim().isEmpty()) {
                return named.trim();
            }
        }
        PreparedStatement query = connection.prepareStatement(
                "SELECT TABLE_SCHEMA, TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(COLUMN_NAME) = 'ISSUE_ID' AND UPPER(TABLE_NAME) LIKE '%AST_LINK'");
        try {
            ResultSet rows = query.executeQuery();
            try {
                if (!rows.next()) {
                    return null;
                }
                String schema = rows.getString(1);
                String table = rows.getString(2);
                if (table == null || !SchemaReady.identifier(table)) {
                    return null;
                }
                if (schema != null && SchemaReady.identifier(schema)) {
                    return schema + "." + table;
                }
                return table;
            } finally {
                rows.close();
            }
        } finally {
            query.close();
        }
    }

    private static AssetException causeOf(Throwable failure) {
        Throwable current = failure;
        int guard = 0;
        while (current != null && guard < 8) {
            if (current instanceof AssetException) {
                return (AssetException) current;
            }
            current = current.getCause();
            guard++;
        }
        return null;
    }

    static Map<String, Object> values(List<Column> columns, int assetId, long issueId, String status, Long forcedId, boolean oracle) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        String filler = oracle ? " " : "";
        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            String key = column.name == null ? "" : column.name.toUpperCase(Locale.ROOT);
            if ("ID".equals(key)) {
                if (forcedId != null) {
                    row.put(column.name, forcedId);
                }
                continue;
            }
            if ("ASSET_ID".equals(key)) {
                row.put(column.name, Integer.valueOf(assetId));
                continue;
            }
            if ("ISSUE_ID".equals(key)) {
                row.put(column.name, Long.valueOf(issueId));
                continue;
            }
            if ("STATUS_BEFORE".equals(key)) {
                row.put(column.name, status == null ? filler : status);
                continue;
            }
            if (!column.required || column.hasDefault || column.identity) {
                continue;
            }
            if (numeric(column.type)) {
                row.put(column.name, Integer.valueOf(0));
            } else if (column.type == Types.BIT || column.type == Types.BOOLEAN) {
                row.put(column.name, Boolean.FALSE);
            } else if (column.type == Types.DATE || column.type == Types.TIMESTAMP || column.type == Types.TIME) {
                row.put(column.name, new java.sql.Timestamp(0L));
            } else {
                row.put(column.name, filler);
            }
        }
        return row;
    }

    static String insertSql(String table, String quote, List<String> names) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(quotedName(table, quote)).append(" (");
        StringBuilder marks = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                sql.append(", ");
                marks.append(", ");
            }
            sql.append(quoted(names.get(i), quote));
            marks.append('?');
        }
        sql.append(") VALUES (").append(marks).append(')');
        return sql.toString();
    }

    static String reason(Throwable failure) {
        Throwable root = failure == null ? null : failure;
        int guard = 0;
        while (root != null && root.getCause() != null && root.getCause() != root && guard < 8) {
            root = root.getCause();
            guard++;
        }
        String text = root == null ? "" : root.getMessage();
        if (text == null || text.trim().isEmpty()) {
            text = root == null ? "error" : root.getClass().getSimpleName();
        }
        text = text.replace('\r', ' ').replace('\n', ' ').trim();
        if (text.length() > 160) {
            text = text.substring(0, 160);
        }
        return text;
    }

    private static List<Column> readColumns(Connection connection, String schema, String table) throws SQLException {
        if (table == null || table.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String[] schemas = schema == null ? new String[] {null} : new String[] {schema, schema.toUpperCase(Locale.ROOT), null};
        String[] names = new String[] {table, table.toUpperCase(Locale.ROOT), table.toLowerCase(Locale.ROOT)};
        for (int s = 0; s < schemas.length; s++) {
            for (int i = 0; i < names.length; i++) {
                ResultSet meta = connection.getMetaData().getColumns(null, schemas[s], names[i], null);
                try {
                    List<Column> found = columns(meta, names[i]);
                    if (!found.isEmpty()) {
                        return found;
                    }
                } finally {
                    meta.close();
                }
            }
        }
        return Collections.emptyList();
    }

    private static List<Column> columns(ResultSet meta, String table) throws SQLException {
        List<Column> found = new ArrayList<Column>();
        while (meta.next()) {
            String owner = meta.getString("TABLE_NAME");
            if (owner != null && !owner.equalsIgnoreCase(table)) {
                continue;
            }
            String name = meta.getString("COLUMN_NAME");
            int type = meta.getInt("DATA_TYPE");
            boolean required = "NO".equalsIgnoreCase(meta.getString("IS_NULLABLE"));
            String fallback = meta.getString("COLUMN_DEF");
            boolean hasDefault = fallback != null && !fallback.trim().isEmpty();
            boolean identity = false;
            try {
                identity = "YES".equalsIgnoreCase(meta.getString("IS_AUTOINCREMENT"));
            } catch (SQLException ignored) {
                identity = false;
            }
            found.add(new Column(name, type, required, identity, hasDefault));
        }
        return found;
    }

    private static boolean present(Connection connection, String quote, String table, List<Column> columns,
                                   int assetId, long issueId) throws SQLException {
        String asset = named(columns, "ASSET_ID");
        String issue = named(columns, "ISSUE_ID");
        if (asset == null || issue == null) {
            return false;
        }
        PreparedStatement query = connection.prepareStatement("SELECT COUNT(*) FROM " + quotedName(table, quote)
                + " WHERE " + quoted(asset, quote) + " = ? AND " + quoted(issue, quote) + " = ?");
        try {
            query.setInt(1, assetId);
            query.setLong(2, issueId);
            ResultSet rows = query.executeQuery();
            try {
                return rows.next() && rows.getInt(1) > 0;
            } finally {
                rows.close();
            }
        } finally {
            query.close();
        }
    }

    private static void insert(Connection connection, String quote, String table, Map<String, Object> row) throws SQLException {
        List<String> names = new ArrayList<String>(row.keySet());
        PreparedStatement statement = connection.prepareStatement(insertSql(table, quote, names));
        try {
            for (int i = 0; i < names.size(); i++) {
                statement.setObject(i + 1, row.get(names.get(i)));
            }
            statement.executeUpdate();
        } finally {
            statement.close();
        }
    }

    private static long nextId(Connection connection, String quote, String table, String idColumn) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT COALESCE(MAX(" + quoted(idColumn, quote) + "), 0) + 1 FROM " + quotedName(table, quote));
        try {
            ResultSet rows = query.executeQuery();
            try {
                return rows.next() ? rows.getLong(1) : 1L;
            } finally {
                rows.close();
            }
        } finally {
            query.close();
        }
    }

    private static String named(List<Column> columns, String wanted) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).name != null && columns.get(i).name.equalsIgnoreCase(wanted)) {
                return columns.get(i).name;
            }
        }
        return null;
    }

    private static boolean missingIdentity(SQLException failure) {
        String text = reason(failure).toLowerCase(Locale.ROOT);
        return text.contains("id") && (text.contains("null") || text.contains("default") || text.contains("identity"));
    }

    private static boolean numeric(int type) {
        return type == Types.TINYINT || type == Types.SMALLINT || type == Types.INTEGER
                || type == Types.BIGINT || type == Types.NUMERIC || type == Types.DECIMAL
                || type == Types.FLOAT || type == Types.REAL || type == Types.DOUBLE;
    }

    static String quotedName(String name, String quote) {
        if (name == null) {
            return "";
        }
        int dot = name.indexOf('.');
        if (dot > 0 && dot < name.length() - 1) {
            String schema = name.substring(0, dot);
            String table = name.substring(dot + 1);
            if (SchemaReady.identifier(schema) && SchemaReady.identifier(table)) {
                return quoted(schema, quote) + "." + quoted(table, quote);
            }
        }
        return quoted(name, quote);
    }

    static String quoted(String name, String quote) {
        String mark = quote == null ? "" : quote.trim();
        if (mark.isEmpty() || " ".equals(quote)) {
            return name;
        }
        return mark + name.replace(mark, mark + mark) + mark;
    }

}
