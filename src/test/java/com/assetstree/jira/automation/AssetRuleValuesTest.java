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
    public void mapReadsPortalFieldValues() {
        Map<String, String> values = AssetRuleValues.map("{\"customfield_1\":\" Room 4 \",\"summary\":\"skip\",\"blank\":\"\"}");
        assertEquals("Room 4", values.get("customfield_1"));
        assertFalse(values.containsKey("summary"));
        assertTrue(AssetRuleValues.map("not-json").isEmpty());
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
        assertTrue(AssetRuleValues.matches(AssetRuleValues.CALENDAR, AssetRuleValues.CALENDAR));
        assertFalse(AssetRuleValues.matches("", AssetRuleValues.CALENDAR));
        assertTrue(AssetRuleValues.matches(AssetRuleValues.WRITEOFF, AssetRuleValues.WRITEOFF));
        assertFalse(AssetRuleValues.matches("", AssetRuleValues.WRITEOFF));
        assertTrue(AssetRuleValues.matches(AssetRuleValues.SCHEDULE, AssetRuleValues.SCHEDULE));
        assertFalse(AssetRuleValues.matches("", AssetRuleValues.SCHEDULE));
        assertTrue(AssetRuleValues.watchesCurrent(AssetRuleValues.WRITEOFF));
        assertTrue(AssetRuleValues.watchesCurrent(AssetRuleValues.SCHEDULE));
        assertFalse(AssetRuleValues.watchesCurrent(AssetRuleValues.STATUS));
        assertTrue(AssetRuleValues.issueStatusMatches("", "10001", "Repair"));
        assertTrue(AssetRuleValues.issueStatusMatches("10001", "10001", "Repair"));
        assertTrue(AssetRuleValues.issueStatusMatches("Repair", "10001", "Repair"));
        assertFalse(AssetRuleValues.issueStatusMatches("10002", "10001", "Repair"));
        assertTrue(AssetRuleValues.detailMatches("", "approved"));
        assertTrue(AssetRuleValues.detailMatches("approved", "approved"));
        assertFalse(AssetRuleValues.detailMatches("pending", "approved"));
        assertTrue(AssetRuleValues.currentStatus("", "in_use"));
        assertTrue(AssetRuleValues.currentStatus("in_use", "in_use"));
        assertFalse(AssetRuleValues.currentStatus("repair", "in_use"));
        assertTrue(AssetRuleValues.kindMatches("", "to"));
        assertTrue(AssetRuleValues.kindMatches("to", "to"));
        assertFalse(AssetRuleValues.kindMatches("kts", "to"));
    }

    @Test
    public void jsonReadsTheEventBody() {
        String body = "{\"event\":\"asset.created\",\"id\":12,\"key\":\"ASIT-6\",\"name\":\"Laptop\"}";
        assertEquals("asset.created", AssetRuleValues.json(body, "event"));
        assertEquals("ASIT-6", AssetRuleValues.json(body, "key"));
        assertEquals(12, AssetRuleValues.jsonInt(body, "id"));
    }
}
