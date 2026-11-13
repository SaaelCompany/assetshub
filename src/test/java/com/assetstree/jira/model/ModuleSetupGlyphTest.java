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
    public void timesheetOnlyTurnsTheCalendarOff() {
        ModuleSetupDto draft = new ModuleSetupDto();
        draft.setScheduleBoards("roster");
        ModuleSetupDto read = ModuleSetup.read(ModuleSetup.write(draft));
        assertEquals("roster", read.getScheduleBoards());
        assertFalse(ModuleSetup.calendarOn(read.getScheduleBoards()));
        assertTrue(ModuleSetup.calendarOn("calendar"));
        assertTrue(ModuleSetup.calendarOn("calendar,roster"));
        assertTrue(ModuleSetup.calendarOn(""));
    }

    @Test
    public void glyphsCannotBreakLaterOptions() {
        ModuleSetupDto draft = new ModuleSetupDto();
        draft.setGlyphs("a=b;c");
        assertEquals("abc", ModuleSetup.read(ModuleSetup.write(draft)).getGlyphs());
    }
}
