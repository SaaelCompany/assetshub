package com.assetstree.jira.model;

import com.assetstree.jira.dto.ModuleSetupDto;
import com.assetstree.jira.dto.PortalRuleDto;
import com.assetstree.jira.servlet.PortalPageMarkup;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TniRulesTest {
    private static final long DAY = TniClock.DAY;

    @Test
    public void privateAddressWinsAndDangerousTargetsAreRefused() {
        assertEquals("192.168.1.20", TniAddress.choose(Arrays.asList("8.8.8.8", "192.168.1.20", "10.0.0.5")));
        assertEquals("8.8.8.8", TniAddress.choose(Collections.singletonList("8.8.8.8")));
        assertEquals("", TniAddress.choose(Arrays.asList("127.0.0.1", "169.254.169.254", "0.0.0.0", "224.0.0.1", "010.1.2.3", "localhost")));
        assertFalse(TniAddress.allowed("169.254.1.1"));
        assertTrue(TniAddress.allowed("10.2.3.4"));
    }

    @Test
    public void browserReportPicksKindAddressAndStableIdentity() {
        TniSightDraft phone = draft("Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X)", "iPhone", null);
        phone.setScreen("390x844");
        phone.setAddresses(Collections.singletonList("10.8.0.4"));
        TniFacts facts = TniFacts.from(phone);
        assertEquals(TniPolicy.PHONE, facts.kind);
        assertEquals("iOS", facts.os);
        assertEquals("10.8.0.4", facts.address);
        assertEquals("390x844", facts.screen);

        TniSightDraft laptop = draft("Mozilla/5.0 (Windows NT 10.0) Chrome/120.0", "Win32", Boolean.TRUE);
        laptop.setScreen("1920x1080");
        laptop.setCores(Integer.valueOf(8));
        laptop.setMemory(Double.valueOf(16));
        assertEquals(TniPolicy.LAPTOP, TniFacts.from(laptop).kind);
        assertEquals("Windows", TniFacts.from(laptop).os);
        assertEquals("Chrome", TniFacts.from(laptop).browser);
        assertEquals("8", TniFacts.from(laptop).cores);
        assertEquals("16", TniFacts.from(laptop).memory);

        TniSightDraft tablet = draft("Mozilla/5.0 (iPad; CPU OS 15_0 like Mac OS X) Safari/605", "iPad", null);
        assertEquals(TniPolicy.TABLET, TniFacts.from(tablet).kind);
        assertEquals("Safari", TniFacts.from(tablet).browser);

        TniSightDraft desk = draft("Mozilla/5.0 (X11; Linux x86_64) Chrome/120.0", "Linux", Boolean.FALSE);
        desk.setScreen("2560x1440");
        assertEquals(TniPolicy.COMPUTER, TniFacts.from(desk).kind);

        String id = TniFacts.identity("STP", "ivanov", facts);
        assertEquals(id, TniFacts.identity("STP", "ivanov", TniFacts.parse(facts.summary())));
        assertFalse(id.equals(TniFacts.identity("STP", "petrova", facts)));
        String card = TniText.describe(facts, "TNI. Collected from the portal.", "Connection", "Name", "Address", "System",
                "Browser", "Screen", "Cores", "Memory", "Video", "Request", "STP-12");
        assertTrue(card.startsWith("TNI"));
        assertTrue(card.contains("Address: 10.8.0.4"));
        assertTrue(card.contains("Request: STP-12"));
        TniSightDraft seen = draft("Mozilla/5.0 (Windows NT 10.0) Chrome/120.0", "Win32", Boolean.FALSE);
        seen.setSeen("::ffff:10.4.4.8");
        seen.setHost("pc-12.corp.local.");
        TniFacts peered = TniFacts.from(seen);
        assertEquals("10.4.4.8", peered.seen);
        assertEquals("10.4.4.8", peered.address);
        assertEquals("pc-12.corp.local", peered.host);
        assertEquals("pc-12.corp.local", TniFacts.parse(peered.summary()).host);
        TniSightDraft gateway = draft("Mozilla/5.0 (Windows NT 10.0) Chrome/120.0", "Win32", Boolean.FALSE);
        gateway.setSeen("203.0.113.9");
        gateway.setHost("203.0.113.9");
        TniFacts outside = TniFacts.from(gateway);
        assertEquals("203.0.113.9", outside.seen);
        assertEquals("", outside.address);
        assertEquals("", outside.host);
        assertEquals("", TniName.clean("192.168.0.1"));
        assertEquals("", TniPeer.address("127.0.0.1"));
        assertEquals("", TniPeer.address("172.17.0.1"));
        assertEquals("", TniPeer.client("172.17.0.1", null, null, null));
        assertEquals("10.1.1.5", TniPeer.client("172.17.0.1", "10.9.9.9, 10.1.1.5", "8.8.8.8", null));
        assertEquals("192.168.0.15", TniPeer.client("172.18.0.1", "172.18.0.1", "192.168.0.15", null));
        assertEquals("10.4.4.8", TniPeer.client("10.4.4.8", "8.8.8.8", null, null));
        assertEquals("10.8.0.20", TniPeer.client("127.0.0.1", null, null, "for=10.8.0.20"));
        TniSightDraft docker = draft("Mozilla/5.0 (Windows NT 10.0) Chrome/120.0", "Win32", Boolean.FALSE);
        docker.setSeen("172.17.0.1");
        TniFacts bridge = TniFacts.from(docker);
        assertEquals("", bridge.seen);
        assertEquals("", bridge.address);
        assertEquals("Phone Ivanov", TniText.name("Phone", "Ivanov", "10.8.0.4"));
        assertEquals("KRUL-IT", TniText.name("Компьютер", "пользователь Бондаренко Олег Сергеевич",
                "10.8.0.4", "krul-it.budzdorov.ru"));
        assertEquals("KRUL-IT", TniText.preferred(
                "Компьютер пользователь Бондаренко Олег Сергеевич",
                "Компьютер пользователь Бондаренко Олег Сергеевич",
                "Компьютер", "пользователь Бондаренко Олег Сергеевич", "10.8.0.4", "KRUL-IT.budzdorov.ru"));
        assertEquals("KRUL-IT", TniText.preferred("krul-it", "krul-it", "Компьютер", "Иванов",
                "10.8.0.4", "krul-it.budzdorov.ru"));
        assertEquals("Касса", TniText.preferred("Касса", "Компьютер Иванов", "Компьютер", "Иванов",
                "10.8.0.4", "krul-it.budzdorov.ru"));
        assertEquals(java.util.Arrays.asList("krul-it.budzdorov.ru", "KRUL-IT", "10.8.0.4"),
                TniTarget.list("Компьютер Иванов", "10.8.0.4", "krul-it.budzdorov.ru", ""));
        assertEquals(java.util.Collections.singletonList("KRUL-IT"),
                TniTarget.list("KRUL-IT", "", "", ""));
        assertEquals("KRUL-IT", TniTarget.shown(
                TniTarget.list("KRUL-IT", "10.8.0.4", "krul-it.budzdorov.ru", "")));
        assertEquals("krul-it.budzdorov.ru", TniTarget.shown(
                TniTarget.list("Компьютер", "10.8.0.4", "krul-it.budzdorov.ru", "")));
        String sites = TniSites.store("KRUL = Крулево\nMSK=Москва\nKRUL-IT = Кабинет");
        assertEquals("KRUL = Крулево\nMSK = Москва\nKRUL-IT = Кабинет", TniSites.text(sites));
        assertEquals("Кабинет", TniSites.match(sites, "krul-it.budzdorov.ru", "KRUL-IT"));
        assertEquals("Крулево", TniSites.match(sites, "", "KRUL-BUH"));
        assertEquals("Москва", TniSites.match(sites, "msk-01.budzdorov.ru", ""));
        assertEquals("", TniSites.match(sites, "", "KRULIT"));
        assertEquals("", TniSites.match("", "", "KRUL-IT"));
        String listed = TniSites.store("KRUL = Крулево");
        assertTrue(TniSites.admits("", "", "Компьютер Иванов"));
        assertTrue(TniSites.admits(listed, "krul-it.budzdorov.ru", "Компьютер Иванов"));
        assertTrue(TniSites.admits(listed, "", "KRUL-BUH"));
        assertFalse(TniSites.admits(listed, "", "Компьютер Иванов Иван Иванович"));
        assertFalse(TniSites.admits(listed, "other-pc.budzdorov.ru", "OTHER-PC"));
        assertTrue(TniText.owned(card));
        assertFalse(TniText.owned("A note written by a person"));
    }

    @Test
    public void storageThenThreeChecksThenDropOrRenew() {
        long seen = 1_000_000L;
        assertEquals(TniClock.Action.WAIT, look(seen, 0, 0, 0, true, seen + 89 * DAY).action);
        TniClock.Mark first = look(seen, 0, 0, 0, true, seen + 90 * DAY);
        assertEquals(TniClock.Action.PING, first.action);
        assertEquals(TniClock.Action.WAIT, look(seen, 0, 0, 0, false, seen + 90 * DAY).action);
        long probed = seen + 90 * DAY;
        assertEquals(TniClock.Action.WAIT, look(seen, probed, 1, probed, true, probed + DAY - 1000).action);
        assertEquals(TniClock.Action.PING, look(seen, probed, 1, probed, true, probed + DAY).action);
        assertEquals(TniClock.Action.DROP, look(seen, probed, 3, probed + 2 * DAY, true, probed + 3 * DAY).action);
        assertEquals(TniClock.Action.WAIT, look(0, 0, 0, 0, true, probed).action);
    }

    @Test
    public void settingsRoundTripKeepsTypesAndDefaults() {
        ModuleSetupDto draft = new ModuleSetupDto();
        draft.setTniKeep(Integer.valueOf(14));
        draft.setTniProbeDays(Integer.valueOf(3));
        draft.setTniProbeTries(Integer.valueOf(3));
        draft.setTniAction("delete");
        draft.setTniComputer("pc");
        draft.setTniLaptop("laptop");
        draft.setTniPhone("Not A Key");
        draft.setTniTablet("tablet");
        ModuleSetupDto read = ModuleSetup.read(ModuleSetup.write(draft));
        assertEquals(Integer.valueOf(14), read.getTniKeep());
        assertEquals(Integer.valueOf(3), read.getTniProbeDays());
        assertEquals(Integer.valueOf(3), read.getTniProbeTries());
        assertEquals(TniPolicy.DELETE, read.getTniAction());
        assertEquals("pc", read.getTniComputer());
        assertEquals("laptop", TniPolicy.typeFor(read, TniPolicy.LAPTOP));
        assertEquals("", read.getTniPhone());
        assertEquals("tablet", TniPolicy.typeFor(read, TniPolicy.TABLET));

        ModuleSetupDto defaults = ModuleSetup.read("");
        assertEquals(Integer.valueOf(90), defaults.getTniKeep());
        assertEquals(TniPolicy.TRASH, defaults.getTniAction());
        ModuleSetupDto patch = new ModuleSetupDto();
        patch.setMapWidth(Integer.valueOf(800));
        ModuleSetupDto merged = ModuleSetup.read(ModuleSetup.write(ModuleSetup.merge(read, patch)));
        assertEquals(Integer.valueOf(14), merged.getTniKeep());
        assertEquals("pc", merged.getTniComputer());
    }

    @Test
    public void browserReportRidesTheCreateRequestAndRootIsTheFallback() throws Exception {
        String json = "{\"portalId\":4,\"projectKey\":\"STP\",\"ua\":\"Mozilla/5.0 (Windows NT 10.0) Chrome/120.0\","
                + "\"platform\":\"Win32\",\"screen\":\"1920x1080\",\"cores\":8,\"memory\":8,\"touch\":0,"
                + "\"gpu\":\"Intel\",\"battery\":false,\"addresses\":[\"10.1.1.8\",\"8.8.8.8\"]}";
        TniSightDraft draft = TniCookie.read(java.net.URLEncoder.encode(json, "UTF-8"));
        assertEquals("Win32", draft.getPlatform());
        assertEquals("1920x1080", draft.getScreen());
        assertEquals(Integer.valueOf(8), draft.getCores());
        assertEquals(Boolean.FALSE, draft.getBattery());
        assertEquals("10.1.1.8", draft.getAddresses().get(0));
        TniHold.keep("customer", draft);
        assertEquals("Win32", TniHold.take("customer").getPlatform());
        assertNull(TniHold.take("customer"));
        assertNull(TniPlace.choose(java.util.Collections.<TniPlace.Spot>emptyList(),
                java.util.Collections.<PortalRules.Answer>emptyList(), null));
    }

    @Test
    public void deviceSitsOnTheBranchNamedInTheRequest() {
        java.util.List<TniPlace.Spot> spots = Arrays.asList(
                new TniPlace.Spot(1, null, "Усть-Лабинск", true),
                new TniPlace.Spot(2, Integer.valueOf(1), "111", true),
                new TniPlace.Spot(3, Integer.valueOf(1), "112", true),
                new TniPlace.Spot(4, Integer.valueOf(2), "Ноутбук Ивана", false));
        assertEquals(Integer.valueOf(1), TniPlace.choose(spots,
                Arrays.asList(answer("Филиал", "Усть-Лабинск")), null));
        assertEquals(Integer.valueOf(2), TniPlace.choose(spots,
                Arrays.asList(answer("Кабинет", "111")), null));
        assertEquals(Integer.valueOf(1), TniPlace.choose(spots,
                Arrays.asList(answer("Кабинет", "111"), answer("Кабинет", "112")), null));
        assertNull(TniPlace.choose(spots, Arrays.asList(answer("Тест", "Не выбрано")), null));
        assertEquals(Integer.valueOf(2), TniPlace.choose(spots, Collections.<PortalRules.Answer>emptyList(), Integer.valueOf(4)));
        assertEquals(Integer.valueOf(1), TniPlace.choose(spots,
                Arrays.asList(answer("Кабинет", "111")), Integer.valueOf(1)));

        PortalConditionDraft condition = new PortalConditionDraft();
        condition.setField("Филиал");
        condition.setOption("Усть-Лабинск");
        PortalRuleDto rule = new PortalRuleDto();
        rule.setAssetId(1);
        rule.setConditions(Collections.singletonList(condition));
        Map<Integer, Integer> parents = new HashMap<Integer, Integer>();
        parents.put(Integer.valueOf(1), null);
        parents.put(Integer.valueOf(2), Integer.valueOf(1));
        PortalRuleDto chosen = PortalRules.choose(Collections.singletonList(rule),
                Arrays.asList(answer("Филиал", "Усть-Лабинск")), parents);
        assertEquals(1, chosen.getAssetId());
    }

    @Test
    public void createPageCanCarryTheDeviceScriptBesideTheField() {
        byte[] page = "<html><body><p>portal</p></body></html>".getBytes(StandardCharsets.UTF_8);
        byte[] withField = PortalPageMarkup.insertBoot(page, PortalPageMarkup.snippet("/jira", 4, "STP", "customfield_1", true, true));
        String html = new String(PortalPageMarkup.insertTni(withField, PortalPageMarkup.tniScript("/jira")), StandardCharsets.UTF_8);
        assertTrue(html.contains("tni-portal.js"));
        assertTrue(html.contains("data-tni=\"1\""));
        assertTrue(html.contains("data-project=\"STP\""));
        assertEquals(html, new String(PortalPageMarkup.insertTni(html.getBytes(StandardCharsets.UTF_8), PortalPageMarkup.tniScript("/jira")), StandardCharsets.UTF_8));
        byte[] already = "<html><body><div id=\"asset-tree-portal-boot\"></div></body></html>".getBytes(StandardCharsets.UTF_8);
        String added = new String(PortalPageMarkup.insertTni(already, PortalPageMarkup.tniScript("/jira")), StandardCharsets.UTF_8);
        assertTrue(added.contains("tni-portal.js"));
        assertTrue(added.indexOf("tni-portal.js") < added.indexOf("</body>"));
    }

    @Test
    public void portalSightingReusesTheComputerAlreadyInTheTree() {
        TniSame.Card manual = new TniSame.Card(4, "KRUL-IT", false, "", "");
        TniSame.Card cabinet = new TniSame.Card(2, "KRUL-IT", true, "", "");
        TniSame.Card other = new TniSame.Card(8, "Касса", false, "", "");
        TniSame.Card born = new TniSame.Card(9, "KRUL-IT", false, "tni:abc", "");
        assertEquals(4, TniSame.pick(Arrays.asList(cabinet, other, manual), 0, "krul-it.budzdorov.ru", "10.8.0.4"));
        assertEquals(4, TniSame.pick(Arrays.asList(born, manual), 0, "KRUL-IT", ""));
        assertEquals(9, TniSame.pick(Arrays.asList(born), 0, "krul-it.budzdorov.ru", ""));
        assertEquals(0, TniSame.pick(Arrays.asList(other), 8, "krul-it.budzdorov.ru", ""));
        assertEquals(4, TniSame.pick(Arrays.asList(other, manual), 8, "krul-it.budzdorov.ru", ""));
        TniSame.Card watched = new TniSame.Card(6, "Касса", false, "", "10.8.0.4");
        assertEquals(6, TniSame.pick(Arrays.asList(watched, other), 0, "", "10.8.0.4"));
        assertEquals(0, TniSame.pick(Arrays.asList(other), 0, "", ""));
        assertEquals(0, TniSame.pick(Arrays.asList(cabinet), 0, "krul-it.budzdorov.ru", ""));
        assertTrue(TniSame.claimed("tni:abc", "abc"));
        assertFalse(TniSame.claimed("", "abc"));
        assertFalse(TniSame.claimed("ad:krul-it", "abc"));
    }

    private static TniClock.Mark look(long seen, long probeFrom, int fails, long lastProbe, boolean address, long now) {
        return TniClock.look(seen, probeFrom, fails, lastProbe, 90, 3, 3, address, now);
    }

    private static PortalRules.Answer answer(String field, String option) {
        return new PortalRules.Answer(field, "", "", option, option);
    }

    private static TniSightDraft draft(String ua, String platform, Boolean battery) {
        TniSightDraft draft = new TniSightDraft();
        draft.setUa(ua);
        draft.setPlatform(platform);
        draft.setBattery(battery);
        return draft;
    }
}
