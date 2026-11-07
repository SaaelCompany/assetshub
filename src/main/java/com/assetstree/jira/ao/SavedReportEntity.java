package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Table("AST_RPT")
public interface SavedReportEntity extends Entity {
    @Indexed
    @StringLength(80)
    String getProjectKey();

    void setProjectKey(String projectKey);

    @StringLength(80)
    String getName();

    void setName(String name);

    @StringLength(16)
    String getGroupBy();

    void setGroupBy(String groupBy);

    @StringLength(16)
    String getFilterField();

    void setFilterField(String filterField);

    @StringLength(255)
    String getFilterValue();

    void setFilterValue(String filterValue);

    /** Full filter when it does not fit in {@link #getFilterValue()}. */
    @StringLength(StringLength.UNLIMITED)
    String getFilterText();

    void setFilterText(String filterText);

    /** First chart, so a database that still has the short column keeps a valid value. */
    @StringLength(16)
    String getChart();

    void setChart(String chart);

    /** Every chart marked in the constructor. The short column cannot hold the list. */
    @StringLength(120)
    String getChartText();

    void setChartText(String chartText);

    @StringLength(16)
    String getMetric();

    void setMetric(String metric);

    /** Type field used when {@link #getGroupBy()} is {@code field}. */
    @StringLength(40)
    String getFieldKey();

    void setFieldKey(String fieldKey);

    /** Optional type field the objects must match. */
    @StringLength(40)
    String getAttrKey();

    void setAttrKey(String attrKey);

    @StringLength(80)
    String getAttrValue();

    void setAttrValue(String attrValue);
}
