package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class SavedReportDraft {
    private String name;
    private String groupBy;
    private String filterField;
    private String filterValue;
    private String chart;
    private String metric;
    private String fieldKey;
    private String attrKey;
    private String attrValue;

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
    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
    public String getAttrKey() { return attrKey; }
    public void setAttrKey(String attrKey) { this.attrKey = attrKey; }
    public String getAttrValue() { return attrValue; }
    public void setAttrValue(String attrValue) { this.attrValue = attrValue; }
}
