package com.assetstree.jira.model;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AssetMailLetterTest {
    @Test
    public void emptySelectionKeepsEveryLine() {
        assertTrue(AssetMailLetter.includes("", "name"));
        assertTrue(AssetMailLetter.includes(null, "status"));
        assertFalse(AssetMailLetter.includes("key,name", "status"));
        assertTrue(AssetMailLetter.includes("key, name", "name"));
        assertFalse(AssetMailLetter.includes("none", "name"));
    }

    @Test
    public void composeSkipsBlankLinesAndKeepsTheExtraSentence() {
        String letter = AssetMailLetter.compose(Arrays.asList("Ключ: STP-1", " ", "Имя: Принтер"), "Проверьте склад");
        assertEquals("Ключ: STP-1\nИмя: Принтер\n\nПроверьте склад", letter);
        assertEquals("Только фраза", AssetMailLetter.compose(null, "Только фраза"));
    }
}
