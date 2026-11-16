package com.assetstree.jira.service;

import org.junit.Test;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IssueLinkStoreTest {
    @Test
    public void requiredLegacyColumnsAreFilledAndIdentityIsLeftToTheDatabase() {
        List<IssueLinkStore.Column> columns = new ArrayList<IssueLinkStore.Column>();
        columns.add(new IssueLinkStore.Column("ID", Types.INTEGER, true, true, true));
        columns.add(new IssueLinkStore.Column("ASSET_ID", Types.INTEGER, true, false, false));
        columns.add(new IssueLinkStore.Column("ISSUE_ID", Types.BIGINT, true, false, false));
        columns.add(new IssueLinkStore.Column("STATUS_BEFORE", Types.VARCHAR, false, false, false));
        columns.add(new IssueLinkStore.Column("LINK_TYPE", Types.VARCHAR, true, false, false));
        columns.add(new IssueLinkStore.Column("FROM_ID", Types.INTEGER, true, false, false));
        columns.add(new IssueLinkStore.Column("TO_ID", Types.INTEGER, true, false, false));
        columns.add(new IssueLinkStore.Column("INBOUND", Types.VARCHAR, false, false, false));

        Map<String, Object> row = IssueLinkStore.values(columns, 7, 53L, "in_use", null, false);
        assertFalse(row.containsKey("ID"));
        assertEquals(Integer.valueOf(7), row.get("ASSET_ID"));
        assertEquals(Long.valueOf(53L), row.get("ISSUE_ID"));
        assertEquals("in_use", row.get("STATUS_BEFORE"));
        assertEquals("", row.get("LINK_TYPE"));
        assertEquals(Integer.valueOf(0), row.get("FROM_ID"));
        assertEquals(Integer.valueOf(0), row.get("TO_ID"));
        assertFalse(row.containsKey("INBOUND"));
    }

    @Test
    public void explicitIdIsUsedWhenTheDatabaseHasNoIdentity() {
        List<IssueLinkStore.Column> columns = Arrays.asList(
                new IssueLinkStore.Column("ID", Types.INTEGER, true, false, false));
        Map<String, Object> row = IssueLinkStore.values(columns, 1, 2L, "", Long.valueOf(9L), false);
        assertEquals(Long.valueOf(9L), row.get("ID"));
    }

    @Test
    public void insertNamesStayQuoted() {
        String sql = IssueLinkStore.insertSql("PUBLIC.AO_X_AST_LINK", "\"", Arrays.asList("ASSET_ID", "LINK_TYPE"));
        assertEquals("INSERT INTO \"PUBLIC\".\"AO_X_AST_LINK\" (\"ASSET_ID\", \"LINK_TYPE\") VALUES (?, ?)", sql);
        assertEquals("AO_X", IssueLinkStore.quoted("AO_X", " "));
    }

    @Test
    public void requestLinkCarriesTheLegacyRequiredType() {
        net.java.ao.DBParam[] params = IssueLinkStore.parameters(49, 10402L, "in_use");
        assertEquals("LINK_TYPE", params[3].getField());
        assertEquals("", params[3].getValue());
        assertEquals(Long.valueOf(10402L), params[1].getValue());
    }

    @Test
    public void failureReasonStaysShort() {
        SQLException deep = new SQLException("null value in column \"LINK_TYPE\" violates not-null constraint");
        assertTrue(IssueLinkStore.reason(new RuntimeException("wrapped", deep)).contains("LINK_TYPE"));
    }
}
