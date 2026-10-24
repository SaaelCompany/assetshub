package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class InventoryDto {
    private int total;
    private int checked;
    private List<InventoryRowDto> rows = new ArrayList<InventoryRowDto>();

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getChecked() {
        return checked;
    }

    public void setChecked(int checked) {
        this.checked = checked;
    }

    public List<InventoryRowDto> getRows() {
        return rows;
    }

    public void setRows(List<InventoryRowDto> rows) {
        this.rows = rows;
    }
}
