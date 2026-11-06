package com.assetstree.jira.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AutomationNoteTest {
    @Test
    public void fillsTheSentenceWithoutMessageFormatQuotes() {
        assertEquals("Equipment ASIT-1 \"O'Brien\": changed.",
                AutomationNote.text("Equipment {0} \"{1}\": {2}.", "ASIT-1", "O'Brien", "changed"));
    }
}
