package com.assetstree.jira.service;

import net.java.ao.schema.ddl.DDLActionType;
import net.java.ao.schema.ddl.DDLField;

import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
}
