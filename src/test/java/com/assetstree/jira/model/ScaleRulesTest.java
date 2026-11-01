package com.assetstree.jira.model;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ScaleRulesTest {
    @Test
    public void reportsDoNotOpenTheTree() {
        String caps = GrantCaps.normalize("reports");
        assertEquals("reports", caps);
        assertFalse(GrantCaps.has(caps, GrantCaps.VIEW));
        assertTrue(GrantCaps.has(caps, GrantCaps.REPORTS));
        assertEquals(GrantCaps.REPORTS, GrantCaps.levelOf(caps));
        assertTrue(GrantCaps.has("view", GrantCaps.REPORTS));
        assertTrue(GrantCaps.has("view", GrantCaps.MAP));
        assertTrue(GrantCaps.normalize("object").contains("view"));
    }

    @Test
    public void replacementDateFollowsTheUsefulLife() {
        LocalDate bought = LocalDate.of(2024, 1, 15);
        assertEquals(LocalDate.of(2027, 1, 15), Depreciation.replaceOn(bought, 36));
        assertEquals(24, Depreciation.monthsLeft(36, 12));
        assertEquals(0, Depreciation.monthsLeft(36, 40));
        assertNull(Depreciation.replaceOn(bought, 0));
    }

    @Test
    public void schemaAndFieldScopeStayNarrow() {
        Map<String, Set<String>> schemas = new LinkedHashMap<String, Set<String>>();
        schemas.put("Laptops", new LinkedHashSet<String>(Collections.singleton("laptop")));
        AccessScope.Slice named = new AccessScope.Slice("other", "Laptops", "serial:read,owner:write");
        AccessScope namedScope = AccessScope.merge(Collections.singletonList(named), schemas);
        assertTrue(namedScope.allowsType("laptop", false));
        assertFalse(namedScope.allowsType("printer", false));
        assertTrue(namedScope.allowsType("room", true));
        assertEquals(AccessScope.READ, namedScope.fieldMode("serial"));
        assertEquals(AccessScope.WRITE, namedScope.fieldMode("owner"));
        assertEquals(AccessScope.HIDE, namedScope.fieldMode("note"));
        AccessScope.Slice wider = new AccessScope.Slice("", "", "serial:write");
        AccessScope scope = AccessScope.merge(Arrays.asList(named, wider), schemas);
        assertEquals(AccessScope.WRITE, scope.fieldMode("serial"));
        assertTrue(scope.allowsType("printer", false));
        AccessScope missing = AccessScope.merge(
                Collections.singletonList(new AccessScope.Slice("", "Gone", "")), schemas);
        assertFalse(missing.allowsType("laptop", false));
        assertTrue(missing.isAllFields());
    }

    @Test
    public void blankJsonFieldKeepsTheStoredValue() {
        List<EquipmentExchange.FieldRef> fields = Collections.singletonList(
                new EquipmentExchange.FieldRef("serial", "Serial", "text", false));
        List<EquipmentExchange.TypeRef> types = Collections.singletonList(
                new EquipmentExchange.TypeRef("laptop", "Laptop", false, fields));
        List<EquipmentExchange.Node> nodes = Collections.singletonList(
                new EquipmentExchange.Node(7, null, "Room", "STP-1", true));
        JsonImport.Existing existing = new JsonImport.Existing(
                4, "ASIT-1", "ext-1", "ThinkPad", "old", "laptop", "in_use", 7, "ivan", "");
        JsonImport.Item item = new JsonImport.Item(
                "ASIT-1", "", "", "laptop", "", "Room", "", "", null, false,
                Collections.<String, String>singletonMap("serial", ""));
        JsonImport.Plan plan = JsonImport.plan(Collections.singletonList(item), Collections.singletonList(existing),
                nodes, types, Collections.singletonList(new EquipmentExchange.Named("in_use", "In use")),
                "in_use", true, people());
        assertTrue(plan.getIssues().isEmpty());
        assertEquals(1, plan.getChanges().size());
        assertEquals("ThinkPad", plan.getChanges().get(0).getChange().getName());
        assertTrue(plan.getChanges().get(0).getChange().getAttributes().isEmpty());
    }

    @Test
    public void unknownObjectKeyIsNotCreated() {
        JsonImport.Item item = new JsonImport.Item(
                "MISSING", "", "New", "laptop", "", "Room", "", "", null, false, Collections.<String, String>emptyMap());
        JsonImport.Plan plan = JsonImport.plan(Collections.singletonList(item), Collections.<JsonImport.Existing>emptyList(),
                Collections.<EquipmentExchange.Node>emptyList(), Collections.<EquipmentExchange.TypeRef>emptyList(),
                Collections.<EquipmentExchange.Named>emptyList(), "in_use", true, people());
        assertEquals(1, plan.getIssues().size());
        assertEquals("asset-tree.error.import.key", plan.getIssues().get(0).getMessageKey());
        assertTrue(plan.getChanges().isEmpty());
    }

    @Test
    public void workbookWindowKeepsTheNextOffset() {
        List<EquipmentSheet.Record> records = Arrays.asList(
                new EquipmentSheet.Record(2, Collections.singletonList("a")),
                new EquipmentSheet.Record(3, Collections.singletonList("b")),
                new EquipmentSheet.Record(4, Collections.singletonList("c")));
        EquipmentSheet.Sheet sheet = new EquipmentSheet.Sheet("Laptop", Collections.singletonList("Name"), records);
        EquipmentExchange.Window first = EquipmentExchange.window(Collections.singletonList(sheet), 0, 2);
        assertEquals(3, first.getTotal());
        assertEquals(2, first.getNext());
        assertFalse(first.isDone());
        assertEquals(1, first.getSheets().size());
        assertEquals(2, first.getSheets().get(0).getRecords().size());
        EquipmentExchange.Window last = EquipmentExchange.window(Collections.singletonList(sheet), 2, 2);
        assertTrue(last.isDone());
        assertEquals(3, last.getNext());
    }

    private static EquipmentExchange.Directory people() {
        return new EquipmentExchange.Directory() {
            @Override
            public EquipmentExchange.Person find(String raw) {
                return EquipmentExchange.Person.blank();
            }
        };
    }
}
