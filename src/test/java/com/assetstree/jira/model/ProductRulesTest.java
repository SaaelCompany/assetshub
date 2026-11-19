package com.assetstree.jira.model;

import com.assetstree.jira.dto.AssetTypeDto;
import com.assetstree.jira.dto.FieldDto;
import com.assetstree.jira.dto.FlowGraphDto;
import com.assetstree.jira.dto.FlowLinkDto;
import com.assetstree.jira.dto.FlowNodeDto;
import com.assetstree.jira.dto.ModuleSetupDto;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProductRulesTest {
    @Test
    public void treeRejectsACycleAndCountsEquipmentUnderAPlace() {
        assertNull(TreeLogic.normalizeParent(Integer.valueOf(0)));
        assertTrue(TreeLogic.sameParent(Integer.valueOf(0), null));
        Map<Integer, Integer> parents = new HashMap<Integer, Integer>();
        parents.put(Integer.valueOf(5), Integer.valueOf(6));
        parents.put(Integer.valueOf(6), Integer.valueOf(5));
        assertTrue(TreeLogic.wouldCycle(parents, 5, Integer.valueOf(6)));
        assertFalse(TreeLogic.wouldCycle(parents, 5, null));

        Map<Integer, List<Integer>> children = new HashMap<Integer, List<Integer>>();
        children.put(Integer.valueOf(1), Arrays.asList(Integer.valueOf(2), Integer.valueOf(3)));
        children.put(Integer.valueOf(3), Collections.singletonList(Integer.valueOf(4)));
        Set<Integer> places = new HashSet<Integer>();
        places.add(Integer.valueOf(1));
        places.add(Integer.valueOf(3));
        Map<Integer, Integer> counts = TreeLogic.equipmentCounts(children, places);
        assertEquals(Integer.valueOf(2), counts.get(Integer.valueOf(1)));
        assertEquals(Integer.valueOf(1), counts.get(Integer.valueOf(3)));
        assertEquals(Integer.valueOf(1), counts.get(Integer.valueOf(4)));
        assertEquals("laptop", TreeLogic.slug("Laptop"));
    }

    @Test
    public void reportsOnlyGrantDoesNotOpenTheTree() {
        assertFalse(GrantCaps.has(GrantCaps.normalize("reports"), GrantCaps.VIEW));
        assertTrue(GrantCaps.has(GrantCaps.normalize("view"), GrantCaps.MAP));
        assertTrue(AccessScope.all().allowsType("printer", false));
        AccessScope narrow = AccessScope.merge(
                Collections.singletonList(new AccessScope.Slice("laptop", "", "serial:read")),
                Collections.<String, Set<String>>emptyMap());
        assertTrue(narrow.allowsType("laptop", false));
        assertFalse(narrow.allowsType("printer", false));
        assertEquals(AccessScope.READ, narrow.fieldMode("serial"));
        assertEquals(AccessScope.HIDE, narrow.fieldMode("note"));
    }

    @Test
    public void oneSetupKeyKeepsTheRestOfTheProjectRow() {
        ModuleSetupDto stored = new ModuleSetupDto();
        stored.setScheduleKinds("to|ТО,verify|Поверка");
        stored.setMapWidth(Integer.valueOf(2400));
        stored.setMapHeight(Integer.valueOf(1800));
        stored.setFurniture("desk^\u0421\u0442\u043e\u043b^table,10,10,80,40");
        stored.setStaffMarks("night|Ночь|4338ca|8,leave|Отпуск|d97706|0,trip|Командировка|0f766e|8");
        ModuleSetupDto read = ModuleSetup.read(ModuleSetup.write(stored));
        ModuleSetupDto patch = new ModuleSetupDto();
        patch.setMapWidth(Integer.valueOf(800));
        ModuleSetupDto merged = ModuleSetup.read(ModuleSetup.write(ModuleSetup.merge(read, patch)));
        assertEquals(Integer.valueOf(800), merged.getMapWidth());
        assertTrue(merged.getScheduleKinds().contains("to|"));
        assertTrue(merged.getScheduleKinds().contains("verify|"));
        assertTrue(merged.getFurniture().contains("desk"));
        assertTrue(merged.getStaffMarks().contains("night|Ночь|4338ca|8|2"));
        assertTrue(merged.getStaffMarks().contains("leave|Отпуск|d97706|0|0"));
        assertTrue(merged.getStaffMarks().contains("trip|Командировка|0f766e|8|1"));
        assertEquals(Integer.valueOf(1800), merged.getMapHeight());
    }

    @Test
    public void lifecycleStartsOpenUntilTheProjectFoldsIt() {
        ModuleSetupDto defaults = ModuleSetup.read("");
        assertEquals(Boolean.FALSE, defaults.getLifeFold());
        ModuleSetupDto folded = new ModuleSetupDto();
        folded.setLifeFold(Boolean.TRUE);
        ModuleSetupDto read = ModuleSetup.read(ModuleSetup.write(folded));
        assertEquals(Boolean.TRUE, read.getLifeFold());
        ModuleSetupDto patch = new ModuleSetupDto();
        patch.setLifeCurrency("USD");
        ModuleSetupDto merged = ModuleSetup.read(ModuleSetup.write(ModuleSetup.merge(read, patch)));
        assertEquals(Boolean.TRUE, merged.getLifeFold());
        assertEquals("USD", merged.getLifeCurrency());
    }

    @Test
    public void cardPhotoStartsOffAndSurvivesAnotherSetting() {
        ModuleSetupDto defaults = ModuleSetup.read("");
        assertEquals(Boolean.FALSE, defaults.getPhoto());
        ModuleSetupDto shown = new ModuleSetupDto();
        shown.setPhoto(Boolean.TRUE);
        ModuleSetupDto read = ModuleSetup.read(ModuleSetup.write(shown));
        assertEquals(Boolean.TRUE, read.getPhoto());
        ModuleSetupDto patch = new ModuleSetupDto();
        patch.setLifeCurrency("USD");
        ModuleSetupDto merged = ModuleSetup.read(ModuleSetup.write(ModuleSetup.merge(read, patch)));
        assertEquals(Boolean.TRUE, merged.getPhoto());
        assertEquals("USD", merged.getLifeCurrency());
        ModuleSetupDto off = new ModuleSetupDto();
        off.setPhoto(Boolean.FALSE);
        ModuleSetupDto hidden = ModuleSetup.read(ModuleSetup.write(ModuleSetup.merge(merged, off)));
        assertEquals(Boolean.FALSE, hidden.getPhoto());
        assertEquals("USD", hidden.getLifeCurrency());
    }

    @Test
    public void issueNotesStayOnUntilTheProjectTurnsThemOff() {
        assertEquals(Boolean.TRUE, ModuleSetup.read("").getIssueNotes());
        ModuleSetupDto off = new ModuleSetupDto();
        off.setIssueNotes(Boolean.FALSE);
        ModuleSetupDto read = ModuleSetup.read(ModuleSetup.write(off));
        assertEquals(Boolean.FALSE, read.getIssueNotes());
        ModuleSetupDto patch = new ModuleSetupDto();
        patch.setPhoto(Boolean.TRUE);
        ModuleSetupDto merged = ModuleSetup.read(ModuleSetup.write(ModuleSetup.merge(read, patch)));
        assertEquals(Boolean.FALSE, merged.getIssueNotes());
        assertEquals(Boolean.TRUE, merged.getPhoto());
        ModuleSetupDto on = new ModuleSetupDto();
        on.setIssueNotes(Boolean.TRUE);
        ModuleSetupDto restored = ModuleSetup.read(ModuleSetup.write(ModuleSetup.merge(merged, on)));
        assertEquals(Boolean.TRUE, restored.getIssueNotes());
    }

    @Test
    public void markFactorsDoubleNightAndSkipLeave() {
        assertEquals(2, TimesheetMath.factor("night", 8, null));
        assertEquals(0, TimesheetMath.factor("leave", 0, null));
        assertEquals(1, TimesheetMath.factor("trip", 8, null));
        assertEquals(16, TimesheetMath.hours(8, 2));
        assertEquals(0, TimesheetMath.hours(8, 0));
    }

    @Test
    public void importWindowAndUnknownKeyStayPredictable() {
        List<EquipmentSheet.Record> records = Arrays.asList(
                new EquipmentSheet.Record(2, Collections.singletonList("a")),
                new EquipmentSheet.Record(3, Collections.singletonList("b")));
        EquipmentSheet.Sheet sheet = new EquipmentSheet.Sheet("Laptop", Collections.singletonList("Name"), records);
        EquipmentExchange.Window window = EquipmentExchange.window(Collections.singletonList(sheet), 0, 1);
        assertEquals(2, window.getTotal());
        assertEquals(1, window.getNext());
        assertFalse(window.isDone());
        JsonImport.Item item = new JsonImport.Item(
                "MISSING", "", "New", "laptop", "", "", "", "", null, false, Collections.<String, String>emptyMap());
        JsonImport.Plan plan = JsonImport.plan(Collections.singletonList(item), Collections.<JsonImport.Existing>emptyList(),
                Collections.<EquipmentExchange.Node>emptyList(), Collections.<EquipmentExchange.TypeRef>emptyList(),
                Collections.<EquipmentExchange.Named>emptyList(), "in_use", true, blankPeople());
        assertEquals("asset-tree.error.import.key", plan.getIssues().get(0).getMessageKey());
    }

    @Test
    public void statusProcessFollowsADirectLineAndARequestStatus() {
        FlowGraphDto direct = graph(
                node("req1", FlowGraph.REQUEST, "100", "", ""),
                node("sta1", FlowGraph.STATUS, "", "", "repair"),
                link("req1", "sta1"));
        assertEquals("repair", FlowGraph.equipmentStatus(direct, "100", "5", false));
        assertNull(FlowGraph.equipmentStatus(direct, "100", "5", true));

        FlowGraphDto chained = graph(
                node("req1", FlowGraph.REQUEST, "100", "", ""),
                node("iss1", FlowGraph.ISSUE, "", "5", ""),
                node("sta1", FlowGraph.STATUS, "", "", "repair"),
                link("req1", "iss1"),
                link("iss1", "sta1"));
        assertEquals("repair", FlowGraph.equipmentStatus(chained, "100", "5", true));
        assertNull(FlowGraph.equipmentStatus(chained, "100", "5", false));
        assertNull(FlowGraph.equipmentStatus(new FlowGraphDto(), "100", "5", false));
    }

    @Test
    public void typedLinkKeepsAttributesAndFinishedDaysStayOnTheCalendar() {
        assertEquals("part", LinkRules.typeKey("Part"));
        assertEquals("", LinkRules.typeKey("входит"));
        List<LinkRules.Attr> patch = Collections.singletonList(new LinkRules.Attr("slot", ""));
        String kept = LinkRules.merge("slot=A1\nport=3", patch, true);
        assertTrue(kept.contains("slot=A1"));
        assertTrue(kept.contains("port=3"));
        String cleared = LinkRules.merge(kept, patch, false);
        assertFalse(cleared.contains("slot="));
        assertTrue(cleared.contains("port=3"));
        String moves = ServiceDue.rememberDone("2026-11-01>2026-11-02", "2026-11-01");
        assertTrue(moves.contains("2026-11-01>done"));
        assertTrue(moves.contains("2026-11-01>2026-11-02"));
        String replaced = ServiceDue.keepDone("2026-11-08>2026-11-09", moves);
        assertTrue(replaced.contains("2026-11-01>done"));
        assertTrue(replaced.contains("2026-11-08>2026-11-09"));
    }

    @Test
    public void serviceColumnsFollowTheModuleAndStayOnTheObjectRow() {
        List<EquipmentExchange.TypeRef> types = Collections.singletonList(
                new EquipmentExchange.TypeRef("laptop", "Laptop", false, Collections.<EquipmentExchange.FieldRef>emptyList()));
        List<EquipmentExchange.Named> statuses = Collections.singletonList(new EquipmentExchange.Named("in_use", "In use"));
        List<EquipmentExchange.Node> nodes = Arrays.asList(
                new EquipmentExchange.Node(1, null, "Room", "STP-1", true),
                new EquipmentExchange.Node(2, Integer.valueOf(1), "ThinkPad", "STP-2", false));
        List<EquipmentExchange.Named> kinds = Arrays.asList(
                new EquipmentExchange.Named("to", "Maintenance"),
                new EquipmentExchange.Named("verify", "Verification"));
        List<String> headers = Arrays.asList("Key", "Name", "Type", "Place",
                "Service: kind", "Service: work", "Service: date", "Service: interval", "Service: period", "Service: assignee");
        EquipmentSheet.Sheet created = new EquipmentSheet.Sheet("Laptop", headers, Collections.singletonList(
                new EquipmentSheet.Record(2, Arrays.asList(
                        "", "New laptop", "Laptop", "Room", "ТО", "Замена фильтра", "01.11.2026", "6", "мес.", "ivan"))));
        EquipmentSheet.Sheet quiet = new EquipmentSheet.Sheet("Laptop", headers, Collections.singletonList(
                new EquipmentSheet.Record(2, Arrays.asList(
                        "STP-2", "ThinkPad", "Laptop", "Room", "", "", "", "", "", ""))));
        EquipmentSheet.Sheet extra = new EquipmentSheet.Sheet("Service", Arrays.asList(
                "Key", "Service: kind", "Service: work", "Service: date", "Service: interval", "Service: period", "Service: assignee"),
                Collections.singletonList(new EquipmentSheet.Record(2, Arrays.asList(
                        "STP-2", "Verification", "", "2026-03-01", "", "once", ""))));
        EquipmentExchange.Plan on = EquipmentExchange.planBook(Arrays.asList(created, extra), types, statuses, nodes,
                "in_use", new HashMap<String, String>(), servicePeople(), true, kinds);
        assertTrue(on.getIssues().toString(), on.getIssues().isEmpty());
        assertEquals(1, on.getChanges().size());
        EquipmentExchange.Service wish = on.getChanges().get(0).getService();
        assertNotNull(wish);
        assertEquals("", wish.getObjectKey());
        assertEquals("to", wish.getKind());
        assertEquals("Замена фильтра", wish.getName());
        assertEquals("2026-11-01", wish.getLastDone());
        assertEquals(6, wish.getEveryCount());
        assertEquals("month", wish.getEveryUnit());
        assertEquals("ivan", wish.getAssigneeKey());
        assertEquals(1, on.getServices().size());
        assertEquals("STP-2", on.getServices().get(0).getObjectKey());
        assertEquals("verify", on.getServices().get(0).getKind());
        assertEquals(1, on.getServices().get(0).getEveryCount());
        assertEquals("once", on.getServices().get(0).getEveryUnit());

        EquipmentExchange.Plan off = EquipmentExchange.planBook(Arrays.asList(created, extra), types, statuses, nodes,
                "in_use", new HashMap<String, String>(), servicePeople(), false, kinds);
        assertTrue(off.getIssues().toString(), off.getIssues().isEmpty());
        assertNull(off.getChanges().get(0).getService());
        assertTrue(off.getServices().isEmpty());

        EquipmentExchange.Plan blank = EquipmentExchange.planBook(Collections.singletonList(quiet), types, statuses, nodes,
                "in_use", new HashMap<String, String>(), servicePeople(), true, kinds);
        List<String> lifeHeaders = Arrays.asList("Key", "Name", "Type", "Place", "Lifecycle: contractor", "Lifecycle: contract number", "Lifecycle: cost");
        EquipmentSheet.Sheet withLife = new EquipmentSheet.Sheet("Laptop", lifeHeaders, Collections.singletonList(
                new EquipmentSheet.Record(2, Arrays.asList("STP-2", "ThinkPad", "Laptop", "Room", "North", "R-14", "1500"))));
        EquipmentSheet.Sheet lifeOnly = new EquipmentSheet.Sheet("Lifecycle", Arrays.asList(
                "Key", "Lifecycle: supplier"), Collections.singletonList(
                new EquipmentSheet.Record(2, Arrays.asList("STP-2", "Kept"))));
        EquipmentExchange.Plan lifeOn = EquipmentExchange.planBook(Arrays.asList(withLife, lifeOnly), types, statuses, nodes,
                "in_use", new HashMap<String, String>(), servicePeople(), false, kinds, true);
        assertTrue(lifeOn.getIssues().toString(), lifeOn.getIssues().isEmpty());
        assertEquals("North", lifeOn.getChanges().get(0).getLifecycle().get("contractor"));
        assertEquals("1500", lifeOn.getChanges().get(0).getLifecycle().get("price"));
        assertEquals(1, lifeOn.getLifecycles().size());
        assertEquals("Kept", lifeOn.getLifecycles().get(0).get("supplier"));
        EquipmentExchange.Plan lifeOff = EquipmentExchange.planBook(Arrays.asList(withLife, lifeOnly), types, statuses, nodes,
                "in_use", new HashMap<String, String>(), servicePeople(), false, kinds, false);
        assertNull(lifeOff.getChanges().get(0).getLifecycle());
        assertTrue(lifeOff.getLifecycles().isEmpty());
        assertTrue(blank.getIssues().isEmpty());
        assertNull(blank.getChanges().get(0).getService());

        EquipmentSheet.Sheet broken = new EquipmentSheet.Sheet("Laptop", headers, Collections.singletonList(
                new EquipmentSheet.Record(2, Arrays.asList(
                        "", "New laptop", "Laptop", "Room", "Polish", "", "yesterday", "6", "hourly", ""))));
        EquipmentExchange.Plan bad = EquipmentExchange.planBook(Collections.singletonList(broken), types, statuses, nodes,
                "in_use", new HashMap<String, String>(), servicePeople(), true, kinds);
        assertTrue(bad.getChanges().isEmpty());
        assertEquals("asset-tree.error.import.service.kind", bad.getIssues().get(0).getMessageKey());
    }

    @Test
    public void importKeepsOneComputerWhenTheHostAlreadyExists() {
        List<EquipmentExchange.TypeRef> types = Arrays.asList(
                new EquipmentExchange.TypeRef("laptop", "Laptop", false, Collections.<EquipmentExchange.FieldRef>emptyList()),
                new EquipmentExchange.TypeRef("server", "Server", false, Collections.<EquipmentExchange.FieldRef>emptyList()),
                new EquipmentExchange.TypeRef("room", "Room", true, Collections.<EquipmentExchange.FieldRef>emptyList()));
        List<EquipmentExchange.Named> statuses = Collections.singletonList(new EquipmentExchange.Named("in_use", "In use"));
        List<EquipmentExchange.Node> nodes = Arrays.asList(
                new EquipmentExchange.Node(1, null, "Room", "STP-1", true, "room"),
                new EquipmentExchange.Node(4, Integer.valueOf(1), "KRUL-IT", "ATD-4", false, "server"));
        List<String> headers = Arrays.asList("Name", "Type", "Place");
        EquipmentSheet.Record row = new EquipmentSheet.Record(2, Arrays.asList("krul-it.budzdorov.ru", "Laptop", "Room"));
        EquipmentSheet.Sheet sheet = new EquipmentSheet.Sheet("Equipment", headers, Collections.singletonList(row));
        EquipmentExchange.Plan plan = EquipmentExchange.plan(sheet, types, statuses, nodes, "in_use",
                new HashMap<String, String>(), blankPeople());
        assertTrue(plan.getIssues().toString(), plan.getIssues().isEmpty());
        assertEquals(1, plan.getChanges().size());
        assertEquals(Integer.valueOf(4), plan.getChanges().get(0).getExistingId());
        assertEquals("server", plan.getChanges().get(0).getTypeKey());

        EquipmentSheet.Sheet twice = new EquipmentSheet.Sheet("Equipment", headers, Arrays.asList(
                row, new EquipmentSheet.Record(3, Arrays.asList("KRUL-IT", "Laptop", "Room"))));
        EquipmentExchange.Plan again = EquipmentExchange.plan(twice, types, statuses, nodes, "in_use",
                new HashMap<String, String>(), blankPeople());
        assertEquals(1, again.getChanges().size());
        assertEquals("asset-tree.error.import.host", again.getIssues().get(0).getMessageKey());

        JsonImport.Item item = new JsonImport.Item("", "", "krul-it.budzdorov.ru", "laptop", "", "Room",
                "", "", "", false, Collections.<String, String>emptyMap());
        JsonImport.Existing computer = new JsonImport.Existing(4, "ATD-4", "", "KRUL-IT", "", "server",
                "in_use", 1, "", "");
        JsonImport.Plan json = JsonImport.plan(Collections.singletonList(item), Collections.singletonList(computer),
                Collections.singletonList(new EquipmentExchange.Node(1, null, "Room", "STP-1", true, "room")),
                types, statuses, "in_use", true, blankPeople());
        assertTrue(json.getIssues().toString(), json.getIssues().isEmpty());
        assertEquals(Integer.valueOf(4), json.getChanges().get(0).getChange().getExistingId());
        assertEquals("server", json.getChanges().get(0).getChange().getTypeKey());
    }

    @Test
    public void importKeepsTwoUpsWhenTheSerialNumbersDiffer() {
        List<EquipmentExchange.FieldRef> fields = Collections.singletonList(
                new EquipmentExchange.FieldRef("serial", "Serial", "text", false));
        List<EquipmentExchange.TypeRef> types = Arrays.asList(
                new EquipmentExchange.TypeRef("ups", "UPS", false, fields),
                new EquipmentExchange.TypeRef("room", "Room", true, Collections.<EquipmentExchange.FieldRef>emptyList()));
        List<EquipmentExchange.Named> statuses = Collections.singletonList(new EquipmentExchange.Named("in_use", "In use"));
        List<EquipmentExchange.Node> places = Collections.singletonList(
                new EquipmentExchange.Node(1, null, "Room", "STP-1", true, "room"));
        List<String> headers = Arrays.asList("Name", "Type", "Place", "Serial");
        EquipmentSheet.Sheet sheet = new EquipmentSheet.Sheet("Equipment", headers, Arrays.asList(
                new EquipmentSheet.Record(2, Arrays.asList("\u0418\u0411\u041f APC", "UPS", "Room", "SN-1")),
                new EquipmentSheet.Record(3, Arrays.asList("\u0418\u0411\u041f APC", "UPS", "Room", "SN-2"))));
        EquipmentExchange.Plan created = EquipmentExchange.plan(sheet, types, statuses, places, "in_use",
                new HashMap<String, String>(), blankPeople());
        assertTrue(created.getIssues().toString(), created.getIssues().isEmpty());
        assertEquals(2, created.getChanges().size());
        assertNull(created.getChanges().get(0).getExistingId());
        assertNull(created.getChanges().get(1).getExistingId());

        EquipmentSheet.Sheet sameNumber = new EquipmentSheet.Sheet("Equipment", headers, Arrays.asList(
                new EquipmentSheet.Record(2, Arrays.asList("\u0418\u0411\u041f APC", "UPS", "Room", "SN-1")),
                new EquipmentSheet.Record(3, Arrays.asList("\u0418\u0411\u041f APC", "UPS", "Room", "SN-1"))));
        EquipmentExchange.Plan repeated = EquipmentExchange.plan(sameNumber, types, statuses, places, "in_use",
                new HashMap<String, String>(), blankPeople());
        assertEquals(1, repeated.getChanges().size());
        assertEquals("asset-tree.error.import.host", repeated.getIssues().get(0).getMessageKey());

        EquipmentExchange.Node known = new EquipmentExchange.Node(4, Integer.valueOf(1), "\u0418\u0411\u041f APC", "ATD-4",
                false, "ups", "SN-1", "");
        List<EquipmentExchange.Node> nodes = Arrays.asList(places.get(0), known);
        EquipmentSheet.Sheet again = new EquipmentSheet.Sheet("Equipment", headers, Arrays.asList(
                new EquipmentSheet.Record(2, Arrays.asList("\u0418\u0411\u041f APC", "UPS", "Room", "SN-1")),
                new EquipmentSheet.Record(3, Arrays.asList("\u0418\u0411\u041f APC", "UPS", "Room", "SN-9"))));
        EquipmentExchange.Plan updated = EquipmentExchange.plan(again, types, statuses, nodes, "in_use",
                new HashMap<String, String>(), blankPeople());
        assertTrue(updated.getIssues().toString(), updated.getIssues().isEmpty());
        assertEquals(Integer.valueOf(4), updated.getChanges().get(0).getExistingId());
        assertNull(updated.getChanges().get(1).getExistingId());

        Map<String, String> fieldsOne = new LinkedHashMap<String, String>();
        fieldsOne.put("serial", "SN-1");
        Map<String, String> fieldsTwo = new LinkedHashMap<String, String>();
        fieldsTwo.put("\u0441\u0435\u0440\u0438\u0439\u043d\u044b\u0439 \u043d\u043e\u043c\u0435\u0440", "SN-2");
        JsonImport.Item first = new JsonImport.Item("", "", "\u0418\u0411\u041f APC", "UPS", "", "Room",
                "", "", "", false, fieldsOne);
        JsonImport.Item second = new JsonImport.Item("", "", "\u0418\u0411\u041f APC", "UPS", "", "Room",
                "", "", "", false, fieldsTwo);
        JsonImport.Existing stored = new JsonImport.Existing(4, "ATD-4", "", "\u0418\u0411\u041f APC", "", "ups",
                "in_use", 1, "", "", "SN-1", "");
        JsonImport.Plan json = JsonImport.plan(Arrays.asList(first, second), Collections.singletonList(stored),
                places, types, statuses, "in_use", true, blankPeople());
        assertTrue(json.getIssues().toString(), json.getIssues().isEmpty());
        assertEquals(Integer.valueOf(4), json.getChanges().get(0).getChange().getExistingId());
        assertNull(json.getChanges().get(1).getChange().getExistingId());
    }

    @Test
    public void importMatchesEachUpsWhenTheSerialColumnIsNamedSn() {
        assertTrue(DuplicateMarks.isSerial("S/N:", "S/N:"));
        assertTrue(DuplicateMarks.isSerial("sn", "S/N:"));
        assertFalse(DuplicateMarks.isSerial("model", "\u041c\u043e\u0434\u0435\u043b\u044c:"));
        List<EquipmentExchange.FieldRef> fields = Collections.singletonList(
                new EquipmentExchange.FieldRef("sn", "S/N:", "text", false));
        List<EquipmentExchange.TypeRef> types = Arrays.asList(
                new EquipmentExchange.TypeRef("ups", "\u0418\u0411\u041f", false, fields),
                new EquipmentExchange.TypeRef("room", "Room", true, Collections.<EquipmentExchange.FieldRef>emptyList()));
        List<EquipmentExchange.Named> statuses = Collections.singletonList(new EquipmentExchange.Named("in_use", "\u0412 \u044d\u043a\u0441\u043f\u043b\u0443\u0430\u0442\u0430\u0446\u0438\u0438"));
        EquipmentExchange.Node room = new EquipmentExchange.Node(1, null, "Room", "STP-1", true, "room");
        List<String> headers = Arrays.asList("\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435", "\u0422\u0438\u043f", "\u041f\u043b\u043e\u0449\u0430\u0434\u043a\u0430", "S/N:");
        List<EquipmentSheet.Record> records = Arrays.asList(
                new EquipmentSheet.Record(2, Arrays.asList("BV500I-GR", "\u0418\u0411\u041f", "Room", "9B2138A10791")),
                new EquipmentSheet.Record(3, Arrays.asList("BV500I-GR", "\u0418\u0411\u041f", "Room", "9B2138A10792")),
                new EquipmentSheet.Record(4, Arrays.asList("Powercom WOW-500U", "\u0418\u0411\u041f", "Room", "20154101006")),
                new EquipmentSheet.Record(5, Arrays.asList("Powercom WOW-500U", "\u0418\u0411\u041f", "Room", "20161911008")));
        EquipmentSheet.Sheet sheet = new EquipmentSheet.Sheet("\u0418\u0411\u041f", headers, records);
        EquipmentExchange.Plan created = EquipmentExchange.plan(sheet, types, statuses,
                Collections.singletonList(room), "in_use", new HashMap<String, String>(), blankPeople());
        assertTrue(created.getIssues().toString(), created.getIssues().isEmpty());
        assertEquals(4, created.getChanges().size());
        for (EquipmentExchange.Change change : created.getChanges()) {
            assertNull(change.getExistingId());
        }
        List<EquipmentExchange.Node> stored = Arrays.asList(
                room,
                new EquipmentExchange.Node(14, Integer.valueOf(1), "BV500I-GR", "BZ-14", false, "ups", "9B2138A10791", ""),
                new EquipmentExchange.Node(28, Integer.valueOf(1), "BV500I-GR", "BZ-28", false, "ups", "9B2138A10792", ""),
                new EquipmentExchange.Node(2, Integer.valueOf(1), "Powercom WOW-500U", "BZ-2", false, "ups", "20154101006", ""),
                new EquipmentExchange.Node(3, Integer.valueOf(1), "Powercom WOW-500U", "BZ-3", false, "ups", "20161911008", ""));
        EquipmentExchange.Plan again = EquipmentExchange.plan(sheet, types, statuses, stored, "in_use",
                new HashMap<String, String>(), blankPeople());
        assertTrue(again.getIssues().toString(), again.getIssues().isEmpty());
        assertEquals(4, again.getChanges().size());
        assertEquals(Integer.valueOf(14), again.getChanges().get(0).getExistingId());
        assertEquals(Integer.valueOf(28), again.getChanges().get(1).getExistingId());
        assertEquals(Integer.valueOf(2), again.getChanges().get(2).getExistingId());
        assertEquals(Integer.valueOf(3), again.getChanges().get(3).getExistingId());
    }

    @Test
    public void importUpdatesEachNamedObjectWhenNoSerialIsFilled() {
        List<EquipmentExchange.FieldRef> fields = Collections.singletonList(
                new EquipmentExchange.FieldRef("sn", "S/N:", "text", false));
        List<EquipmentExchange.TypeRef> types = Arrays.asList(
                new EquipmentExchange.TypeRef("printer", "\u041f\u0440\u0438\u043d\u0442\u0435\u0440", false, fields),
                new EquipmentExchange.TypeRef("mfp", "\u041c\u0424\u0423", false, fields),
                new EquipmentExchange.TypeRef("computer", "\u041a\u043e\u043c\u043f\u044c\u044e\u0442\u0435\u0440", false, fields),
                new EquipmentExchange.TypeRef("room", "Room", true, Collections.<EquipmentExchange.FieldRef>emptyList()));
        List<EquipmentExchange.Named> statuses = Collections.singletonList(
                new EquipmentExchange.Named("in_use", "\u0412 \u044d\u043a\u0441\u043f\u043b\u0443\u0430\u0442\u0430\u0446\u0438\u0438"));
        EquipmentExchange.Node room = new EquipmentExchange.Node(1, null, "Room", "STP-1", true, "room");
        List<String> headers = Arrays.asList("\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435", "\u0422\u0438\u043f", "\u041f\u043b\u043e\u0449\u0430\u0434\u043a\u0430", "S/N:");
        List<EquipmentExchange.Node> stored = Arrays.asList(
                room,
                new EquipmentExchange.Node(4, Integer.valueOf(1), "HP LaserJet M111a", "BZ-4", false, "printer", "VNCV", ""),
                new EquipmentExchange.Node(5, Integer.valueOf(1), "\u0411\u0410\u041d\u041a", "BZ-5", false, "computer", "", ""),
                new EquipmentExchange.Node(6, Integer.valueOf(1), "HP Color LaserJet Pro MFP M479dw", "BZ-6", false, "mfp", "", ""),
                new EquipmentExchange.Node(7, Integer.valueOf(1), "HP Color LaserJet Pro MFP M479dw", "BZ-7", false, "mfp", "", ""),
                new EquipmentExchange.Node(8, Integer.valueOf(1), "HP Color LaserJet Pro MFP M479dw", "BZ-8", false, "mfp", "", ""),
                new EquipmentExchange.Node(9, Integer.valueOf(1), "HP Color LaserJet Pro MFP M479dw", "BZ-9", false, "mfp", "", ""));
        EquipmentSheet.Sheet sheet = new EquipmentSheet.Sheet("\u041e\u0431\u043e\u0440\u0443\u0434\u043e\u0432\u0430\u043d\u0438\u0435", headers, Arrays.asList(
                new EquipmentSheet.Record(2, Arrays.asList("HP LaserJet M111a", "\u041f\u0440\u0438\u043d\u0442\u0435\u0440", "Room", "")),
                new EquipmentSheet.Record(3, Arrays.asList("\u0411\u0410\u041d\u041a", "\u041a\u043e\u043c\u043f\u044c\u044e\u0442\u0435\u0440", "Room", "")),
                new EquipmentSheet.Record(4, Arrays.asList("HP Color LaserJet Pro MFP M479dw", "\u041c\u0424\u0423", "Room", "")),
                new EquipmentSheet.Record(5, Arrays.asList("HP Color LaserJet Pro MFP M479dw", "\u041c\u0424\u0423", "Room", "")),
                new EquipmentSheet.Record(6, Arrays.asList("HP Color LaserJet Pro MFP M479dw", "\u041c\u0424\u0423", "Room", "")),
                new EquipmentSheet.Record(7, Arrays.asList("HP Color LaserJet Pro MFP M479dw", "\u041c\u0424\u0423", "Room", "")),
                new EquipmentSheet.Record(8, Arrays.asList("HP Color LaserJet Pro MFP M479dw", "\u041c\u0424\u0423", "Room", ""))));
        EquipmentExchange.Plan plan = EquipmentExchange.plan(sheet, types, statuses, stored, "in_use",
                new HashMap<String, String>(), blankPeople());
        assertEquals(1, plan.getIssues().size());
        assertEquals("asset-tree.error.import.host", plan.getIssues().get(0).getMessageKey());
        assertEquals(6, plan.getChanges().size());
        assertNull(plan.getChanges().get(0).getExistingId());
        assertEquals(Integer.valueOf(5), plan.getChanges().get(1).getExistingId());
        Set<Integer> updated = new HashSet<Integer>();
        for (int i = 2; i < plan.getChanges().size(); i++) {
            updated.add(plan.getChanges().get(i).getExistingId());
        }
        assertEquals(new HashSet<Integer>(Arrays.asList(Integer.valueOf(6), Integer.valueOf(7), Integer.valueOf(8), Integer.valueOf(9))), updated);

        JsonImport.Item bank = new JsonImport.Item("", "", "\u0411\u0410\u041d\u041a", "\u041a\u043e\u043c\u043f\u044c\u044e\u0442\u0435\u0440", "", "Room",
                "", "", "", false, Collections.<String, String>emptyMap());
        JsonImport.Item again = new JsonImport.Item("", "", "\u0411\u0410\u041d\u041a", "\u041a\u043e\u043c\u043f\u044c\u044e\u0442\u0435\u0440", "", "Room",
                "", "", "", false, Collections.<String, String>emptyMap());
        JsonImport.Existing computer = new JsonImport.Existing(5, "BZ-5", "", "\u0411\u0410\u041d\u041a", "", "computer",
                "in_use", 1, "", "", "", "");
        JsonImport.Plan json = JsonImport.plan(Arrays.asList(bank, again), Collections.singletonList(computer),
                Collections.singletonList(room), types, statuses, "in_use", true, blankPeople());
        assertEquals(1, json.getChanges().size());
        assertEquals(Integer.valueOf(5), json.getChanges().get(0).getChange().getExistingId());
        assertEquals("asset-tree.error.import.host", json.getIssues().get(0).getMessageKey());
    }

    @Test
    public void importKeepsLaterHostNamesWhenTheStoredSerialIsEmpty() {
        List<EquipmentExchange.FieldRef> fields = Collections.singletonList(
                new EquipmentExchange.FieldRef("sn", "S/N:", "text", false));
        List<EquipmentExchange.TypeRef> types = Arrays.asList(
                new EquipmentExchange.TypeRef("ups", "\u0418\u0411\u041f", false, fields),
                new EquipmentExchange.TypeRef("printer", "\u041f\u0440\u0438\u043d\u0442\u0435\u0440", false, fields),
                new EquipmentExchange.TypeRef("room", "Room", true, Collections.<EquipmentExchange.FieldRef>emptyList()));
        List<EquipmentExchange.Named> statuses = Collections.singletonList(
                new EquipmentExchange.Named("in_use", "\u0412 \u044d\u043a\u0441\u043f\u043b\u0443\u0430\u0442\u0430\u0446\u0438\u0438"));
        EquipmentExchange.Node room = new EquipmentExchange.Node(1, null, "Room", "STP-1", true, "room");
        EquipmentExchange.Node ups = new EquipmentExchange.Node(4, Integer.valueOf(1), "BV500I-GR", "BZ-4", false, "ups", "", "");
        EquipmentExchange.Node printer = new EquipmentExchange.Node(5, Integer.valueOf(1), "HP2035", "BZ-5", false, "printer", "", "");
        List<String> headers = Arrays.asList("\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435", "\u0422\u0438\u043f", "\u041f\u043b\u043e\u0449\u0430\u0434\u043a\u0430", "S/N:");
        EquipmentSheet.Sheet sheet = new EquipmentSheet.Sheet("\u041e\u0431\u043e\u0440\u0443\u0434\u043e\u0432\u0430\u043d\u0438\u0435", headers, Arrays.asList(
                new EquipmentSheet.Record(14, Arrays.asList("BV500I-GR", "\u0418\u0411\u041f", "Room", "9B2138A10791")),
                new EquipmentSheet.Record(28, Arrays.asList("BV500I-GR", "\u0418\u0411\u041f", "Room", "9B2138A10792")),
                new EquipmentSheet.Record(32, Arrays.asList("BV500I-GR", "\u0418\u0411\u041f", "Room", "9B2138A10789")),
                new EquipmentSheet.Record(33, Arrays.asList("HP2035", "\u041f\u0440\u0438\u043d\u0442\u0435\u0440", "Room", "VNC4D36053")),
                new EquipmentSheet.Record(36, Arrays.asList("HP2035", "\u041f\u0440\u0438\u043d\u0442\u0435\u0440", "Room", "VNC4D36054"))));
        EquipmentExchange.Plan plan = EquipmentExchange.plan(sheet, types, statuses, Arrays.asList(room, ups, printer),
                "in_use", new HashMap<String, String>(), blankPeople());
        assertTrue(plan.getIssues().toString(), plan.getIssues().isEmpty());
        assertEquals(5, plan.getChanges().size());
        assertEquals(Integer.valueOf(4), plan.getChanges().get(0).getExistingId());
        assertNull(plan.getChanges().get(1).getExistingId());
        assertNull(plan.getChanges().get(2).getExistingId());
        assertEquals(Integer.valueOf(5), plan.getChanges().get(3).getExistingId());
        assertNull(plan.getChanges().get(4).getExistingId());
    }

    private static EquipmentExchange.Directory servicePeople() {
        return new EquipmentExchange.Directory() {
            @Override
            public EquipmentExchange.Person find(String raw) {
                if (raw == null || raw.trim().isEmpty()) {
                    return EquipmentExchange.Person.blank();
                }
                if ("ivan".equalsIgnoreCase(raw.trim())) {
                    return EquipmentExchange.Person.found("ivan");
                }
                return EquipmentExchange.Person.missing();
            }
        };
    }

    private static EquipmentExchange.Directory blankPeople() {
        return new EquipmentExchange.Directory() {
            @Override
            public EquipmentExchange.Person find(String raw) {
                return EquipmentExchange.Person.blank();
            }
        };
    }

    private static FlowGraphDto graph(Object... parts) {
        FlowGraphDto graph = new FlowGraphDto();
        for (Object part : parts) {
            if (part instanceof FlowNodeDto) {
                graph.getNodes().add((FlowNodeDto) part);
            } else if (part instanceof FlowLinkDto) {
                graph.getLinks().add((FlowLinkDto) part);
            }
        }
        return graph;
    }

    private static FlowNodeDto node(String id, String kind, String issueTypeId, String issueStatusId, String statusKey) {
        FlowNodeDto node = new FlowNodeDto();
        node.setId(id);
        node.setKind(kind);
        node.setIssueTypeId(issueTypeId);
        node.setIssueStatusId(issueStatusId);
        node.setStatusKey(statusKey);
        return node;
    }

    @Test
    public void subtypeKeepsParentFieldsUntilItsOwnKeyReplacesThem() {
        AssetTypeDto parent = typeDto("laptop", "\u041d\u043e\u0443\u0442\u0431\u0443\u043a", "");
        parent.setFields(Arrays.asList(fieldDto("serial", "\u0421\u0435\u0440\u0438\u0439\u043d\u044b\u0439"), fieldDto("model", "\u041c\u043e\u0434\u0435\u043b\u044c")));
        AssetTypeDto child = typeDto("thin", "\u0423\u043b\u044c\u0442\u0440\u0430\u0431\u0443\u043a", "laptop");
        child.setFields(new ArrayList<FieldDto>(Collections.singletonList(fieldDto("model", "\u0421\u0432\u043e\u044f"))));
        FieldInheritance.apply(Arrays.asList(parent, child));
        assertEquals(2, child.getFields().size());
        assertTrue(child.getFields().get(0).isInherited());
        assertEquals("serial", child.getFields().get(0).getFieldKey());
        assertEquals("\u041d\u043e\u0443\u0442\u0431\u0443\u043a", child.getFields().get(0).getInheritedFrom());
        assertFalse(child.getFields().get(1).isInherited());
        assertEquals("\u0421\u0432\u043e\u044f", child.getFields().get(1).getLabel());
        assertFalse(parent.getFields().get(0).isInherited());
        FieldInheritance.apply(Arrays.asList(parent, child));
        assertEquals(2, child.getFields().size());
        assertTrue(child.getFields().get(0).isInherited());
    }

    @Test
    public void exportKeepsFiftyThousandRows() {
        assertFalse(ReportLines.cutsExport(50000));
        assertTrue(ReportLines.cutsExport(50001));
        assertEquals(300, ReportLines.PAGE);
        assertEquals(50000, ReportLines.EXPORT_MAX);
    }

    @Test
    public void inboundPhraseReplacesTheDefaultOnTheWayIn() {
        assertEquals("linkIncludes", LinkRules.shownPhrase(LinkRules.PART, true, ""));
        assertEquals("linkPart", LinkRules.shownPhrase(LinkRules.PART, false, "\u0432\u043a\u043b\u044e\u0447\u0430\u0435\u0442"));
        assertEquals("\u043d\u0435\u0441\u0451\u0442", LinkRules.shownPhrase(LinkRules.INSTALLED, true, "\u043d\u0435\u0441\u0451\u0442"));
        assertEquals(80, LinkRules.inbound(new String(new char[90]).replace('\0', 'a')).length());
    }

    private static AssetTypeDto typeDto(String key, String label, String parent) {
        AssetTypeDto type = new AssetTypeDto();
        type.setTypeKey(key);
        type.setLabel(label);
        type.setParentTypeKey(parent);
        return type;
    }

    private static FieldDto fieldDto(String key, String label) {
        FieldDto field = new FieldDto();
        field.setFieldKey(key);
        field.setLabel(label);
        field.setKind("text");
        return field;
    }

    private static FlowLinkDto link(String from, String to) {
        FlowLinkDto link = new FlowLinkDto();
        link.setFrom(from);
        link.setTo(to);
        return link;
    }
}
