package com.assetstree.jira.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NameShortTest {
    @Test
    public void theFamilyNameStaysAndTheRestBecomeInitials() {
        assertEquals("Бондаренко О.С.", NameShort.of("Бондаренко Олег Сергеевич"));
        assertEquals("Хуснуллин Н.Р.", NameShort.of("  Хуснуллин   Никита Рустамович "));
        assertEquals("Бондаренко О.С.", NameShort.of("Бондаренко О.С."));
        assertEquals("Иванова А.-М.", NameShort.of("Иванова Анна-Мария"));
        assertEquals("Казань", NameShort.of("Казань"));
        assertEquals("", NameShort.of("  "));
        assertEquals("", NameShort.of(null));
    }
}
