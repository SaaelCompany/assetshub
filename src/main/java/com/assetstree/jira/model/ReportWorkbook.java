package com.assetstree.jira.model;

import com.assetstree.jira.dto.ReportLineDto;
import com.assetstree.jira.dto.ReportViewDto;
import com.assetstree.jira.dto.ReportVennDto;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Chooses the picture that belongs on a report sheet. */
public final class ReportWorkbook {
    public static final int CATEGORY_LIMIT = 80;
    public static final int GANTT_LIMIT = 40;

    private ReportWorkbook() {
    }

    public static WorkbookSheet.Chart chart(ReportViewDto view, String countLabel, String moneyLabel, String startLabel, String spanLabel) {
        List<WorkbookSheet.Chart> pictures = charts(view, countLabel, moneyLabel, startLabel, spanLabel, null);
        if (!pictures.isEmpty()) {
            return pictures.get(0);
        }
        List<String> kinds = kindsOf(view);
        if (!kinds.isEmpty() && ("gantt".equals(kinds.get(0)) || "venn".equals(kinds.get(0)))) {
            return chartKind(view, "bar", countLabel, moneyLabel, startLabel, spanLabel, titleOf(view));
        }
        return null;
    }

    /** One picture for every chart marked on the report. */
    public static List<WorkbookSheet.Chart> charts(ReportViewDto view, String countLabel, String moneyLabel, String startLabel, String spanLabel, Map<String, String> captions) {
        List<WorkbookSheet.Chart> pictures = new ArrayList<WorkbookSheet.Chart>();
        if (view == null) {
            return pictures;
        }
        List<String> kinds = kindsOf(view);
        String reportTitle = titleOf(view);
        for (String kind : kinds) {
            String caption = captions == null ? null : captions.get(kind);
            String title = kinds.size() > 1 && caption != null && !caption.isEmpty() ? caption : reportTitle;
            WorkbookSheet.Chart picture = chartKind(view, kind, countLabel, moneyLabel, startLabel, spanLabel, title);
            if (picture != null && picture.usable()) {
                pictures.add(picture);
            }
        }
        return pictures;
    }

    private static WorkbookSheet.Chart chartKind(ReportViewDto view, String kind, String countLabel, String moneyLabel, String startLabel, String spanLabel, String title) {
        if ("bar".equals(kind) || "columns".equals(kind)) {
            kind = "histogram";
        }
        boolean money = view.getSum() != null && !view.getSum().isEmpty();
        int buckets = view.getRows() == null ? 0 : view.getRows().size();
        String series = money ? text(moneyLabel) : text(countLabel);
        if ("gantt".equals(kind)) {
            return gantt(view, title, text(startLabel), text(spanLabel));
        }
        if ("venn".equals(kind)) {
            return venn(view, title, text(countLabel));
        }
        if (buckets <= 0) {
            return null;
        }
        int last = 1 + Math.min(buckets, CATEGORY_LIMIT);
        if ("scatter".equals(kind)) {
            if (money) {
                return WorkbookSheet.Chart.scatter(title, series, 1, 2, 2, last);
            }
            return WorkbookSheet.Chart.scatterIndex(title, series, 1, 2, last);
        }
        String plot = "col";
        boolean histogram = "histogram".equals(kind);
        if ("bars".equals(kind)) {
            plot = "bar";
        } else if ("line".equals(kind)) {
            plot = "line";
        } else if ("area".equals(kind)) {
            plot = "area";
        } else if ("pie".equals(kind)) {
            plot = "pie";
        } else if ("donut".equals(kind)) {
            plot = "doughnut";
        } else if ("radar".equals(kind)) {
            plot = "radar";
        }
        return WorkbookSheet.Chart.categories(plot, title, series, 0, money ? 2 : 1, 2, last, histogram);
    }

    private static List<String> kindsOf(ReportViewDto view) {
        List<String> kinds = new ArrayList<String>();
        String raw = view == null || view.getChart() == null ? "" : view.getChart().trim().toLowerCase(Locale.ROOT);
        if (raw.isEmpty()) {
            raw = "bar";
        }
        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length; i++) {
            String kind = parts[i].trim();
            if ("columns".equals(kind) || "bar".equals(kind)) {
                kind = "histogram";
            }
            if (!kind.isEmpty() && !kinds.contains(kind)) {
                kinds.add(kind);
            }
        }
        if (kinds.isEmpty()) {
            kinds.add("bar");
        }
        return kinds;
    }

    private static String titleOf(ReportViewDto view) {
        return view == null || view.getTitle() == null ? "" : view.getTitle();
    }

    private static WorkbookSheet.Chart gantt(ReportViewDto view, String title, String startLabel, String spanLabel) {
        if (view.getLines() == null) {
            return null;
        }
        List<LocalDate> starts = new ArrayList<LocalDate>();
        List<LocalDate> ends = new ArrayList<LocalDate>();
        List<String> names = new ArrayList<String>();
        for (ReportLineDto line : view.getLines()) {
            if (names.size() >= GANTT_LIMIT) {
                break;
            }
            LocalDate start = day(line.getFrom());
            if (start == null) {
                continue;
            }
            LocalDate end = day(line.getTo());
            if (end == null || end.isBefore(start)) {
                end = start;
            }
            String name = line.getName() == null ? "" : line.getName().trim();
            if (name.isEmpty()) {
                name = line.getObjectKey() == null ? "" : line.getObjectKey();
            }
            names.add(name);
            starts.add(start);
            ends.add(end);
        }
        if (names.isEmpty()) {
            return null;
        }
        LocalDate origin = starts.get(0);
        for (LocalDate start : starts) {
            if (start.isBefore(origin)) {
                origin = start;
            }
        }
        List<String> offsets = new ArrayList<String>();
        List<String> spans = new ArrayList<String>();
        for (int i = 0; i < names.size(); i++) {
            offsets.add(Long.toString(ChronoUnit.DAYS.between(origin, starts.get(i))));
            long span = ChronoUnit.DAYS.between(starts.get(i), ends.get(i)) + 1L;
            spans.add(Long.toString(span < 1L ? 1L : span));
        }
        return WorkbookSheet.Chart.gantt(title, startLabel, spanLabel, names, offsets, spans);
    }

    private static WorkbookSheet.Chart venn(ReportViewDto view, String title, String series) {
        ReportVennDto venn = view.getVenn();
        if (venn == null || venn.getLabels() == null || venn.getLabels().isEmpty()) {
            return null;
        }
        List<String> labels = venn.getLabels();
        List<Integer> only = venn.getOnly() == null ? new ArrayList<Integer>() : venn.getOnly();
        List<Integer> pairs = venn.getPairs() == null ? new ArrayList<Integer>() : venn.getPairs();
        List<String> names = new ArrayList<String>();
        List<String> values = new ArrayList<String>();
        int size = Math.min(3, labels.size());
        for (int i = 0; i < size; i++) {
            names.add(labelAt(labels, i));
            values.add(Integer.toString(i < only.size() && only.get(i) != null ? only.get(i).intValue() : 0));
        }
        if (size >= 2) {
            names.add(labelAt(labels, 0) + " + " + labelAt(labels, 1));
            values.add(Integer.toString(pairAt(pairs, 0)));
        }
        if (size >= 3) {
            names.add(labelAt(labels, 0) + " + " + labelAt(labels, 2));
            values.add(Integer.toString(pairAt(pairs, 1)));
            names.add(labelAt(labels, 1) + " + " + labelAt(labels, 2));
            values.add(Integer.toString(pairAt(pairs, 2)));
            names.add(labelAt(labels, 0) + " + " + labelAt(labels, 1) + " + " + labelAt(labels, 2));
            values.add(Integer.toString(venn.getAll()));
        }
        return names.isEmpty() ? null : WorkbookSheet.Chart.columnsOf(title, series, names, values);
    }

    private static int pairAt(List<Integer> pairs, int index) {
        if (index >= pairs.size() || pairs.get(index) == null) {
            return 0;
        }
        return pairs.get(index).intValue();
    }

    private static String labelAt(List<String> labels, int index) {
        if (index >= labels.size() || labels.get(index) == null) {
            return "";
        }
        return labels.get(index);
    }

    private static LocalDate day(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if (text.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(text.substring(0, 10));
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static String text(String raw) {
        return raw == null ? "" : raw;
    }
}
