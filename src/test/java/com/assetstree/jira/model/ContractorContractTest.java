package com.assetstree.jira.model;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ContractorContractTest {
    @Test
    public void anEmptyContractStaysOffTheCard() {
        assertFalse(ContractorContract.present("", "  "));
        assertFalse(ContractorContract.present(null, null));
        assertNull(ContractorContract.reject("", "", "", ""));
        assertEquals(0, ContractorContract.phase("", "", "2026-01-01", "2026-12-31", LocalDate.of(2026, 6, 1)));
    }

    @Test
    public void aNameOrANumberPutsTheContractOnTheCard() {
        assertTrue(ContractorContract.present("Сервис", ""));
        assertTrue(ContractorContract.present("", "12/26"));
        assertNull(ContractorContract.reject("Сервис", "12/26", "2026-01-01", "2026-12-31"));
        assertEquals(1, ContractorContract.phase("Сервис", "", "", "", LocalDate.of(2026, 6, 1)));
        assertEquals(1, ContractorContract.phase("Сервис", "", "2026-01-01", "2026-12-31", LocalDate.of(2026, 6, 1)));
        assertEquals(1, ContractorContract.phase("Сервис", "", "2026-06-01", "2026-06-01", LocalDate.of(2026, 6, 1)));
        assertEquals(3, ContractorContract.phase("Сервис", "", "2026-07-01", "", LocalDate.of(2026, 6, 1)));
        assertEquals(2, ContractorContract.phase("Сервис", "", "", "2026-05-01", LocalDate.of(2026, 6, 1)));
    }

    @Test
    public void datesAloneOrAReversedTermAreRejected() {
        assertEquals("asset-tree.error.life", ContractorContract.reject("", "", "2026-01-01", ""));
        assertEquals("asset-tree.error.life", ContractorContract.reject("Сервис", "", "2026-12-31", "2026-01-01"));
        assertEquals("asset-tree.error.life", ContractorContract.reject("Сервис", "", "yesterday", ""));
        assertEquals("asset-tree.error.life", ContractorContract.reject(repeat('а', 121), "", "", ""));
        assertEquals("asset-tree.error.life", ContractorContract.reject("", repeat('1', 161), "", ""));
    }

    private static String repeat(char symbol, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(symbol);
        }
        return builder.toString();
    }
}
