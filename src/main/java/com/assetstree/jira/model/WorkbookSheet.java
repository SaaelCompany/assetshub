package com.assetstree.jira.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Equipment book in the Excel .xlsx package. Each equipment type is its own
 * worksheet, so a field that exists on one type is not a column on another.
 * Cells are written as text so a place path or a key is not rewritten as a
 * number. A date that Excel stored as a serial is read back as yyyy-MM-dd.
 * The sheet XML is read without the JDK XML factory: inside Jira that factory
 * has no provider.
 */
public final class WorkbookSheet {
    private static final int MAX_BYTES = 8_000_000;
    private static final int MAX_COLUMNS = 512;
    private static final int MAX_NAME = 31;
    private static final long EXCEL_EPOCH = 25569L;

    private WorkbookSheet() {
    }

    /** One worksheet: a name Excel can show, a header row, and data rows. */
    public static final class Page {
        private final String name;
        private final List<String> headers;
        private final List<List<String>> rows;
        private final Chart chart;
        private final List<Chart> charts;

        public Page(String name, List<String> headers, List<List<String>> rows) {
            this(name, headers, rows, (Chart) null);
        }

        public Page(String name, List<String> headers, List<List<String>> rows, Chart chart) {
            this(name, headers, rows, chart == null ? null : java.util.Collections.singletonList(chart));
        }

        public Page(String name, List<String> headers, List<List<String>> rows, List<Chart> charts) {
            this.name = name;
            this.headers = headers;
            this.rows = rows;
            List<Chart> kept = new ArrayList<Chart>();
            if (charts != null) {
                for (Chart picture : charts) {
                    if (picture != null && picture.usable()) {
                        kept.add(picture);
                    }
                }
            }
            this.charts = kept;
            this.chart = kept.isEmpty() ? null : kept.get(0);
        }

        public String getName() {
            return name;
        }

        public List<String> getHeaders() {
            return headers;
        }

        public List<List<String>> getRows() {
            return rows;
        }

        public Chart getChart() {
            return chart;
        }

        public List<Chart> getCharts() {
            return charts;
        }
    }

    /** The pictures drawn on one worksheet, with the chart part numbers. */
    private static final class SheetPicture {
        private final List<Chart> charts = new ArrayList<Chart>();
        private final List<Integer> files = new ArrayList<Integer>();
    }

    /**
     * A drawing on the sheet. Categories and values are either a cell range
     * on this sheet or a list of literals when the picture is not the summary
     * columns, as with a Gantt bar or a Venn overlap.
     */
    public static final class Chart {
        private final String plot;
        private final String title;
        private final String series;
        private final String baseSeries;
        private final int catColumn;
        private final int valueColumn;
        private final int xColumn;
        private final int firstRow;
        private final int lastRow;
        private final boolean histogram;
        private final boolean indexX;
        private final boolean stacked;
        private final List<String> labels;
        private final List<String> values;
        private final List<String> bases;

        private Chart(String plot, String title, String series, String baseSeries, int catColumn, int valueColumn,
                int xColumn, int firstRow, int lastRow, boolean histogram, boolean indexX, boolean stacked,
                List<String> labels, List<String> values, List<String> bases) {
            this.plot = plot == null ? "col" : plot;
            this.title = title == null ? "" : title;
            this.series = series == null ? "" : series;
            this.baseSeries = baseSeries == null ? "" : baseSeries;
            this.catColumn = catColumn;
            this.valueColumn = valueColumn;
            this.xColumn = xColumn;
            this.firstRow = firstRow;
            this.lastRow = lastRow;
            this.histogram = histogram;
            this.indexX = indexX;
            this.stacked = stacked;
            this.labels = labels;
            this.values = values;
            this.bases = bases;
        }

        public static Chart categories(String plot, String title, String series, int catColumn, int valueColumn,
                int firstRow, int lastRow, boolean histogram) {
            return new Chart(plot, title, series, "", catColumn, valueColumn, -1, firstRow, lastRow, histogram, false, false, null, null, null);
        }

        public static Chart scatter(String title, String series, int xColumn, int valueColumn, int firstRow, int lastRow) {
            return new Chart("scatter", title, series, "", 0, valueColumn, xColumn, firstRow, lastRow, false, false, false, null, null, null);
        }

        public static Chart scatterIndex(String title, String series, int valueColumn, int firstRow, int lastRow) {
            return new Chart("scatter", title, series, "", 0, valueColumn, -1, firstRow, lastRow, false, true, false, null, null, null);
        }

        public static Chart columnsOf(String title, String series, List<String> labels, List<String> values) {
            return new Chart("col", title, series, "", 0, 1, -1, 0, 0, false, false, false, labels, values, null);
        }

        public static Chart gantt(String title, String startName, String spanName, List<String> labels, List<String> offsets, List<String> spans) {
            return new Chart("bar", title, spanName, startName, 0, 1, -1, 0, 0, false, false, true, labels, spans, offsets);
        }

        public boolean usable() {
            if (labels != null) {
                return !labels.isEmpty() && values != null && values.size() == labels.size()
                        && (bases == null || bases.size() == labels.size());
            }
            return firstRow >= 2 && lastRow >= firstRow && catColumn >= 0 && valueColumn >= 0;
        }

        public String getPlot() {
            return plot;
        }

        public int getValueColumn() {
            return valueColumn;
        }

        public int getXColumn() {
            return xColumn;
        }

        public int getFirstRow() {
            return firstRow;
        }

        public int getLastRow() {
            return lastRow;
        }

        public boolean isHistogram() {
            return histogram;
        }

        public boolean isIndexX() {
            return indexX;
        }

        public boolean isStacked() {
            return stacked;
        }

        public List<String> getLabels() {
            return labels;
        }

        public List<String> getValues() {
            return values;
        }

        public List<String> getBases() {
            return bases;
        }

        private int pointCount() {
            if (labels != null) {
                return labels.size();
            }
            return lastRow - firstRow + 1;
        }
    }

    public static byte[] write(List<String> headers, List<List<String>> rows) {
        List<Page> pages = new ArrayList<Page>();
        pages.add(new Page("Equipment", headers, rows));
        return write(pages);
    }

    public static byte[] write(List<Page> pages) {
        List<Page> book = pages == null || pages.isEmpty() ? new ArrayList<Page>() : pages;
        if (book.isEmpty()) {
            book.add(new Page("Equipment", new ArrayList<String>(), new ArrayList<List<String>>()));
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream(bytes);
            Set<String> used = new HashSet<String>();
            List<String> names = new ArrayList<String>();
            for (Page page : book) {
                names.add(uniqueTitle(page == null ? "" : page.getName(), used));
            }
            List<SheetPicture> pictures = new ArrayList<SheetPicture>();
            int chartFile = 1;
            for (Page page : book) {
                SheetPicture picture = new SheetPicture();
                if (page != null && page.getCharts() != null) {
                    for (Chart chart : page.getCharts()) {
                        picture.charts.add(chart);
                        picture.files.add(Integer.valueOf(chartFile));
                        chartFile++;
                    }
                }
                pictures.add(picture.charts.isEmpty() ? null : picture);
            }
            put(zip, "[Content_Types].xml", contentTypes(book.size(), pictures));
            put(zip, "_rels/.rels", rootRels());
            put(zip, "xl/workbook.xml", workbook(names));
            put(zip, "xl/_rels/workbook.xml.rels", workbookRels(book.size()));
            put(zip, "xl/styles.xml", styles());
            for (int i = 0; i < book.size(); i++) {
                Page page = book.get(i);
                List<String> headers = page == null ? null : page.getHeaders();
                List<List<String>> rows = page == null ? null : page.getRows();
                SheetPicture picture = pictures.get(i);
                int number = i + 1;
                put(zip, "xl/worksheets/sheet" + number + ".xml", sheet(headers, rows, picture != null));
                if (picture == null) {
                    continue;
                }
                put(zip, "xl/worksheets/_rels/sheet" + number + ".xml.rels", sheetRels(number));
                put(zip, "xl/drawings/drawing" + number + ".xml", drawing(picture.charts));
                put(zip, "xl/drawings/_rels/drawing" + number + ".xml.rels", drawingRels(picture.files));
                for (int c = 0; c < picture.charts.size(); c++) {
                    put(zip, "xl/charts/chart" + picture.files.get(c) + ".xml", chartXml(picture.charts.get(c), names.get(i), rows));
                }
            }
            zip.close();
            return bytes.toByteArray();
        } catch (AssetException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AssetException(500, "asset-tree.error.unexpected");
        }
    }

    /**
     * A title Excel accepts as a sheet name: at most 31 characters, without
     * the characters a worksheet name cannot contain.
     */
    public static String sheetTitle(String raw) {
        String cleaned = sanitize(raw);
        return cleaned.isEmpty() ? "Sheet" : cleaned;
    }

    public static EquipmentSheet.Sheet read(byte[] bytes) {
        return readAll(bytes).get(0);
    }

    public static List<EquipmentSheet.Sheet> readAll(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            throw new AssetException(400, "asset-tree.error.import.workbook");
        }
        if ((bytes[0] & 0xff) == 0xd0 && (bytes[1] & 0xff) == 0xcf) {
            throw new AssetException(400, "asset-tree.error.import.workbook");
        }
        if (bytes[0] != 'P' || bytes[1] != 'K') {
            throw new AssetException(400, "asset-tree.error.import.workbook");
        }
        Map<String, byte[]> entries = unzip(bytes);
        byte[] shared = entries.get("xl/sharedStrings.xml");
        List<String> strings = shared == null ? new ArrayList<String>() : sharedStrings(shared);
        boolean[] dates = dateStyles(entries.get("xl/styles.xml"));
        List<SheetRef> refs = sheetRefs(entries);
        List<EquipmentSheet.Sheet> sheets = new ArrayList<EquipmentSheet.Sheet>();
        for (SheetRef ref : refs) {
            byte[] xml = entries.get(ref.path);
            if (xml == null) {
                continue;
            }
            EquipmentSheet.Sheet parsed = rows(xml, strings, dates);
            sheets.add(new EquipmentSheet.Sheet(ref.name, parsed.getHeaders(), parsed.getRecords()));
        }
        if (sheets.isEmpty()) {
            throw new AssetException(400, "asset-tree.error.import.workbook");
        }
        return sheets;
    }

    private static String uniqueTitle(String raw, Set<String> used) {
        String base = sheetTitle(raw);
        String candidate = base;
        int number = 2;
        while (!used.add(candidate.toLowerCase(Locale.ROOT))) {
            String suffix = " (" + number + ")";
            int room = MAX_NAME - suffix.length();
            String stem = base.length() > room ? base.substring(0, Math.max(0, room)).trim() : base;
            if (stem.isEmpty()) {
                stem = "Sheet";
            }
            candidate = stem + suffix;
            number++;
        }
        return candidate;
    }

    private static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length() && out.length() < MAX_NAME; i++) {
            char ch = raw.charAt(i);
            if (ch == '\\' || ch == '/' || ch == '?' || ch == '*' || ch == '[' || ch == ']' || ch == ':' || ch < 0x20) {
                if (out.length() > 0 && out.charAt(out.length() - 1) != ' ') {
                    out.append(' ');
                }
                continue;
            }
            out.append(ch);
        }
        String text = out.toString().trim();
        while (text.startsWith("'")) {
            text = text.substring(1).trim();
        }
        while (text.endsWith("'")) {
            text = text.substring(0, text.length() - 1).trim();
        }
        if (text.length() > MAX_NAME) {
            text = text.substring(0, MAX_NAME).trim();
        }
        return text;
    }

    private static void put(ZipOutputStream zip, String name, String xml) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(xml.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String contentTypes(int sheets, List<SheetPicture> pictures) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        xml.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">");
        xml.append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>");
        xml.append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>");
        xml.append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
        for (int i = 1; i <= sheets; i++) {
            xml.append("<Override PartName=\"/xl/worksheets/sheet").append(i);
            xml.append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        for (int i = 0; i < pictures.size(); i++) {
            SheetPicture picture = pictures.get(i);
            if (picture == null) {
                continue;
            }
            int number = i + 1;
            xml.append("<Override PartName=\"/xl/drawings/drawing").append(number);
            xml.append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.drawing+xml\"/>");
            for (Integer file : picture.files) {
                xml.append("<Override PartName=\"/xl/charts/chart").append(file);
                xml.append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.drawingml.chart+xml\"/>");
            }
        }
        xml.append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
        xml.append("</Types>");
        return xml.toString();
    }

    private static String rootRels() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private static String workbook(List<String> names) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        xml.append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");
        xml.append("<sheets>");
        for (int i = 0; i < names.size(); i++) {
            xml.append("<sheet name=\"").append(escapeAttr(names.get(i)));
            xml.append("\" sheetId=\"").append(i + 1).append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        xml.append("</sheets></workbook>");
        return xml.toString();
    }

    private static String workbookRels(int count) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        xml.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 1; i <= count; i++) {
            xml.append("<Relationship Id=\"rId").append(i);
            xml.append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet");
            xml.append(i).append(".xml\"/>");
        }
        xml.append("<Relationship Id=\"rId").append(count + 1);
        xml.append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
        xml.append("</Relationships>");
        return xml.toString();
    }

    private static String escapeAttr(String value) {
        return escape(value).replace("\"", "&quot;");
    }

    private static String styles() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<fonts count=\"1\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>"
                + "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills>"
                + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + "<cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
                + "<xf numFmtId=\"2\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/></cellXfs>"
                + "</styleSheet>";
    }

    private static String sheet(List<String> headers, List<List<String>> rows, boolean drawing) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        xml.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"");
        if (drawing) {
            xml.append(" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"");
        }
        xml.append("><sheetData>");
        writeRow(xml, 1, headers);
        if (rows != null) {
            int number = 2;
            for (List<String> row : rows) {
                writeRow(xml, number, row);
                number++;
            }
        }
        xml.append("</sheetData>");
        if (drawing) {
            xml.append("<drawing r:id=\"rId1\"/>");
        }
        xml.append("</worksheet>");
        return xml.toString();
    }

    private static void writeRow(StringBuilder xml, int number, List<String> cells) {
        xml.append("<row r=\"").append(number).append("\">");
        if (cells != null) {
            for (int i = 0; i < cells.size() && i < MAX_COLUMNS; i++) {
                String raw = cells.get(i);
                if (raw != null && raw.length() > 3 && raw.charAt(0) == '\u0001' && raw.charAt(2) == ':') {
                    String numberText = raw.substring(3);
                    if (plainNumeric(numberText)) {
                        char kind = raw.charAt(1);
                        xml.append("<c r=\"").append(columnName(i)).append(number).append("\"");
                        if (kind == 'd') {
                            xml.append(" s=\"1\"");
                        }
                        xml.append("><v>").append(numberText).append("</v></c>");
                        continue;
                    }
                }
                xml.append("<c r=\"").append(columnName(i)).append(number).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">");
                xml.append(escape(raw));
                xml.append("</t></is></c>");
            }
        }
        xml.append("</row>");
    }

    private static boolean plainNumeric(String raw) {
        if (raw == null || raw.isEmpty()) {
            return false;
        }
        int start = raw.charAt(0) == '-' ? 1 : 0;
        if (start >= raw.length()) {
            return false;
        }
        boolean dot = false;
        for (int i = start; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '.') {
                if (dot) {
                    return false;
                }
                dot = true;
                continue;
            }
            if (ch < '0' || ch > '9') {
                return false;
            }
        }
        return true;
    }

    private static String columnName(int index) {
        StringBuilder name = new StringBuilder();
        int n = index + 1;
        while (n > 0) {
            int rem = (n - 1) % 26;
            name.insert(0, (char) ('A' + rem));
            n = (n - 1) / 26;
        }
        return name.toString();
    }

    private static String escape(String value) {
        String text = value == null ? "" : value;
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '&') {
                out.append("&amp;");
            } else if (ch == '<') {
                out.append("&lt;");
            } else if (ch == '>') {
                out.append("&gt;");
            } else if (ch == '\t' || ch == '\n' || ch == '\r' || ch >= 0x20) {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static String sheetRels(int number) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing\" Target=\"../drawings/drawing"
                + number + ".xml\"/>"
                + "</Relationships>";
    }

    private static String drawingRels(List<Integer> files) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        xml.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 0; i < files.size(); i++) {
            xml.append("<Relationship Id=\"rId").append(i + 1);
            xml.append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/chart\" Target=\"../charts/chart");
            xml.append(files.get(i)).append(".xml\"/>");
        }
        xml.append("</Relationships>");
        return xml.toString();
    }

    private static String drawing(List<Chart> charts) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        xml.append("<xdr:wsDr xmlns:xdr=\"http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing\" xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");
        int row = 0;
        for (int i = 0; i < charts.size(); i++) {
            int height = Math.max(16, Math.min(36, charts.get(i).pointCount() + 4));
            xml.append("<xdr:twoCellAnchor>");
            xml.append("<xdr:from><xdr:col>4</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>").append(row);
            xml.append("</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:from>");
            xml.append("<xdr:to><xdr:col>15</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>").append(row + height);
            xml.append("</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:to>");
            xml.append("<xdr:graphicFrame macro=\"\">");
            xml.append("<xdr:nvGraphicFramePr><xdr:cNvPr id=\"").append(i + 2);
            xml.append("\" name=\"Chart ").append(i + 1).append("\"/>");
            xml.append("<xdr:cNvGraphicFramePr><a:graphicFrameLocks noGrp=\"1\"/></xdr:cNvGraphicFramePr></xdr:nvGraphicFramePr>");
            xml.append("<xdr:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/></xdr:xfrm>");
            xml.append("<a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/chart\">");
            xml.append("<c:chart xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" r:id=\"rId");
            xml.append(i + 1).append("\"/>");
            xml.append("</a:graphicData></a:graphic></xdr:graphicFrame><xdr:clientData/>");
            xml.append("</xdr:twoCellAnchor>");
            row += height + 1;
        }
        xml.append("</xdr:wsDr>");
        return xml.toString();
    }

    private static String chartXml(Chart chart, String sheetName, List<List<String>> rows) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        xml.append("<c:chartSpace xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\" xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">");
        xml.append("<c:chart>");
        String title = chart.title.isEmpty() ? chart.series : chart.title;
        xml.append("<c:title><c:tx><c:rich><a:bodyPr/><a:lstStyle/><a:p><a:pPr><a:defRPr/></a:pPr><a:r><a:rPr lang=\"ru-RU\" sz=\"1200\"/><a:t>");
        xml.append(escape(title));
        xml.append("</a:t></a:r></a:p></c:rich></c:tx><c:overlay val=\"0\"/></c:title>");
        xml.append("<c:plotArea><c:layout/>");
        plot(xml, chart, sheetName, rows);
        xml.append("</c:plotArea><c:legend><c:legendPos val=\"b\"/><c:overlay val=\"0\"/></c:legend><c:plotVisOnly val=\"1\"/></c:chart>");
        xml.append("<c:printSettings><c:headerFooter/><c:pageMargins b=\"0.75\" l=\"0.7\" r=\"0.7\" t=\"0.75\" header=\"0.3\" footer=\"0.3\"/><c:pageSetup/></c:printSettings>");
        xml.append("</c:chartSpace>");
        return xml.toString();
    }

    private static void plot(StringBuilder xml, Chart chart, String sheetName, List<List<String>> rows) {
        String plot = chart.plot;
        if ("pie".equals(plot)) {
            xml.append("<c:pieChart><c:varyColors val=\"1\"/>");
            sheetSeries(xml, chart, sheetName, rows, false);
            percentLabels(xml);
            xml.append("</c:pieChart>");
            return;
        }
        if ("doughnut".equals(plot)) {
            xml.append("<c:doughnutChart><c:varyColors val=\"1\"/>");
            sheetSeries(xml, chart, sheetName, rows, false);
            percentLabels(xml);
            xml.append("<c:holeSize val=\"60\"/></c:doughnutChart>");
            return;
        }
        if ("line".equals(plot)) {
            xml.append("<c:lineChart><c:grouping val=\"standard\"/><c:varyColors val=\"0\"/>");
            sheetSeries(xml, chart, sheetName, rows, true);
            xml.append("<c:marker val=\"1\"/><c:smooth val=\"0\"/><c:axId val=\"1\"/><c:axId val=\"2\"/></c:lineChart>");
            categoryAxes(xml, false);
            return;
        }
        if ("area".equals(plot)) {
            xml.append("<c:areaChart><c:grouping val=\"standard\"/><c:varyColors val=\"0\"/>");
            sheetSeries(xml, chart, sheetName, rows, false);
            xml.append("<c:axId val=\"1\"/><c:axId val=\"2\"/></c:areaChart>");
            categoryAxes(xml, false);
            return;
        }
        if ("radar".equals(plot)) {
            xml.append("<c:radarChart><c:radarStyle val=\"marker\"/><c:varyColors val=\"0\"/>");
            sheetSeries(xml, chart, sheetName, rows, true);
            xml.append("<c:axId val=\"1\"/><c:axId val=\"2\"/></c:radarChart>");
            categoryAxes(xml, false);
            return;
        }
        if ("scatter".equals(plot)) {
            xml.append("<c:scatterChart><c:scatterStyle val=\"lineMarker\"/><c:varyColors val=\"0\"/>");
            scatterSeries(xml, chart, sheetName, rows);
            xml.append("<c:axId val=\"1\"/><c:axId val=\"2\"/></c:scatterChart>");
            valueAxes(xml);
            return;
        }
        boolean horizontal = "bar".equals(plot);
        xml.append("<c:barChart><c:barDir val=\"").append(horizontal ? "bar" : "col").append("\"/>");
        xml.append("<c:grouping val=\"").append(chart.stacked ? "stacked" : "clustered").append("\"/><c:varyColors val=\"1\"/>");
        sheetSeries(xml, chart, sheetName, rows, false);
        xml.append("<c:gapWidth val=\"").append(chart.histogram ? "0" : "80").append("\"/>");
        if (chart.stacked) {
            xml.append("<c:overlap val=\"100\"/>");
        }
        xml.append("<c:axId val=\"1\"/><c:axId val=\"2\"/></c:barChart>");
        categoryAxes(xml, horizontal);
    }

    private static void sheetSeries(StringBuilder xml, Chart chart, String sheetName, List<List<String>> rows, boolean marker) {
        if (chart.labels != null) {
            if (chart.stacked && chart.bases != null) {
                oneSeries(xml, 0, chart.baseSeries, null, chart.labels, null, chart.bases, false, true);
                oneSeries(xml, 1, chart.series, null, chart.labels, null, chart.values, marker, false);
                return;
            }
            oneSeries(xml, 0, chart.series, null, chart.labels, null, chart.values, marker, false);
            return;
        }
        List<String> cats = new ArrayList<String>();
        List<String> vals = new ArrayList<String>();
        for (int row = chart.firstRow; row <= chart.lastRow; row++) {
            cats.add(cellText(rows, chart.catColumn, row));
            vals.add(numericText(cellText(rows, chart.valueColumn, row)));
        }
        String catFormula = range(sheetName, chart.catColumn, chart.firstRow, chart.lastRow);
        String valFormula = range(sheetName, chart.valueColumn, chart.firstRow, chart.lastRow);
        oneSeries(xml, 0, chart.series, catFormula, cats, valFormula, vals, marker, false);
    }

    private static void scatterSeries(StringBuilder xml, Chart chart, String sheetName, List<List<String>> rows) {
        int count = chart.pointCount();
        List<String> xs = new ArrayList<String>();
        List<String> ys = new ArrayList<String>();
        String xFormula = null;
        for (int i = 0; i < count; i++) {
            int row = chart.firstRow + i;
            if (chart.indexX) {
                xs.add(Integer.toString(i + 1));
            } else {
                xs.add(numericText(cellText(rows, chart.xColumn, row)));
            }
            ys.add(numericText(cellText(rows, chart.valueColumn, row)));
        }
        if (!chart.indexX) {
            xFormula = range(sheetName, chart.xColumn, chart.firstRow, chart.lastRow);
        }
        String yFormula = range(sheetName, chart.valueColumn, chart.firstRow, chart.lastRow);
        xml.append("<c:ser><c:idx val=\"0\"/><c:order val=\"0\"/><c:tx><c:v>");
        xml.append(escape(chart.series));
        xml.append("</c:v></c:tx><c:marker><c:symbol val=\"circle\"/><c:size val=\"7\"/></c:marker>");
        xml.append("<c:xVal>");
        numberPoints(xml, xFormula, xs);
        xml.append("</c:xVal><c:yVal>");
        numberPoints(xml, yFormula, ys);
        xml.append("</c:yVal></c:ser>");
    }

    private static void oneSeries(StringBuilder xml, int index, String name, String catFormula, List<String> cats,
            String valFormula, List<String> vals, boolean marker, boolean hidden) {
        xml.append("<c:ser><c:idx val=\"").append(index).append("\"/><c:order val=\"").append(index).append("\"/>");
        xml.append("<c:tx><c:v>").append(escape(name)).append("</c:v></c:tx>");
        if (marker) {
            xml.append("<c:marker><c:symbol val=\"circle\"/><c:size val=\"6\"/></c:marker>");
        }
        if (hidden) {
            xml.append("<c:spPr><a:noFill/><a:ln><a:noFill/></a:ln></c:spPr>");
        }
        xml.append("<c:cat>");
        textPoints(xml, catFormula, cats);
        xml.append("</c:cat><c:val>");
        numberPoints(xml, valFormula, vals);
        xml.append("</c:val></c:ser>");
    }

    private static void textPoints(StringBuilder xml, String formula, List<String> points) {
        if (formula == null) {
            xml.append("<c:strLit>");
            pointList(xml, points);
            xml.append("</c:strLit>");
            return;
        }
        xml.append("<c:strRef><c:f>").append(escape(formula)).append("</c:f><c:strCache>");
        pointList(xml, points);
        xml.append("</c:strCache></c:strRef>");
    }

    private static void numberPoints(StringBuilder xml, String formula, List<String> points) {
        if (formula == null) {
            xml.append("<c:numLit><c:formatCode>General</c:formatCode>");
            pointList(xml, points);
            xml.append("</c:numLit>");
            return;
        }
        xml.append("<c:numRef><c:f>").append(escape(formula)).append("</c:f><c:numCache><c:formatCode>General</c:formatCode>");
        pointList(xml, points);
        xml.append("</c:numCache></c:numRef>");
    }

    private static void pointList(StringBuilder xml, List<String> points) {
        xml.append("<c:ptCount val=\"").append(points.size()).append("\"/>");
        for (int i = 0; i < points.size(); i++) {
            xml.append("<c:pt idx=\"").append(i).append("\"><c:v>");
            xml.append(escape(points.get(i)));
            xml.append("</c:v></c:pt>");
        }
    }

    private static void percentLabels(StringBuilder xml) {
        xml.append("<c:dLbls><c:showLegendKey val=\"0\"/><c:showVal val=\"0\"/><c:showCatName val=\"0\"/><c:showSerName val=\"0\"/><c:showPercent val=\"1\"/><c:showLeaderLines val=\"1\"/></c:dLbls>");
    }

    private static void categoryAxes(StringBuilder xml, boolean horizontal) {
        String catPos = horizontal ? "l" : "b";
        String valPos = horizontal ? "b" : "l";
        xml.append("<c:catAx><c:axId val=\"1\"/><c:scaling><c:orientation val=\"minMax\"/></c:scaling><c:delete val=\"0\"/><c:axPos val=\"");
        xml.append(catPos);
        xml.append("\"/><c:majorTickMark val=\"out\"/><c:minorTickMark val=\"none\"/><c:tickLblPos val=\"nextTo\"/><c:crossAx val=\"2\"/><c:crosses val=\"autoZero\"/><c:auto val=\"1\"/><c:lblAlgn val=\"ctr\"/><c:lblOffset val=\"100\"/></c:catAx>");
        xml.append("<c:valAx><c:axId val=\"2\"/><c:scaling><c:orientation val=\"minMax\"/></c:scaling><c:delete val=\"0\"/><c:axPos val=\"");
        xml.append(valPos);
        xml.append("\"/><c:majorGridlines/><c:majorTickMark val=\"out\"/><c:minorTickMark val=\"none\"/><c:tickLblPos val=\"nextTo\"/><c:crossAx val=\"1\"/><c:crosses val=\"autoZero\"/><c:crossBetween val=\"between\"/></c:valAx>");
    }

    private static void valueAxes(StringBuilder xml) {
        xml.append("<c:valAx><c:axId val=\"1\"/><c:scaling><c:orientation val=\"minMax\"/></c:scaling><c:delete val=\"0\"/><c:axPos val=\"b\"/><c:majorTickMark val=\"out\"/><c:minorTickMark val=\"none\"/><c:tickLblPos val=\"nextTo\"/><c:crossAx val=\"2\"/><c:crosses val=\"autoZero\"/><c:crossBetween val=\"midCat\"/></c:valAx>");
        xml.append("<c:valAx><c:axId val=\"2\"/><c:scaling><c:orientation val=\"minMax\"/></c:scaling><c:delete val=\"0\"/><c:axPos val=\"l\"/><c:majorGridlines/><c:majorTickMark val=\"out\"/><c:minorTickMark val=\"none\"/><c:tickLblPos val=\"nextTo\"/><c:crossAx val=\"1\"/><c:crosses val=\"autoZero\"/><c:crossBetween val=\"midCat\"/></c:valAx>");
    }

    private static String range(String sheetName, int column, int from, int to) {
        String col = columnName(column);
        return formulaSheet(sheetName) + "!$" + col + "$" + from + ":$" + col + "$" + to;
    }

    private static String formulaSheet(String sheetName) {
        String name = sheetName == null ? "Sheet" : sheetName;
        return "'" + name.replace("'", "''") + "'";
    }

    private static String cellText(List<List<String>> rows, int column, int excelRow) {
        int index = excelRow - 2;
        if (rows == null || index < 0 || index >= rows.size() || column < 0) {
            return "";
        }
        List<String> row = rows.get(index);
        if (row == null || column >= row.size() || row.get(column) == null) {
            return "";
        }
        String raw = row.get(column);
        if (raw.length() > 3 && raw.charAt(0) == '\u0001' && raw.charAt(2) == ':') {
            return raw.substring(3);
        }
        return raw;
    }

    private static String numericText(String raw) {
        return plainNumeric(raw) ? raw : "0";
    }

    private static Map<String, byte[]> unzip(byte[] bytes) {
        Map<String, byte[]> entries = new HashMap<String, byte[]>();
        try {
            ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes));
            ZipEntry entry;
            long total = 0;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int read;
                while ((read = zip.read(buffer)) >= 0) {
                    total += read;
                    if (total > MAX_BYTES) {
                        zip.close();
                        throw new AssetException(400, "asset-tree.error.import.workbook");
                    }
                    out.write(buffer, 0, read);
                }
                String name = entry.getName().replace('\\', '/');
                while (name.startsWith("/")) {
                    name = name.substring(1);
                }
                entries.put(name, out.toByteArray());
            }
            zip.close();
            return entries;
        } catch (AssetException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AssetException(400, "asset-tree.error.import.workbook");
        }
    }

    private static final class SheetRef {
        private final String name;
        private final String path;

        private SheetRef(String name, String path) {
            this.name = name == null ? "" : name;
            this.path = path;
        }
    }

    private static List<SheetRef> sheetRefs(Map<String, byte[]> entries) {
        List<SheetRef> listed = listedSheets(entries.get("xl/workbook.xml"), entries.get("xl/_rels/workbook.xml.rels"));
        List<SheetRef> present = new ArrayList<SheetRef>();
        for (SheetRef ref : listed) {
            if (entries.containsKey(ref.path)) {
                present.add(ref);
            }
        }
        if (!present.isEmpty()) {
            return present;
        }
        if (entries.containsKey("xl/worksheets/sheet1.xml")) {
            present.add(new SheetRef("", "xl/worksheets/sheet1.xml"));
            return present;
        }
        for (String key : entries.keySet()) {
            if (key.startsWith("xl/worksheets/") && key.endsWith(".xml")) {
                present.add(new SheetRef("", key));
                return present;
            }
        }
        return present;
    }

    private static List<SheetRef> listedSheets(byte[] workbook, byte[] rels) {
        List<SheetRef> refs = new ArrayList<SheetRef>();
        if (workbook == null || rels == null) {
            return refs;
        }
        List<String> names = new ArrayList<String>();
        List<String> relations = new ArrayList<String>();
        XmlCursor reader = cursor(workbook);
        try {
            while (reader.hasNext()) {
                if (reader.next() == XmlCursor.START && "sheet".equals(reader.localName())) {
                    names.add(reader.attr("name"));
                    relations.add(reader.attr("id"));
                }
            }
        } catch (AssetException ex) {
            throw ex;
        } catch (Exception ex) {
            return refs;
        }
        Map<String, String> targets = new HashMap<String, String>();
        reader = cursor(rels);
        try {
            while (reader.hasNext()) {
                if (reader.next() == XmlCursor.START && "Relationship".equals(reader.localName())) {
                    targets.put(reader.attr("Id"), resolve(reader.attr("Target")));
                }
            }
        } catch (AssetException ex) {
            throw ex;
        } catch (Exception ex) {
            return new ArrayList<SheetRef>();
        }
        for (int i = 0; i < relations.size(); i++) {
            String path = targets.get(relations.get(i));
            if (path != null) {
                refs.add(new SheetRef(names.get(i), path));
            }
        }
        return refs;
    }

    private static String resolve(String target) {
        if (target == null || target.isEmpty()) {
            return null;
        }
        String path = target.replace('\\', '/');
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.startsWith("xl/")) {
            return path;
        }
        return "xl/" + path;
    }

    private static List<String> sharedStrings(byte[] xml) {
        List<String> strings = new ArrayList<String>();
        XmlCursor reader = cursor(xml);
        StringBuilder current = null;
        boolean inText = false;
        int skip = 0;
        try {
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XmlCursor.START) {
                    String name = reader.localName();
                    if ("si".equals(name)) {
                        current = new StringBuilder();
                    } else if ("rPh".equals(name) || "phoneticPr".equals(name)) {
                        skip++;
                    } else if ("t".equals(name) && current != null && skip == 0) {
                        inText = true;
                    }
                } else if (event == XmlCursor.TEXT && inText && current != null) {
                    current.append(reader.text());
                } else if (event == XmlCursor.END) {
                    String name = reader.localName();
                    if ("t".equals(name)) {
                        inText = false;
                    } else if ("rPh".equals(name) || "phoneticPr".equals(name)) {
                        if (skip > 0) {
                            skip--;
                        }
                    } else if ("si".equals(name)) {
                        strings.add(current == null ? "" : current.toString());
                        current = null;
                        inText = false;
                    }
                }
            }
        } catch (AssetException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AssetException(400, "asset-tree.error.import.workbook");
        }
        return strings;
    }

    private static boolean[] dateStyles(byte[] xml) {
        if (xml == null) {
            return new boolean[0];
        }
        List<Integer> formats = new ArrayList<Integer>();
        Map<Integer, String> custom = new HashMap<Integer, String>();
        boolean inXfs = false;
        XmlCursor reader = cursor(xml);
        try {
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XmlCursor.START) {
                    String name = reader.localName();
                    if ("numFmt".equals(name)) {
                        custom.put(Integer.valueOf(number(reader.attr("numFmtId"))), reader.attr("formatCode"));
                    } else if ("cellXfs".equals(name)) {
                        inXfs = true;
                    } else if (inXfs && "xf".equals(name)) {
                        formats.add(Integer.valueOf(number(reader.attr("numFmtId"))));
                    }
                } else if (event == XmlCursor.END && "cellXfs".equals(reader.localName())) {
                    inXfs = false;
                }
            }
        } catch (AssetException ex) {
            throw ex;
        } catch (Exception ex) {
            return new boolean[0];
        }
        boolean[] dates = new boolean[formats.size()];
        for (int i = 0; i < formats.size(); i++) {
            int format = formats.get(i).intValue();
            dates[i] = internalDate(format) || dateCode(custom.get(Integer.valueOf(format)));
        }
        return dates;
    }

    private static boolean internalDate(int format) {
        return (format >= 14 && format <= 22) || (format >= 27 && format <= 36)
                || (format >= 45 && format <= 47) || (format >= 50 && format <= 58);
    }

    private static boolean dateCode(String format) {
        if (format == null || format.isEmpty()) {
            return false;
        }
        StringBuilder plain = new StringBuilder();
        for (int i = 0; i < format.length(); i++) {
            char ch = format.charAt(i);
            if (ch == '\\' && i + 1 < format.length()) {
                i++;
                continue;
            }
            if (ch == '"') {
                i++;
                while (i < format.length() && format.charAt(i) != '"') {
                    i++;
                }
                continue;
            }
            if (ch == '[') {
                while (i < format.length() && format.charAt(i) != ']') {
                    i++;
                }
                continue;
            }
            plain.append(ch);
        }
        String code = plain.toString().toLowerCase(Locale.ROOT);
        if (code.indexOf('y') >= 0 || code.indexOf('d') >= 0 || code.indexOf('h') >= 0 || code.indexOf('s') >= 0) {
            return true;
        }
        for (int i = 0; i < code.length(); i++) {
            if (code.charAt(i) != 'm') {
                continue;
            }
            boolean minute = false;
            for (int left = i - 1; left >= 0; left--) {
                char prev = code.charAt(left);
                if (prev == 'h') {
                    minute = true;
                    break;
                }
                if (prev != 'm' && prev != ':') {
                    break;
                }
            }
            for (int right = i + 1; right < code.length(); right++) {
                char next = code.charAt(right);
                if (next == 's') {
                    minute = true;
                    break;
                }
                if (next != 'm' && next != ':') {
                    break;
                }
            }
            if (!minute) {
                return true;
            }
        }
        return false;
    }

    private static EquipmentSheet.Sheet rows(byte[] xml, List<String> strings, boolean[] dates) {
        List<String> headers = new ArrayList<String>();
        List<EquipmentSheet.Record> records = new ArrayList<EquipmentSheet.Record>();
        XmlCursor reader = cursor(xml);
        try {
            while (reader.hasNext()) {
                if (reader.next() == XmlCursor.START && "row".equals(reader.localName())) {
                    int rowNumber = number(reader.attr("r"));
                    List<String> cells = readRow(reader, strings, dates);
                    if (blank(cells)) {
                        continue;
                    }
                    if (headers.isEmpty()) {
                        for (String cell : cells) {
                            headers.add(cell == null ? "" : cell.trim());
                        }
                    } else {
                        records.add(new EquipmentSheet.Record(rowNumber > 0 ? rowNumber : records.size() + 2, cells));
                    }
                }
            }
        } catch (AssetException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AssetException(400, "asset-tree.error.import.workbook");
        }
        return new EquipmentSheet.Sheet(headers, records);
    }

    private static List<String> readRow(XmlCursor reader, List<String> strings, boolean[] dates) {
        String[] cells = new String[0];
        int next = 0;
        int depth = 1;
        while (depth > 0 && reader.hasNext()) {
            int event = reader.next();
            if (event == XmlCursor.START) {
                if (depth == 1 && "c".equals(reader.localName())) {
                    int column = columnIndex(reader.attr("r"));
                    if (column < 0) {
                        column = next;
                    }
                    String type = reader.attr("t");
                    String styleAttr = reader.attr("s");
                    int style = styleAttr == null ? -1 : number(styleAttr);
                    String value = readCell(reader, type, style, strings, dates);
                    if (column >= 0 && column < MAX_COLUMNS) {
                        if (column >= cells.length) {
                            String[] grown = new String[column + 1];
                            System.arraycopy(cells, 0, grown, 0, cells.length);
                            cells = grown;
                        }
                        cells[column] = value;
                        next = column + 1;
                    }
                } else {
                    depth++;
                }
            } else if (event == XmlCursor.END) {
                depth--;
            }
        }
        List<String> row = new ArrayList<String>();
        for (int i = 0; i < cells.length; i++) {
            row.add(cells[i] == null ? "" : cells[i]);
        }
        return row;
    }

    private static String readCell(XmlCursor reader, String type, int style, List<String> strings, boolean[] dates) {
        StringBuilder text = new StringBuilder();
        StringBuilder value = new StringBuilder();
        boolean inText = false;
        boolean inValue = false;
        int depth = 1;
        while (depth > 0 && reader.hasNext()) {
            int event = reader.next();
            if (event == XmlCursor.START) {
                depth++;
                String name = reader.localName();
                if ("t".equals(name)) {
                    inText = true;
                } else if ("v".equals(name)) {
                    inValue = true;
                }
            } else if (event == XmlCursor.TEXT) {
                if (inText) {
                    text.append(reader.text());
                } else if (inValue) {
                    value.append(reader.text());
                }
            } else if (event == XmlCursor.END) {
                String name = reader.localName();
                if ("t".equals(name)) {
                    inText = false;
                } else if ("v".equals(name)) {
                    inValue = false;
                }
                depth--;
            }
        }
        if ("inlineStr".equals(type) || text.length() > 0 && "s".equals(type) == false && value.length() == 0) {
            return text.toString();
        }
        String raw = value.toString().trim();
        if ("s".equals(type)) {
            int index = number(raw);
            return index >= 0 && index < strings.size() ? strings.get(index) : "";
        }
        if ("b".equals(type)) {
            return "1".equals(raw) ? "true" : "false";
        }
        if ("str".equals(type)) {
            return raw;
        }
        if (raw.isEmpty()) {
            return text.toString();
        }
        boolean date = style >= 0 && style < dates.length && dates[style];
        return date ? excelDate(raw) : plainNumber(raw);
    }

    private static String excelDate(String raw) {
        try {
            double serial = Double.parseDouble(raw);
            long days = (long) Math.floor(serial + 1e-8);
            if (days <= 0) {
                return plainNumber(raw);
            }
            return LocalDate.ofEpochDay(days - EXCEL_EPOCH).toString();
        } catch (RuntimeException ex) {
            return raw;
        }
    }

    private static String plainNumber(String raw) {
        try {
            double value = Double.parseDouble(raw);
            if (Double.isInfinite(value) || Double.isNaN(value)) {
                return raw;
            }
            if (Math.abs(value) < 1e15 && Math.abs(value - Math.rint(value)) < 1e-6) {
                return Long.toString((long) Math.rint(value));
            }
            return raw;
        } catch (NumberFormatException ex) {
            return raw;
        }
    }

    private static boolean blank(List<String> cells) {
        for (String cell : cells) {
            if (cell != null && !cell.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static int columnIndex(String ref) {
        if (ref == null) {
            return -1;
        }
        int column = 0;
        int used = 0;
        for (int i = 0; i < ref.length(); i++) {
            char ch = ref.charAt(i);
            if (ch >= 'A' && ch <= 'Z') {
                column = column * 26 + (ch - 'A' + 1);
                used++;
            } else if (ch >= 'a' && ch <= 'z') {
                column = column * 26 + (ch - 'a' + 1);
                used++;
            } else {
                break;
            }
        }
        return used == 0 ? -1 : column - 1;
    }

    private static int number(String raw) {
        if (raw == null || raw.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static XmlCursor cursor(byte[] xml) {
        return new XmlCursor(xml);
    }

    /**
     * Pull reader for the small XML subset stored in an xlsx package.
     * A document type is rejected so the book cannot pull in external files.
     */
    private static final class XmlCursor {
        static final int START = 1;
        static final int END = 2;
        static final int TEXT = 4;

        private final String xml;
        private int index;
        private String name = "";
        private String text = "";
        private final Map<String, String> attributes = new HashMap<String, String>();
        private boolean closeAfterStart;

        private XmlCursor(byte[] bytes) {
            String decoded = new String(bytes, StandardCharsets.UTF_8);
            if (decoded.startsWith("\uFEFF")) {
                decoded = decoded.substring(1);
            }
            this.xml = decoded;
        }

        private boolean hasNext() {
            if (closeAfterStart) {
                return true;
            }
            int saved = index;
            skipMeta();
            boolean more = index < xml.length();
            index = saved;
            return more;
        }

        private int next() {
            if (closeAfterStart) {
                closeAfterStart = false;
                text = "";
                return END;
            }
            skipMeta();
            if (index >= xml.length()) {
                throw new AssetException(400, "asset-tree.error.import.workbook");
            }
            if (xml.charAt(index) != '<') {
                text = readCharacters('<');
                name = "";
                attributes.clear();
                return TEXT;
            }
            if (starts("</")) {
                index += 2;
                name = local(readName());
                skipSpace();
                expect('>');
                text = "";
                attributes.clear();
                return END;
            }
            if (starts("<![CDATA[")) {
                index += 9;
                int end = xml.indexOf("]]>", index);
                if (end < 0) {
                    throw new AssetException(400, "asset-tree.error.import.workbook");
                }
                text = xml.substring(index, end);
                index = end + 3;
                name = "";
                attributes.clear();
                return TEXT;
            }
            index++;
            name = local(readName());
            attributes.clear();
            boolean self = false;
            while (index < xml.length()) {
                skipSpace();
                if (index >= xml.length()) {
                    break;
                }
                char ch = xml.charAt(index);
                if (ch == '>') {
                    index++;
                    break;
                }
                if (ch == '/') {
                    self = true;
                    index++;
                    skipSpace();
                    expect('>');
                    break;
                }
                String attrName = local(readName());
                skipSpace();
                expect('=');
                skipSpace();
                attributes.put(attrName, readQuoted());
            }
            text = "";
            closeAfterStart = self;
            return START;
        }

        private String localName() {
            return name;
        }

        private String text() {
            return text;
        }

        private String attr(String local) {
            return attributes.get(local);
        }

        private void skipMeta() {
            while (index < xml.length()) {
                if (starts("<?")) {
                    int end = xml.indexOf("?>", index);
                    if (end < 0) {
                        throw new AssetException(400, "asset-tree.error.import.workbook");
                    }
                    index = end + 2;
                    continue;
                }
                if (starts("<!--")) {
                    int end = xml.indexOf("-->", index);
                    if (end < 0) {
                        throw new AssetException(400, "asset-tree.error.import.workbook");
                    }
                    index = end + 3;
                    continue;
                }
                if (starts("<!DOCTYPE") || starts("<!doctype") || starts("<!ENTITY") || starts("<!entity")) {
                    throw new AssetException(400, "asset-tree.error.import.workbook");
                }
                break;
            }
        }

        private String readName() {
            int start = index;
            while (index < xml.length()) {
                char ch = xml.charAt(index);
                if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' || ch == '=' || ch == '/' || ch == '>' || ch == '?') {
                    break;
                }
                index++;
            }
            if (start == index) {
                throw new AssetException(400, "asset-tree.error.import.workbook");
            }
            return xml.substring(start, index);
        }

        private String readQuoted() {
            if (index >= xml.length()) {
                throw new AssetException(400, "asset-tree.error.import.workbook");
            }
            char quote = xml.charAt(index);
            if (quote != '"' && quote != '\'') {
                throw new AssetException(400, "asset-tree.error.import.workbook");
            }
            index++;
            return readCharacters(quote);
        }

        private String readCharacters(char stop) {
            StringBuilder out = new StringBuilder();
            while (index < xml.length()) {
                char ch = xml.charAt(index);
                if (ch == stop) {
                    if (stop != '<') {
                        index++;
                    }
                    return out.toString();
                }
                if (ch == '&') {
                    out.append(readEntity());
                    continue;
                }
                out.append(ch);
                index++;
            }
            if (stop != '<') {
                throw new AssetException(400, "asset-tree.error.import.workbook");
            }
            return out.toString();
        }

        private String readEntity() {
            int start = index + 1;
            int end = xml.indexOf(';', start);
            if (end < 0 || end - start > 12) {
                throw new AssetException(400, "asset-tree.error.import.workbook");
            }
            String body = xml.substring(start, end);
            index = end + 1;
            if ("amp".equals(body)) {
                return "&";
            }
            if ("lt".equals(body)) {
                return "<";
            }
            if ("gt".equals(body)) {
                return ">";
            }
            if ("quot".equals(body)) {
                return "\"";
            }
            if ("apos".equals(body)) {
                return "'";
            }
            try {
                int code;
                if (body.startsWith("#x") || body.startsWith("#X")) {
                    code = Integer.parseInt(body.substring(2), 16);
                } else if (body.startsWith("#")) {
                    code = Integer.parseInt(body.substring(1));
                } else {
                    throw new AssetException(400, "asset-tree.error.import.workbook");
                }
                if (code < 0 || code > 0x10FFFF || (code >= 0xD800 && code <= 0xDFFF)) {
                    throw new AssetException(400, "asset-tree.error.import.workbook");
                }
                return new String(Character.toChars(code));
            } catch (NumberFormatException ex) {
                throw new AssetException(400, "asset-tree.error.import.workbook");
            }
        }

        private void skipSpace() {
            while (index < xml.length()) {
                char ch = xml.charAt(index);
                if (ch != ' ' && ch != '\t' && ch != '\n' && ch != '\r') {
                    break;
                }
                index++;
            }
        }

        private void expect(char ch) {
            if (index >= xml.length() || xml.charAt(index) != ch) {
                throw new AssetException(400, "asset-tree.error.import.workbook");
            }
            index++;
        }

        private boolean starts(String token) {
            return xml.startsWith(token, index);
        }

        private static String local(String qualified) {
            int colon = qualified.lastIndexOf(':');
            return colon < 0 ? qualified : qualified.substring(colon + 1);
        }
    }
}
