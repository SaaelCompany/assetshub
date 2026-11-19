package com.assetstree.jira.model;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DocxFillTest {
    @Test
    public void aTokenSplitAcrossRunsIsFilled() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>"
                + "<w:p><w:r><w:t>{{наиме</w:t></w:r><w:r><w:t>нование}}</w:t></w:r></w:p>"
                + "<w:p><w:r><w:t>s/n: {{серийный}}</w:t></w:r></w:p>"
                + "<w:p><w:r><w:t>Шапка без метки</w:t></w:r></w:p>"
                + "</w:body></w:document>";
        Map<String, String> values = new HashMap<String, String>();
        values.put("наименование", "Атол 55Ф");
        values.put("серийный", "00106228866670");
        byte[] filled = DocxFill.fill(docx(xml), values);
        String document = documentXml(filled);
        assertTrue(document.contains("Атол 55Ф"));
        assertTrue(document.contains("00106228866670"));
        assertTrue(document.contains("Шапка без метки"));
        assertTrue(!document.contains("{{"));
    }

    @Test
    public void anUnknownTokenStaysVisible() throws Exception {
        String xml = "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>"
                + "<w:p><w:r><w:t>{{причина}}</w:t></w:r></w:p></w:body></w:document>";
        String document = documentXml(DocxFill.fill(docx(xml), new HashMap<String, String>()));
        assertTrue(document.contains("{{причина}}"));
    }

    private static byte[] docx(String xml) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(out);
        zip.putNextEntry(new ZipEntry("word/document.xml"));
        zip.write(xml.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
        zip.close();
        return out.toByteArray();
    }

    private static String documentXml(byte[] docx) throws Exception {
        ZipInputStream in = new ZipInputStream(new java.io.ByteArrayInputStream(docx));
        ZipEntry entry;
        while ((entry = in.getNextEntry()) != null) {
            if ("word/document.xml".equals(entry.getName())) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    @Test
    public void normalizeDropsSpacesAndCase() {
        assertEquals("серийный", DocxFill.normalize(" Серийный "));
        assertEquals("s/n", DocxFill.normalize("S/N"));
    }
}
