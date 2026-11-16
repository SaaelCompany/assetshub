package com.assetstree.jira.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AssetValidatorIssueKeyTest {
    @Test
    public void plainKeyAndBrowseLinkBothBecomeTheKey() {
        assertEquals("SD-14", AssetValidator.normalizeIssueKey(" sd-14 "));
        assertEquals("SD-14", AssetValidator.normalizeIssueKey("https://jira.example/browse/SD-14"));
        assertEquals("SERVICE_DESK-3", AssetValidator.normalizeIssueKey("service_desk-3"));
        assertTrue(AssetValidator.isIssueKey("https://jira.example/browse/SD-14"));
        assertFalse(AssetValidator.isIssueKey("not a request"));
    }
}
