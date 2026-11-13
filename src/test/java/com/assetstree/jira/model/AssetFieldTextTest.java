package com.assetstree.jira.model;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class AssetFieldTextTest {
    @Test
    public void pathLabelKeepsTheWholeLineAndEachPieceFromTheEnd() {
        List<String> hints = AssetFieldText.lookupHints("Ноутбук / Компьютер admin");
        assertEquals(3, hints.size());
        assertEquals("Ноутбук / Компьютер admin", hints.get(0));
        assertEquals("Компьютер admin", hints.get(1));
        assertEquals("Ноутбук", hints.get(2));
    }

    @Test
    public void emptyAndClearedValuesYieldNoHints() {
        assertEquals(0, AssetFieldText.lookupHints(null).size());
        assertEquals(0, AssetFieldText.lookupHints("  ").size());
        assertEquals(0, AssetFieldText.lookupHints("-1").size());
        List<String> plain = AssetFieldText.lookupHints("  SD-14  ");
        assertEquals(1, plain.size());
        assertEquals("SD-14", plain.get(0));
    }
}
