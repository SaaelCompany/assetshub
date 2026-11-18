package com.assetstree.jira.automation;

import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Jira ships a phrase only from a written AJS.I18n.getText('key') call. */
public class AutomationPhrasesTest {
    @Test
    public void automationScreenAsksForEachPhraseByItsKey() throws Exception {
        InputStream input = AutomationPhrasesTest.class.getResourceAsStream("/js/automation-rules.js");
        assertNotNull(input);
        InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[4096];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            builder.append(buffer, 0, read);
        }
        reader.close();
        String script = builder.toString();
        assertTrue(script.contains("AJS.I18n.getText('asset-tree.automation.linked')"));
        assertTrue(script.contains("AJS.I18n.getText('asset-tree.automation.anyStatus')"));
        assertTrue(script.contains("AJS.I18n.getText('asset-tree.ui.statusStock')"));
        assertTrue(script.contains("AJS.I18n.getText('asset-tree.automation.day')"));
        assertTrue(script.contains("AJS.I18n.getText('asset-tree.automation.issueStatus')"));
        assertFalse(script.contains("text('asset-tree."));
    }
}
