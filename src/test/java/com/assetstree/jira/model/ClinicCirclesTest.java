package com.assetstree.jira.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ClinicCirclesTest {
    @Test
    public void nameCollapsesSpaceAndStopsAtEighty() {
        assertEquals("Клиника Краснодар", ClinicCircles.name("  Клиника   Краснодар  "));
        assertEquals("", ClinicCircles.name("   "));
        assertEquals(80, ClinicCircles.name(repeat("а", 90)).length());
    }

    @Test
    public void membersKeepRealKeysAndDropDuplicates() {
        assertEquals(Arrays.asList("JIRAUSER10000", "o.bondarenko"),
                ClinicCircles.members(Arrays.asList(" JIRAUSER10000 ", "bad key", "o.bondarenko", "JIRAUSER10000", "")));
    }

    @Test
    public void placesKeepPositiveIds() {
        assertEquals(Arrays.asList(Integer.valueOf(4), Integer.valueOf(9)),
                ClinicCircles.places(Arrays.asList(Integer.valueOf(4), Integer.valueOf(0), null, Integer.valueOf(4), Integer.valueOf(9))));
        assertTrue(ClinicCircles.places(Collections.<Integer>emptyList()).isEmpty());
    }

    private static String repeat(String token, int times) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < times; i++) {
            text.append(token);
        }
        return text.toString();
    }
}
