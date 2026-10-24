package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class ReportViewDto {
    private String id;
    private String title;
    private String hint;
    private int total;
    private String sum;
    private boolean standard;
    private String groupBy;
    private String chart;
    private List<ReportBucketDto> rows = new ArrayList<ReportBucketDto>();
    private List<ReportLineDto> lines = new ArrayList<ReportLineDto>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = hint; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public String getSum() { return sum; }
    public void setSum(String sum) { this.sum = sum; }
    public boolean isStandard() { return standard; }
    public void setStandard(boolean standard) { this.standard = standard; }
    public String getGroupBy() { return groupBy; }
    public void setGroupBy(String groupBy) { this.groupBy = groupBy; }
    public String getChart() { return chart; }
    public void setChart(String chart) { this.chart = chart; }
    public List<ReportBucketDto> getRows() { return rows; }
    public void setRows(List<ReportBucketDto> rows) { this.rows = rows; }
    public List<ReportLineDto> getLines() { return lines; }
    public void setLines(List<ReportLineDto> lines) { this.lines = lines; }
}
