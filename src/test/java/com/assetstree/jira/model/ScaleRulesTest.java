package com.assetstree.jira.model;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
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
        assertTrue(GrantCaps.has("view", GrantCaps.SCHEME));
        assertFalse(GrantCaps.has("view", GrantCaps.SCHEME_EDIT));
        assertFalse(GrantCaps.has("object", GrantCaps.SCHEME_EDIT));
        assertTrue(GrantCaps.has("assets", GrantCaps.SCHEME_EDIT));
        assertTrue(GrantCaps.has("scheme", GrantCaps.SCHEME));
        assertFalse(GrantCaps.has("scheme", GrantCaps.SCHEME_EDIT));
        assertTrue(GrantCaps.has("schemeEdit", GrantCaps.SCHEME));
        assertTrue(GrantCaps.normalize("schemeEdit").contains("scheme"));
        assertFalse(GrantCaps.has("scheme", GrantCaps.VIEW));
        assertTrue(GrantCaps.normalize("object").contains("view"));
    }

    @Test
    public void projectAdminStaysInsideOneProject() {
        String caps = GrantCaps.normalize("projectAdmin");
        assertTrue(GrantCaps.has(caps, GrantCaps.PROJECT_ADMIN));
        assertTrue(GrantCaps.has(caps, GrantCaps.ASSETS));
        assertTrue(GrantCaps.has(caps, GrantCaps.SCHEME_EDIT));
        assertTrue(GrantCaps.has(caps, GrantCaps.TYPES));
        assertFalse(GrantCaps.has(caps, GrantCaps.ADMIN));
        assertEquals(GrantCaps.PROJECT_ADMIN, GrantCaps.levelOf(caps));
        assertEquals(GrantCaps.ADMIN, GrantCaps.levelOf("admin"));
    }

    @Test
    public void fineActionsStayInsideTheOldBundle() {
        assertEquals("reports", GrantCaps.normalize("reports"));
        assertTrue(GrantCaps.normalize("object").contains("view"));
        assertFalse(GrantCaps.normalize("object").contains("objEdit"));
        assertTrue(GrantCaps.allows("object", GrantCaps.OBJ_EDIT));
        assertTrue(GrantCaps.allows("object", GrantCaps.OBJ_REMOVE));
        assertTrue(GrantCaps.allows("objEdit", GrantCaps.OBJ_EDIT));
        assertFalse(GrantCaps.allows("objEdit", GrantCaps.OBJ_REMOVE));
        assertFalse(GrantCaps.allows("objEdit", GrantCaps.OBJECT));
        assertTrue(GrantCaps.allows("view", GrantCaps.PLAN));
        assertFalse(GrantCaps.has("reports", GrantCaps.VIEW));
        assertTrue(GrantCaps.normalize("objEdit").contains("view"));
        assertEquals("edit", GrantCaps.levelOf("objEdit"));
    }

    @Test
    public void eachActionKeepsItsOwnReach() {
        assertEquals("own", GrantCaps.actionReach("objEdit:own,objRemove:all", GrantCaps.OBJ_EDIT, "all"));
        assertEquals("all", GrantCaps.actionReach("objEdit:own,objRemove:all", GrantCaps.OBJ_REMOVE, "own"));
        assertEquals("", GrantCaps.actionReach("objEdit:own", GrantCaps.OBJ_CREATE, "own"));
        assertEquals("own", GrantCaps.actionReach("object", GrantCaps.OBJ_EDIT, "own"));
        assertEquals("all", GrantCaps.actionReach("object", GrantCaps.OBJ_EDIT, "all"));
        assertTrue(GrantCaps.normalize("objEdit:own").contains("objEdit:own"));
        assertTrue(GrantCaps.allows("objEdit:own", GrantCaps.OBJ_EDIT));
        assertFalse(GrantCaps.allows("objEdit:own", GrantCaps.OBJ_REMOVE));
        assertEquals("view", GrantCaps.coarse("objEdit:own"));
    }

    @Test
    public void ownReachCoversThePlaceAndTheCustodian() {
        Map<Integer, Integer> parents = new LinkedHashMap<Integer, Integer>();
        parents.put(Integer.valueOf(1), null);
        parents.put(Integer.valueOf(2), Integer.valueOf(1));
        parents.put(Integer.valueOf(3), Integer.valueOf(2));
        Map<Integer, String> custodians = new LinkedHashMap<Integer, String>();
        custodians.put(Integer.valueOf(1), "place-mol");
        custodians.put(Integer.valueOf(2), "");
        custodians.put(Integer.valueOf(3), "asset-mol");
        Set<Integer> held = new LinkedHashSet<Integer>();
        held.add(Integer.valueOf(1));
        assertTrue(OwnReach.covers("place-mol", 2, parents, custodians, held));
        assertTrue(OwnReach.covers("asset-mol", 3, parents, custodians, Collections.<Integer>emptySet()));
        assertFalse(OwnReach.covers("other", 3, parents, custodians, Collections.<Integer>emptySet()));
        assertTrue(OwnReach.covers("other", 2, parents, custodians, Collections.singleton(Integer.valueOf(1))));
    }

    @Test
    public void lifeBookFillsOnlyTheCellsThatArePresent() {
        assertTrue(LifeBook.template().length > 100);
        List<String> headers = Arrays.asList("Key", "Contractor", "Contract", "Contract from", "Price");
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(Arrays.asList("KRUL-1", "North", "R-14", "01.02.2026", "1500"));
        rows.add(Arrays.asList("", "Skip", "", "", ""));
        byte[] filled = WorkbookSheet.write(Collections.singletonList(
                new WorkbookSheet.Page("Lifecycle", headers, rows)));
        List<LifeBook.Change> changes = LifeBook.read(WorkbookSheet.readAll(filled));
        assertEquals(1, changes.size());
        assertEquals("KRUL-1", changes.get(0).getKey());
        LifeBook.Stored stored = new LifeBook.Stored();
        stored.priceMinor = 100L;
        stored.supplier = "Kept";
        assertNull(LifeBook.apply(stored, changes.get(0)));
        assertEquals("North", stored.contractor);
        assertEquals("R-14", stored.contractNo);
        assertEquals("2026-02-01", stored.contractFrom);
        assertEquals(150000L, stored.priceMinor);
        assertEquals("Kept", stored.supplier);
        LifeBook.Stored rich = new LifeBook.Stored();
        rich.priceMinor = 100L;
        LifeBook.Change over = changes.get(0);
        assertNull(LifeBook.apply(rich, over));
        rich.residualMinor = rich.priceMinor + 1L;
        LifeBook.Change residual = new LifeBook.Change(2, "KRUL-1", Collections.singletonMap("residual", "999999"));
        assertEquals("asset-tree.error.life", LifeBook.apply(rich, residual));
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
