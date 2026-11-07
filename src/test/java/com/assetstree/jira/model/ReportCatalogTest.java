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
        assertEquals("histogram,scatter", ReportCatalog.chart("histogram,scatter"));
        assertEquals("donut,pie,bar,bars,line,area,histogram,scatter,radar,gantt,venn",
                ReportCatalog.chart("donut,pie,bar,bars,line,area,histogram,scatter,radar,gantt,venn"));
        assertEquals("", ReportCatalog.chart("bar,waffle"));
        assertEquals("", ReportCatalog.group("type+nope"));
        assertEquals("", ReportCatalog.group("type+status+place"));
        assertEquals("model", ReportCatalog.fieldKey("Model"));
        assertEquals("", ReportCatalog.fieldKey("1bad"));
        assertEquals("", ReportCatalog.fieldKey("has space"));
    }
}
