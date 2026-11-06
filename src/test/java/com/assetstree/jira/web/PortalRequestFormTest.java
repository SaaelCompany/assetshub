package com.assetstree.jira.web;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class PortalRequestFormTest {
    @Test
    public void fillsTheAssetFieldAndDropsUnknownInput() {
        PortalRequestForm form = form();
        Map<String, String> input = new LinkedHashMap<String, String>();
        input.put("summary", "  Не включается  ");
        input.put("description", "После обновления");
        input.put("customfield_10010", "999");
        input.put("other", "no");

        PortalRequestForm.Outcome outcome = PortalRequestForm.compose(form, "1", input, "42");

        assertNull(outcome.getFailureKey());
        assertEquals("Не включается", outcome.getValues().get("summary"));
        assertEquals("После обновления", outcome.getValues().get("description"));
        assertEquals("42", outcome.getValues().get("customfield_10010"));
        assertFalse(outcome.getValues().containsKey("other"));
    }

    @Test
    public void asksForARequiredFieldAndAListedOption() {
        PortalRequestForm form = form();
        PortalRequestForm.Outcome empty = PortalRequestForm.compose(form, "2", Collections.<String, String>emptyMap(), "42");
        assertEquals("asset-tree.error.field.required", empty.getFailureKey());
        assertEquals("Тема", empty.getFailureArg());

        Map<String, String> input = new LinkedHashMap<String, String>();
        input.put("summary", "Обслужить");
        input.put("urgency", "later");
        PortalRequestForm.Outcome option = PortalRequestForm.compose(form, "2", input, "42");
        assertEquals("asset-tree.error.portalRequest.option", option.getFailureKey());
        assertEquals("Срочность", option.getFailureArg());
    }

    @Test
    public void keepsAChosenOptionAndSkipsAnEmptyOptionalField() {
        PortalRequestForm form = form();
        Map<String, String> input = new LinkedHashMap<String, String>();
        input.put("summary", "Обслужить");
        input.put("urgency", "high");
        PortalRequestForm.Outcome outcome = PortalRequestForm.compose(form, "2", input, "7");
        assertNull(outcome.getFailureKey());
        assertEquals("high", outcome.getValues().get("urgency"));
        assertFalse(outcome.getValues().containsKey("description"));
        assertEquals("7", outcome.getValues().get("customfield_10010"));
    }

    @Test
    public void rejectsAnUnknownTypeAndClipsTheSummary() {
        PortalRequestForm.Outcome missing = PortalRequestForm.compose(form(), "9", Collections.<String, String>emptyMap(), "1");
        assertEquals("asset-tree.error.portalRequest.type", missing.getFailureKey());

        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            summary.append('a');
        }
        Map<String, String> input = new LinkedHashMap<String, String>();
        input.put("summary", summary.toString());
        PortalRequestForm.Outcome clipped = PortalRequestForm.compose(form(), "1", input, "1");
        assertNull(clipped.getFailureKey());
        assertEquals(255, clipped.getValues().get("summary").length());
    }

    private static PortalRequestForm form() {
        PortalRequestForm.Field summary = new PortalRequestForm.Field("summary", "Тема", true, "text", Collections.<PortalRequestForm.Option>emptyList());
        PortalRequestForm.Field description = new PortalRequestForm.Field("description", "Описание", false, "textarea", Collections.<PortalRequestForm.Option>emptyList());
        PortalRequestForm.Field urgency = new PortalRequestForm.Field("urgency", "Срочность", true, "select", Arrays.asList(
                new PortalRequestForm.Option("low", "Низкая"),
                new PortalRequestForm.Option("high", "Высокая")));
        PortalRequestForm.Type problem = new PortalRequestForm.Type("1", "Сообщить о проблеме", "", Arrays.asList(summary, description),
                Collections.singletonList("customfield_10010"));
        PortalRequestForm.Type service = new PortalRequestForm.Type("2", "Запрос на обслуживание", "", Arrays.asList(summary, description, urgency),
                Collections.singletonList("customfield_10010"));
        return PortalRequestForm.ready(Arrays.asList(problem, service));
    }
}
