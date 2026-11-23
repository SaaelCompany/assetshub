package com.assetstree.jira.service;

import com.assetstree.jira.ao.AssetIssueLinkEntity;

import net.java.ao.schema.NotNull;
import net.java.ao.schema.ddl.DDLActionType;
import net.java.ao.schema.ddl.DDLField;

import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SchemaReadyTest {
    @Test
    public void repairOnlyCreatesTablesAndAddsColumns() {
        assertTrue(SchemaReady.keeps(DDLActionType.CREATE));
        assertTrue(SchemaReady.keeps(DDLActionType.ALTER_ADD_COLUMN));
        assertFalse(SchemaReady.keeps(DDLActionType.DROP));
        assertFalse(SchemaReady.keeps(DDLActionType.ALTER_DROP_COLUMN));
        assertFalse(SchemaReady.keeps(DDLActionType.ALTER_CHANGE_COLUMN));
        assertFalse(SchemaReady.keeps(DDLActionType.DROP_INDEX));
    }

    @Test
    public void requiredNumberColumnGetsADefaultSoExistingRowsStay() {
        DDLField number = new DDLField();
        number.setNotNull(true);
        number.setJdbcType(Types.INTEGER);
        SchemaReady.prepareAdd(number);
        assertEquals(Integer.valueOf(0), number.getDefaultValue());
        assertTrue(number.isNotNull());
    }

    @Test
    public void requiredTextColumnBecomesOptionalSoExistingRowsStay() {
        DDLField text = new DDLField();
        text.setNotNull(true);
        text.setJdbcType(Types.VARCHAR);
        SchemaReady.prepareAdd(text);
        assertFalse(text.isNotNull());
    }

    @Test
    public void requiredNumericColumnKeepsAZeroDefault() {
        DDLField number = new DDLField();
        number.setNotNull(true);
        number.setJdbcType(Types.NUMERIC);
        SchemaReady.prepareAdd(number);
        assertEquals(Integer.valueOf(0), number.getDefaultValue());
        assertTrue(number.isNotNull());
    }

    @Test
    public void repairRefusesStatementsThatEraseRows() {
        assertTrue(SchemaReady.removes("ALTER TABLE AO_X DROP COLUMN NAME"));
        assertTrue(SchemaReady.removes("drop table AO_X"));
        assertTrue(SchemaReady.removes("TRUNCATE AO_X"));
        assertTrue(SchemaReady.removes("DELETE FROM AO_X"));
        assertFalse(SchemaReady.removes("ALTER TABLE AO_X ADD COLUMN EXTERNAL_KEY VARCHAR(120)"));
        assertFalse(SchemaReady.removes("CREATE TABLE AO_X (ID INTEGER)"));
        assertFalse(SchemaReady.removes(null));
    }

    @Test
    public void sharedLinkColumnsBecomeOptionalWithoutDeletingRows() {
        java.util.List<String> statements = SchemaReady.relaxColumn("AO_X_AST_LINK", "LINK_TYPE", true);
        assertFalse(statements.isEmpty());
        for (int i = 0; i < statements.size(); i++) {
            assertFalse(SchemaReady.removes(statements.get(i)));
            assertTrue(statements.get(i).contains("LINK_TYPE"));
        }
        assertTrue(SchemaReady.relaxColumn("AO_X; DROP TABLE AO_X", "LINK_TYPE", true).isEmpty());
        assertTrue(SchemaReady.identifier("AO_1234_AST_LINK"));
        assertFalse(SchemaReady.identifier("AST LINK"));
    }

    @Test
    public void requestLinkTypeStaysOptionalSoH2CanStartThePlugin() throws Exception {
        assertNull(AssetIssueLinkEntity.class.getMethod("getLinkType").getAnnotation(NotNull.class));
    }

    @Test
    public void linkTypeBecomesOptionalOnTheSharedTable() {
        java.util.List<String> statements = com.assetstree.jira.service.LinkTableRepair.statements("PUBLIC", "AO_F08E8A_AST_LINK");
        assertFalse(statements.isEmpty());
        boolean nullable = false;
        for (int i = 0; i < statements.size(); i++) {
            assertFalse(SchemaReady.removes(statements.get(i)));
            if (statements.get(i).contains("SET NULL")) {
                nullable = true;
            }
        }
        assertTrue(nullable);
        assertTrue(com.assetstree.jira.service.LinkTableRepair.statements("PUBLIC.X", "AO_X").isEmpty());
    }
}
