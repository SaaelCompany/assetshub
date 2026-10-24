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

    @StringLength(16)
    String getChart();

    void setChart(String chart);

    @StringLength(16)
    String getMetric();

    void setMetric(String metric);
}
