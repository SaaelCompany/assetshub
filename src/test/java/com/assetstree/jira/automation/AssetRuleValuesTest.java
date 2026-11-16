package com.assetstree.jira.automation;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AssetRuleValuesTest {
    @Test
    public void emptyFilterMatchesEachEquipmentEvent() {
        assertTrue(AssetRuleValues.matches("", AssetRuleValues.CREATED));
        assertTrue(AssetRuleValues.matches(null, AssetRuleValues.UPDATED));
        assertTrue(AssetRuleValues.matches("  ", AssetRuleValues.DELETED));
        assertTrue(AssetRuleValues.matches("", AssetRuleValues.RESTORED));
    }

    @Test
    public void namedFilterMatchesOnlyThatEvent() {
        assertTrue(AssetRuleValues.matches("asset.updated", AssetRuleValues.UPDATED));
        assertFalse(AssetRuleValues.matches("asset.updated", AssetRuleValues.CREATED));
        assertFalse(AssetRuleValues.matches("asset.updated", "import.completed"));
    }

    @Test
    public void fieldReadsTheStoredMap() {
        Map<String, String> value = new HashMap<String, String>();
        value.put("status", " in_use ");
        assertEquals("in_use", AssetRuleValues.field(value, "status"));
        assertEquals("", AssetRuleValues.field(value, "event"));
    }

    @Test
    public void statusFilterMatchesOnlyAChangeIntoThatStatus() {
        assertTrue(AssetRuleValues.statusMatches("", "repair", "in_use"));
        assertTrue(AssetRuleValues.statusMatches("repair", "repair", "in_use"));
        assertTrue(AssetRuleValues.statusMatches("repair", "repair", ""));
        assertFalse(AssetRuleValues.statusMatches("repair", "repair", "repair"));
        assertFalse(AssetRuleValues.statusMatches("repair", "in_use", ""));
        assertTrue(AssetRuleValues.matches("asset.status", AssetRuleValues.STATUS));
        assertFalse(AssetRuleValues.matches("", AssetRuleValues.STATUS));
    }

    @Test
    public void jsonReadsTheEventBody() {
        String body = "{\"event\":\"asset.created\",\"id\":12,\"key\":\"ASIT-6\",\"name\":\"Laptop\"}";
        assertEquals("asset.created", AssetRuleValues.json(body, "event"));
        assertEquals("ASIT-6", AssetRuleValues.json(body, "key"));
        assertEquals(12, AssetRuleValues.jsonInt(body, "id"));
    }
}
