package com.assetstree.jira.model;

import com.assetstree.jira.dto.ModuleSetupDto;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ModuleSetupGlyphTest {
    @Test
    public void glyphsSurviveTheOptionRow() {
        ModuleSetupDto draft = new ModuleSetupDto();
        draft.setGlyphs("rack^Rack^80^200^r,0,0,80,200,eef2f7,2b3440,2");
        ModuleSetupDto read = ModuleSetup.read(ModuleSetup.write(draft));
        assertEquals("rack^Rack^80^200^r,0,0,80,200,eef2f7,2b3440,2", read.getGlyphs());
    }

    @Test
    public void glyphsCannotBreakLaterOptions() {
        ModuleSetupDto draft = new ModuleSetupDto();
        draft.setGlyphs("a=b;c");
        assertEquals("abc", ModuleSetup.read(ModuleSetup.write(draft)).getGlyphs());
    }
}
