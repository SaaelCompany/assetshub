package com.assetstree.jira.model;

import com.assetstree.jira.dto.AssetTypeDto;
import com.assetstree.jira.dto.FieldDto;
import com.assetstree.jira.dto.FlowGraphDto;
import com.assetstree.jira.dto.FlowLinkDto;
import com.assetstree.jira.dto.FlowNodeDto;
import com.assetstree.jira.dto.ModuleSetupDto;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
    public void timesheetDoublesNightAndSkipsLeave() {
        assertEquals(2, TimesheetMath.factor("night", 8, null));
        assertEquals(0, TimesheetMath.factor("leave", 0, null));
        assertEquals(1, TimesheetMath.factor("trip", 8, null));
        assertEquals(16, TimesheetMath.hours(8, 2));
        assertEquals(0, TimesheetMath.hours(8, 0));
        List<LocalDate[]> leave = new ArrayList<LocalDate[]>();
        leave.add(new LocalDate[] { LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 2) });
        int[] tally = StaffCalendar.tally(31, 8, leave, 2026, 11);
        assertEquals(20, tally[0]);
        assertEquals(160, tally[1]);
        assertEquals(1, tally[2]);
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
