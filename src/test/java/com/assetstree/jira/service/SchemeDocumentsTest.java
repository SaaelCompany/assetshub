package com.assetstree.jira.service;

import com.assetstree.jira.model.AssetException;
import com.atlassian.jira.util.json.JSONArray;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

    @Test
    public void aSharedPlanKeepsTheDrawingAndSplitsEquipment() throws Exception {
        String drawing = "{\"name\":\"Hall\",\"placeId\":3,\"floors\":[{\"id\":\"f1\",\"objects\":["
                + "{\"id\":\"w1\",\"type\":\"wall\"},"
                + "{\"id\":\"a1\",\"type\":\"asset\",\"assetBinding\":{\"workspaceId\":\"ATD\",\"objectId\":\"5\"}},"
                + "{\"id\":\"a2\",\"type\":\"asset\",\"assetBinding\":{\"workspaceId\":\"STP\",\"objectId\":\"9\"}}"
                + "]}]}";
        String kept = SchemeDocuments.stripGuestAssets(drawing, "ATD");
        assertFalse(kept.contains("\"a2\""));
        assertTrue(kept.contains("\"w1\""));
        assertTrue(kept.contains("\"a1\""));
        assertEquals(kept, SchemeDocuments.stripGuestAssets(kept, "ATD"));

        String pins = SchemeDocuments.takeProjectAssets(drawing, "STP");
        JSONArray taken = new JSONArray(pins);
        assertEquals(1, taken.length());
        assertEquals("f1", taken.getJSONObject(0).getString("floorId"));
        assertEquals("a2", taken.getJSONObject(0).getJSONObject("object").getString("id"));
        assertEquals("[]", SchemeDocuments.takeProjectAssets(drawing, "OTHER"));

        String merged = SchemeDocuments.mergePlacements(kept, Collections.singletonList(pins));
        assertTrue(merged.contains("\"a2\""));
        assertTrue(merged.contains("\"w1\""));
        String again = SchemeDocuments.mergePlacements(merged, Collections.singletonList(pins));
        assertEquals(1, count(again, "\"a2\""));

        String moved = "[{\"floorId\":\"missing\",\"object\":{\"id\":\"a9\",\"type\":\"asset\",\"assetBinding\":{\"workspaceId\":\"STP\",\"objectId\":\"9\"}}}]";
        String onFirst = SchemeDocuments.mergePlacements(kept, Collections.singletonList(moved));
        assertTrue(onFirst.contains("\"a9\""));
        assertEquals(kept, SchemeDocuments.mergePlacements(kept, Collections.<String>emptyList()));
    }

    @Test
    public void shareKeysSkipTheOwnerAndRepeatKeys() {
        assertEquals(Arrays.asList("STP"), SchemeDocuments.shareKeys("ATD", Arrays.asList("STP", " stp ", "ATD", "", "STP", null)));
        assertEquals(Collections.emptyList(), SchemeDocuments.shareKeys("ATD", null));
        java.util.ArrayList<String> many = new java.util.ArrayList<String>();
        for (int i = 0; i < 25; i++) {
            many.add("P" + i);
        }
        assertEquals(SchemeDocuments.MAX_SHARES, SchemeDocuments.shareKeys("ATD", many).size());
    }

    private static int count(String text, String token) {
        int found = 0;
        int at = 0;
        while (at >= 0) {
            at = text.indexOf(token, at);
            if (at < 0) {
                break;
            }
            found++;
            at += token.length();
        }
        return found;
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
