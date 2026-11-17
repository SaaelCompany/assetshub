package com.assetstree.jira.model;

import com.assetstree.jira.dto.ModuleSetupDto;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ModuleSetupGlyphTest {
    @Test
    public void glyphsSurviveTheOptionRow() {
        ModuleSetupDto draft = new ModuleSetupDto();
        draft.setGlyphs("rack^Rack^80^200^r,0,0,80,200,eef2f7,2b3440,2");
        ModuleSetupDto read = ModuleSetup.read(ModuleSetup.write(draft));
        assertEquals("rack^Rack^80^200^r,0,0,80,200,eef2f7,2b3440,2", read.getGlyphs());
    }

    @Test
    public void storedRosterStillOpensTheCalendar() {
        ModuleSetupDto draft = new ModuleSetupDto();
        draft.setScheduleBoards("roster");
        draft.setScheduleOpen("roster");
        ModuleSetupDto read = ModuleSetup.read(ModuleSetup.write(draft));
        assertEquals("calendar", read.getScheduleBoards());
        assertEquals("calendar", read.getScheduleOpen());
        assertTrue(ModuleSetup.calendarOn(read.getScheduleBoards()));
        assertTrue(ModuleSetup.calendarOn("calendar"));
        assertTrue(ModuleSetup.calendarOn(""));
    }

    @Test
    public void branchMailRoundTripsBesideTheSharedList() {
        ModuleSetupDto draft = new ModuleSetupDto();
        draft.setReportMailBranch(Boolean.TRUE);
        draft.setReportMailPart("2026-11-17:b.4.JIRAUSER10000,admin");
        ModuleSetupDto read = ModuleSetup.read(ModuleSetup.write(draft));
        assertEquals(Boolean.TRUE, read.getReportMailBranch());
        assertEquals("2026-11-17:b.4.JIRAUSER10000,admin", read.getReportMailPart());
        draft.setReportMailBranch(Boolean.FALSE);
        assertFalse(Boolean.TRUE.equals(ModuleSetup.read(ModuleSetup.write(draft)).getReportMailBranch()));
    }

    @Test
    public void glyphsCannotBreakLaterOptions() {
        ModuleSetupDto draft = new ModuleSetupDto();
        draft.setGlyphs("a=b;c");
        assertEquals("abc", ModuleSetup.read(ModuleSetup.write(draft)).getGlyphs());
    }
}
