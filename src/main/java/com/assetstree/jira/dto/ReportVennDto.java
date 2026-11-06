package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

/** Overlap of the largest groups. {@code only} is each group alone, {@code pairs} is AB, AC, BC. */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class ReportVennDto {
    private List<String> keys = new ArrayList<String>();
    private List<String> labels = new ArrayList<String>();
    private List<Integer> only = new ArrayList<Integer>();
    private List<Integer> pairs = new ArrayList<Integer>();
    private int all;

    public List<String> getKeys() { return keys; }
    public void setKeys(List<String> keys) { this.keys = keys; }
    public List<String> getLabels() { return labels; }
    public void setLabels(List<String> labels) { this.labels = labels; }
    public List<Integer> getOnly() { return only; }
    public void setOnly(List<Integer> only) { this.only = only; }
    public List<Integer> getPairs() { return pairs; }
    public void setPairs(List<Integer> pairs) { this.pairs = pairs; }
    public int getAll() { return all; }
    public void setAll(int all) { this.all = all; }
}
