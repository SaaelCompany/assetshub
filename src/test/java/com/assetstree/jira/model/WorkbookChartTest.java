package com.assetstree.jira.model;

import com.assetstree.jira.dto.ReportBucketDto;
import com.assetstree.jira.dto.ReportLineDto;
import com.assetstree.jira.dto.ReportVennDto;
import com.assetstree.jira.dto.ReportViewDto;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WorkbookChartTest {
    @Test
    public void reportSheetKeepsTheChartBesideTheSummary() throws Exception {
        ReportViewDto view = view("By status", "donut", false);
        view.getRows().add(bucket("In use", 4));
        view.getRows().add(bucket("Repair", 1));
        WorkbookSheet.Chart chart = ReportWorkbook.chart(view, "Count", "Book value", "Start", "Length");
        assertEquals("doughnut", chart.getPlot());
        assertEquals(1, chart.getValueColumn());
        assertEquals(2, chart.getFirstRow());
        assertEquals(3, chart.getLastRow());
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(row("In use", "\u0001i:4"));
        rows.add(row("Repair", "\u0001i:1"));
        Map<String, String> parts = unzip(WorkbookSheet.write(page("By status", rows, chart)));
        String picture = parts.get("xl/charts/chart1.xml");
        assertTrue(picture.contains("<c:doughnutChart>"));
        assertTrue(picture.contains("'By status'!$A$2:$A$3"));
        assertTrue(picture.contains("'By status'!$B$2:$B$3"));
        assertTrue(picture.contains(">In use<"));
        assertTrue(picture.contains(">4<"));
        assertTrue(parts.get("[Content_Types].xml").contains("drawingml.chart+xml"));
        assertTrue(parts.get("xl/worksheets/sheet1.xml").contains("<drawing r:id=\"rId1\"/>"));
        assertTrue(parts.get("xl/worksheets/_rels/sheet1.xml.rels").contains("drawings/drawing1.xml"));
        assertTrue(parts.get("xl/drawings/_rels/drawing1.xml.rels").contains("charts/chart1.xml"));
    }

    @Test
    public void equipmentSheetStaysATable() throws Exception {
        Map<String, String> parts = unzip(WorkbookSheet.write(headers(), rows()));
        assertFalse(parts.containsKey("xl/charts/chart1.xml"));
        assertFalse(parts.get("xl/worksheets/sheet1.xml").contains("drawing"));
        assertFalse(parts.get("[Content_Types].xml").contains("chart+xml"));
    }

    @Test
    public void moneyHistogramAndScatterUseTheAmountColumn() {
        ReportViewDto money = view("Cost", "histogram", true);
        money.getRows().add(bucket("Laptop", 2));
        WorkbookSheet.Chart histogram = ReportWorkbook.chart(money, "Count", "Book value", "Start", "Length");
        assertEquals("col", histogram.getPlot());
        assertTrue(histogram.isHistogram());
        assertEquals(2, histogram.getValueColumn());
        money.setChart("scatter");
        WorkbookSheet.Chart scatter = ReportWorkbook.chart(money, "Count", "Book value", "Start", "Length");
        assertEquals("scatter", scatter.getPlot());
        assertEquals(1, scatter.getXColumn());
        assertEquals(2, scatter.getValueColumn());
        assertFalse(scatter.isIndexX());
        ReportViewDto counts = view("Age", "scatter", false);
        counts.getRows().add(bucket("New", 3));
        WorkbookSheet.Chart indexed = ReportWorkbook.chart(counts, "Count", "Book value", "Start", "Length");
        assertTrue(indexed.isIndexX());
        assertEquals(1, indexed.getValueColumn());
    }

    @Test
    public void ganttUsesPurchaseAndWarrantyDates() throws Exception {
        ReportViewDto view = view("Warranty", "gantt", false);
        view.getRows().add(bucket("Open", 2));
        ReportLineDto first = line("AST-1", "Switch", "2024-01-01", "2024-01-11");
        ReportLineDto same = line("AST-2", "Server", "2024-01-06", "2024-01-06");
        ReportLineDto blank = line("AST-3", "Phone", "", "2024-02-01");
        view.getLines().add(first);
        view.getLines().add(same);
        view.getLines().add(blank);
        WorkbookSheet.Chart chart = ReportWorkbook.chart(view, "Count", "Book value", "Start", "Length");
        assertTrue(chart.isStacked());
        assertEquals(2, chart.getLabels().size());
        assertEquals("Switch", chart.getLabels().get(0));
        assertEquals("0", chart.getBases().get(0));
        assertEquals("11", chart.getValues().get(0));
        assertEquals("5", chart.getBases().get(1));
        assertEquals("1", chart.getValues().get(1));
        String picture = unzip(WorkbookSheet.write(page("Warranty", new ArrayList<List<String>>(), chart))).get("xl/charts/chart1.xml");
        assertTrue(picture.contains("<c:barDir val=\"bar\"/>"));
        assertTrue(picture.contains("<c:grouping val=\"stacked\"/>"));
        assertTrue(picture.contains("<a:noFill/>"));
        assertTrue(picture.contains(">Switch<"));
    }

    @Test
    public void vennBecomesColumnsOfTheOverlaps() {
        ReportViewDto view = view("Kinds", "venn", false);
        ReportVennDto venn = new ReportVennDto();
        venn.getLabels().add("A");
        venn.getLabels().add("B");
        venn.getLabels().add("C");
        venn.getOnly().add(Integer.valueOf(3));
        venn.getOnly().add(Integer.valueOf(1));
        venn.getOnly().add(Integer.valueOf(0));
        venn.getPairs().add(Integer.valueOf(2));
        venn.getPairs().add(Integer.valueOf(4));
        venn.getPairs().add(Integer.valueOf(5));
        venn.setAll(6);
        view.setVenn(venn);
        WorkbookSheet.Chart chart = ReportWorkbook.chart(view, "Count", "Book value", "Start", "Length");
        assertEquals("col", chart.getPlot());
        assertEquals(7, chart.getLabels().size());
        assertEquals("A + B", chart.getLabels().get(3));
        assertEquals("6", chart.getValues().get(6));
    }

    @Test
    public void everyMarkedChartIsDrawn() throws Exception {
        ReportViewDto view = view("Types", "line,radar,gantt", false);
        view.getRows().add(bucket("Laptop", 3));
        view.getRows().add(bucket("Printer", 1));
        view.getLines().add(line("AST-1", "Laptop", "2024-01-01", "2026-01-01"));
        java.util.Map<String, String> captions = new java.util.HashMap<String, String>();
        captions.put("line", "Line");
        captions.put("radar", "Radar");
        captions.put("gantt", "Gantt");
        List<WorkbookSheet.Chart> charts = ReportWorkbook.charts(view, "Count", "Book value", "Start", "Length", captions);
        assertEquals(3, charts.size());
        assertEquals("line", charts.get(0).getPlot());
        assertEquals("radar", charts.get(1).getPlot());
        assertTrue(charts.get(2).isStacked());
        assertEquals("line", ReportWorkbook.chart(view, "Count", "Book value", "Start", "Length").getPlot());
        List<WorkbookSheet.Page> pages = new ArrayList<WorkbookSheet.Page>();
        pages.add(new WorkbookSheet.Page("Types", headers(), rows(), charts));
        Map<String, String> parts = unzip(WorkbookSheet.write(pages));
        assertTrue(parts.get("xl/charts/chart1.xml").contains("<c:lineChart>"));
        assertTrue(parts.get("xl/charts/chart1.xml").contains(">Line<"));
        assertTrue(parts.get("xl/charts/chart2.xml").contains("<c:radarChart>"));
        assertTrue(parts.get("xl/charts/chart3.xml").contains("<c:grouping val=\"stacked\"/>"));
        assertTrue(parts.get("xl/drawings/drawing1.xml").contains("r:id=\"rId3\""));
        assertTrue(parts.get("xl/drawings/_rels/drawing1.xml.rels").contains("charts/chart3.xml"));
        assertFalse(parts.containsKey("xl/charts/chart4.xml"));
    }

    @Test
    public void emptyReportHasNoPicture() {
        assertNull(ReportWorkbook.chart(view("Empty", "pie", false), "Count", "Book value", "Start", "Length"));
    }

    @Test
    public void aQuoteInTheSheetNameIsDoubledInTheFormula() throws Exception {
        WorkbookSheet.Chart chart = WorkbookSheet.Chart.categories("pie", "O'Brien", "Count", 0, 1, 2, 2, false);
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(row("Open", "\u0001i:2"));
        String picture = unzip(WorkbookSheet.write(page("O'Brien", rows, chart))).get("xl/charts/chart1.xml");
        assertTrue(picture.contains("<c:pieChart>"));
        assertTrue(picture.contains("'O''Brien'!$A$2:$A$2"));
        assertNotNull(picture);
    }

    private static ReportViewDto view(String title, String chart, boolean money) {
        ReportViewDto view = new ReportViewDto();
        view.setTitle(title);
        view.setChart(chart);
        if (money) {
            view.setSum("1.00");
        }
        return view;
    }

    private static ReportBucketDto bucket(String label, int count) {
        return new ReportBucketDto(label, label, "#0052CC", count);
    }

    private static ReportLineDto line(String key, String name, String from, String to) {
        ReportLineDto line = new ReportLineDto(1, key, name, "");
        line.setFrom(from);
        line.setTo(to);
        return line;
    }

    private static List<String> headers() {
        List<String> headers = new ArrayList<String>();
        headers.add("Key");
        return headers;
    }

    private static List<List<String>> rows() {
        List<List<String>> rows = new ArrayList<List<String>>();
        List<String> row = new ArrayList<String>();
        row.add("AST-1");
        rows.add(row);
        return rows;
    }

    private static List<String> row(String label, String value) {
        List<String> cells = new ArrayList<String>();
        cells.add(label);
        cells.add(value);
        return cells;
    }

    private static List<WorkbookSheet.Page> page(String name, List<List<String>> rows, WorkbookSheet.Chart chart) {
        List<WorkbookSheet.Page> pages = new ArrayList<WorkbookSheet.Page>();
        pages.add(new WorkbookSheet.Page(name, headers(), rows, chart));
        return pages;
    }

    private static Map<String, String> unzip(byte[] bytes) throws Exception {
        Map<String, String> parts = new HashMap<String, String>();
        ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes));
        ZipEntry entry;
        byte[] buffer = new byte[4096];
        while ((entry = zip.getNextEntry()) != null) {
            if (entry.isDirectory()) {
                continue;
            }
            StringBuilder text = new StringBuilder();
            int read;
            while ((read = zip.read(buffer)) >= 0) {
                text.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
            }
            parts.put(entry.getName(), text.toString());
        }
        zip.close();
        return parts;
    }
}
