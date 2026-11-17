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
        assertEquals("done", ReportCatalog.group("done"));
        assertEquals("done+month", ReportCatalog.group("done+month"));
        assertEquals("done+kind+who", ReportCatalog.group("done+kind+who"));
        assertEquals("done+month+place", ReportCatalog.group("done+month+place"));
        assertEquals("", ReportCatalog.group("who"));
        assertEquals("", ReportCatalog.group("month+done"));
        assertEquals("", ReportCatalog.group("done+holder"));
        assertEquals("", ReportCatalog.group("done+status"));
        assertEquals("", ReportCatalog.group("done+kind+month+place"));
        assertEquals("branch", ReportCatalog.group("branch"));
        assertEquals("type+branch", ReportCatalog.group("type+branch"));
        assertEquals("asset", ReportCatalog.opsGroup("asset"));
        assertEquals("branch+type", ReportCatalog.opsGroup("branch+type"));
        assertEquals("", ReportCatalog.opsGroup("month"));
        assertEquals("", ReportCatalog.opsGroup("asset+type+place"));
        assertEquals("faults", ReportCatalog.measure("Faults"));
        assertEquals("cost", ReportCatalog.measure("cost"));
        assertEquals("count", ReportCatalog.measure("nope"));
        assertEquals("model", ReportCatalog.fieldKey("Model"));
        assertEquals("", ReportCatalog.fieldKey("1bad"));
        assertEquals("", ReportCatalog.fieldKey("has space"));
    }
}
