package com.assetstree.jira.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReportCatalogTest {
    @Test
    public void chartsAndFieldKeysStayInsideTheStoredColumns() {
        assertEquals("gantt", ReportCatalog.chart("gantt"));
        assertEquals("histogram", ReportCatalog.chart(" Histogram "));
        assertEquals("", ReportCatalog.chart("waffle"));
        assertEquals("field", ReportCatalog.group("field"));
        assertEquals("model", ReportCatalog.fieldKey("Model"));
        assertEquals("", ReportCatalog.fieldKey("1bad"));
        assertEquals("", ReportCatalog.fieldKey("has space"));
    }
}
