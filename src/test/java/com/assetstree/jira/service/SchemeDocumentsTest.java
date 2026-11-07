package com.assetstree.jira.service;

import com.assetstree.jira.model.AssetException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SchemeDocumentsTest {
    @Test
    public void nameAndBodyStayWithinTheStore() {
        assertEquals("Scheme", SchemeDocuments.nameOf("  "));
        assertEquals("Hall", SchemeDocuments.nameOf(" Hall "));
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 90; i++) {
            longName.append('a');
        }
        assertEquals(80, SchemeDocuments.nameOf(longName.toString()).length());
        assertEquals("{\"floors\":[]}", SchemeDocuments.bodyOf("  {\"floors\":[]}  "));
        assertEquals(0, SchemeDocuments.placeIdOf(null));
        assertEquals(0, SchemeDocuments.placeIdOf("{}"));
        assertEquals(12, SchemeDocuments.placeIdOf("{\"name\":\"Hall\",\"placeId\":12}"));
        assertEquals(0, SchemeDocuments.placeIdOf("{\"placeId\":0}"));
    }

    @Test
    public void aDiagramMustBeAnObjectAndFitTheColumn() {
        assertRejected(null);
        assertRejected("");
        assertRejected("[]");
        StringBuilder huge = new StringBuilder("{");
        while (huge.length() <= SchemeDocuments.MAX_BODY) {
            huge.append('x');
        }
        assertRejected(huge.toString());
    }

    private static void assertRejected(String body) {
        try {
            SchemeDocuments.bodyOf(body);
            fail(String.valueOf(body == null ? 0 : body.length()));
        } catch (AssetException ex) {
            assertEquals(400, ex.getStatus());
        }
    }
}
