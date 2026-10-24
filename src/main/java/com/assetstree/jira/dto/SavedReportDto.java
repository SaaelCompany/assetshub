package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class SavedReportDto {
    private String id;
    private String name;
    private String groupBy;
    private String filterField;
    private String filterValue;
    private String chart;
    private String metric;
    private boolean standard;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGroupBy() { return groupBy; }
    public void setGroupBy(String groupBy) { this.groupBy = groupBy; }
    public String getFilterField() { return filterField; }
    public void setFilterField(String filterField) { this.filterField = filterField; }
    public String getFilterValue() { return filterValue; }
    public void setFilterValue(String filterValue) { this.filterValue = filterValue; }
    public String getChart() { return chart; }
    public void setChart(String chart) { this.chart = chart; }
    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }
    public boolean isStandard() { return standard; }
    public void setStandard(boolean standard) { this.standard = standard; }
}
