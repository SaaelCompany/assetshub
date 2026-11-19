package com.assetstree.jira.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AutomationNoteTest {
    @Test
    public void fillsTheSentenceWithoutMessageFormatQuotes() {
        assertEquals("Equipment ASIT-1 \"O'Brien\": changed.",
                AutomationNote.text("Equipment {0} \"{1}\": {2}.", "ASIT-1", "O'Brien", "changed"));
    }

    @Test
    public void equipmentChangeRecognizesOnlyTheOldRequestNote() {
        assertTrue(AutomationNote.equipmentChange("Оборудование BZ-980 «KRUL-BUH»: создано."));
        assertTrue(AutomationNote.equipmentChange("Equipment ASIT-1 \"O'Brien\": changed."));
        assertTrue(AutomationNote.equipmentChange("Оборудование P-4 «Старый»: восстановлено.\n"));
        assertFalse(AutomationNote.equipmentChange("Оборудование стоит в кабинете."));
        assertFalse(AutomationNote.equipmentChange("Equipment ASIT-1 \"O'Brien\": moved."));
        assertFalse(AutomationNote.equipmentChange(""));
    }
}
