package com.assetstree.jira.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AssetFieldMatchTest {
    @Test
    public void nameInTheRequestProjectWins() {
        List<AssetFieldMatch.Row> rows = Arrays.asList(
                row(4, "Test", "AST-4", "OTHER", false, "Склад"),
                row(9, "Test", "AST-9", "STP", false, "Усть-Лабинск"));
        assertEquals(Integer.valueOf(9), AssetFieldMatch.choose(rows, hints("Test"), "STP", ""));
    }

    @Test
    public void objectKeyMatchesRegardlessOfCase() {
        List<AssetFieldMatch.Row> rows = Collections.singletonList(
                row(3, "Ноутбук", "sd-14", "OTHER", false, ""));
        assertEquals(Integer.valueOf(3), AssetFieldMatch.choose(rows, hints("SD-14"), "STP", ""));
    }

    @Test
    public void viewLabelUsesTheKeyBeforeTheName() {
        List<AssetFieldMatch.Row> rows = Arrays.asList(
                row(3, "Другой", "SD-14", "STP", false, ""),
                row(8, "Test", "AST-8", "STP", false, ""));
        assertEquals(Integer.valueOf(3), AssetFieldMatch.choose(rows, AssetFieldText.lookupHints("SD-14  Test"), "STP", ""));
    }

    @Test
    public void repeatedNameUsesThePlaceNamedOnTheRequest() {
        List<AssetFieldMatch.Row> rows = Arrays.asList(
                row(1, "Test", "AST-1", "STP", false, "Краснодар / Склад"),
                row(2, "Test", "AST-2", "STP", false, "Усть-Лабинск / Кабинет"));
        assertEquals(Integer.valueOf(2), AssetFieldMatch.choose(rows, hints("Test"), "STP", "Филиал Усть-Лабинск"));
    }

    @Test
    public void repeatedNameWithoutAPlaceStaysUnresolved() {
        List<AssetFieldMatch.Row> rows = Arrays.asList(
                row(1, "Test", "AST-1", "STP", false, "Склад"),
                row(2, "Test", "AST-2", "STP", false, "Кабинет"));
        assertNull(AssetFieldMatch.choose(rows, hints("Test"), "STP", "Тест новостей"));
    }

    @Test
    public void oneNameOutsideTheRequestProjectStillMatches() {
        List<AssetFieldMatch.Row> rows = Collections.singletonList(
                row(6, "test", "AST-6", "EQ", false, ""));
        assertEquals(Integer.valueOf(6), AssetFieldMatch.choose(rows, hints("Test"), "STP", ""));
    }

    @Test
    public void portalPathSelectsTheLeaf() {
        List<AssetFieldMatch.Row> rows = Arrays.asList(
                row(1, "Ноутбук", "AST-1", "STP", true, ""),
                row(2, "Компьютер admin", "AST-2", "STP", false, "Ноутбук"),
                row(5, "Canon 2035", "KRUL-IT", "STP", false, "Принтер"));
        assertEquals(Integer.valueOf(2), AssetFieldMatch.choose(rows,
                AssetFieldText.lookupHints("Ноутбук / Компьютер admin"), "STP", ""));
        assertEquals(Integer.valueOf(5), AssetFieldMatch.choose(rows, hints("KRUL-IT"), "STP", ""));
        assertEquals(Integer.valueOf(5), AssetFieldMatch.choose(rows,
                AssetFieldText.lookupHints("Принтер / Canon 2035"), "STP", ""));
    }

    @Test
    public void equipmentWinsOverAPlaceWithTheSameName() {
        List<AssetFieldMatch.Row> rows = Arrays.asList(
                row(1, "Test", "PLC-1", "STP", true, ""),
                row(2, "Test", "AST-2", "STP", false, "Test"));
        assertEquals(Integer.valueOf(2), AssetFieldMatch.choose(rows, hints("Test"), "STP", ""));
    }

    @Test
    public void ambiguousFullTextDoesNotFallThroughToAnotherAsset() {
        List<AssetFieldMatch.Row> rows = Arrays.asList(
                row(1, "Test", "AST-1", "STP", false, "Склад"),
                row(2, "Test", "AST-2", "STP", false, "Кабинет"),
                row(3, "Ноутбук", "AST-3", "STP", false, ""));
        assertNull(AssetFieldMatch.choose(rows, AssetFieldText.lookupHints("Ноутбук / Test"), "STP", ""));
    }

    private static List<String> hints(String text) {
        return AssetFieldText.lookupHints(text);
    }

    private static AssetFieldMatch.Row row(int id, String name, String key, String project, boolean location, String place) {
        return new AssetFieldMatch.Row(id, name, key, project, location, place);
    }
}
