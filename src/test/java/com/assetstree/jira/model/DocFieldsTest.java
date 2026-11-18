package com.assetstree.jira.model;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DocFieldsTest {
    @Test
    public void aSerialFieldFillsTheRussianToken() {
        DocFields.Source source = new DocFields.Source();
        source.name = "Атол 55Ф";
        source.fields.put("serial", "00106228866670");
        source.labels.put("serial", "Серийный номер");
        source.life = "5 лет";
        source.mol = "Иванов И.И.";
        Map<String, String> values = DocFields.values(source);
        assertEquals("Атол 55Ф", values.get(DocxFill.normalize("наименование")));
        assertEquals("00106228866670", values.get(DocxFill.normalize("серийный")));
        assertEquals("00106228866670", values.get("serial"));
        assertEquals("5 лет", values.get(DocxFill.normalize("срок")));
        assertEquals("Иванов И.И.", values.get(DocxFill.normalize("мол")));
    }
}
