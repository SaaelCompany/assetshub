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
        assertEquals("type+status", ReportCatalog.group("type+status"));
        assertEquals("bar,pie", ReportCatalog.chart("bar,pie"));
        assertEquals("", ReportCatalog.group("type+nope"));
        assertEquals("", ReportCatalog.group("type+status+place"));
        assertEquals("", ReportCatalog.chart("histogram,scatter"));
        assertEquals("model", ReportCatalog.fieldKey("Model"));
        assertEquals("", ReportCatalog.fieldKey("1bad"));
        assertEquals("", ReportCatalog.fieldKey("has space"));
    }
}
