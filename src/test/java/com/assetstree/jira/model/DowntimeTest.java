package com.assetstree.jira.model;

import com.assetstree.jira.dto.ModuleSetupDto;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DowntimeTest {
    @Test
    public void emptySettingMeansRepairAndADashMeansNone() {
        assertEquals("", Downtime.store(null));
        assertEquals("", Downtime.store("  "));
        assertTrue(Downtime.counts("", Statuses.REPAIR));
        assertFalse(Downtime.counts("", "broken"));
        assertEquals(Downtime.NONE, Downtime.store("-"));
        assertTrue(Downtime.statuses("-").isEmpty());
        assertFalse(Downtime.counts("-", Statuses.REPAIR));
    }

    @Test
    public void listKeepsValidKeysAndDropsTheRest() {
        assertEquals("repair,broken", Downtime.store(" repair, broken, repair, !!, "));
        assertEquals("in_use", Downtime.store("active"));
        assertTrue(Downtime.counts("repair,broken", "broken"));
        assertFalse(Downtime.counts("repair,broken", Statuses.IN_USE));
    }

    @Test
    public void aMoveBetweenDowntimeStatusesStaysOneStay() {
        long day = RepairTime.DAY;
        List<RepairTime.Change> changes = Arrays.asList(
                new RepairTime.Change(day, Statuses.REPAIR),
                new RepairTime.Change(day * 2, "broken"),
                new RepairTime.Change(day * 4, Statuses.IN_USE));
        List<RepairTime.Span> spans = RepairTime.spans(changes, false, day, day * 5,
                new HashSet<String>(Arrays.asList(Statuses.REPAIR, "broken")));
        assertEquals(1, spans.size());
        assertEquals(day * 3, RepairTime.overlap(spans));
        List<RepairTime.Span> split = RepairTime.spans(changes, false, day, day * 5);
        assertEquals(1, split.size());
        assertEquals(day, RepairTime.overlap(split));
    }

    @Test
    public void setupKeepsTheListWhenAnotherSaveOmitsIt() {
        ModuleSetupDto stored = new ModuleSetupDto();
        stored.setReportIdleStatuses("repair,broken");
        ModuleSetupDto read = ModuleSetup.read(ModuleSetup.write(stored));
        assertEquals("repair,broken", read.getReportIdleStatuses());
        ModuleSetupDto patch = new ModuleSetupDto();
        assertEquals("repair,broken", ModuleSetup.merge(read, patch).getReportIdleStatuses());
        patch.setReportIdleStatuses("-");
        assertEquals("-", ModuleSetup.merge(read, patch).getReportIdleStatuses());
        patch.setReportIdleStatuses("");
        assertEquals("", ModuleSetup.merge(read, patch).getReportIdleStatuses());
    }
}
