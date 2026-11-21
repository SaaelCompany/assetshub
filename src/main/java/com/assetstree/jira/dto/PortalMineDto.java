package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

/** Equipment a customer sees on their own portal profile. */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class PortalMineDto {
    private String title = "";
    private int total;
    private String totalLabel = "";
    private String layout = "";
    private List<Group> projects = new ArrayList<Group>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getTotalLabel() {
        return totalLabel;
    }

    public void setTotalLabel(String totalLabel) {
        this.totalLabel = totalLabel == null ? "" : totalLabel;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout == null ? "" : layout;
    }

    public List<Group> getProjects() {
        return projects;
    }

    public void setProjects(List<Group> projects) {
        this.projects = projects == null ? new ArrayList<Group>() : projects;
    }

    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class Group {
        private String key = "";
        private int count;
        private String countLabel = "";
        private String layout = "";
        private List<Card> assets = new ArrayList<Card>();
        private List<Chart> charts = new ArrayList<Chart>();

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key == null ? "" : key;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public String getCountLabel() {
            return countLabel;
        }

        public void setCountLabel(String countLabel) {
            this.countLabel = countLabel == null ? "" : countLabel;
        }

        public String getLayout() {
            return layout;
        }

        public void setLayout(String layout) {
            this.layout = layout == null ? "" : layout;
        }

        public List<Card> getAssets() {
            return assets;
        }

        public void setAssets(List<Card> assets) {
            this.assets = assets == null ? new ArrayList<Card>() : assets;
        }

        public List<Chart> getCharts() {
            return charts;
        }

        public void setCharts(List<Chart> charts) {
            this.charts = charts == null ? new ArrayList<Chart>() : charts;
        }
    }

    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class Chart {
        private String title = "";
        private String kind = "bars";
        private int total;
        private List<Slice> rows = new ArrayList<Slice>();

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title == null ? "" : title;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = "donut".equals(kind) ? "donut" : "bars";
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public List<Slice> getRows() {
            return rows;
        }

        public void setRows(List<Slice> rows) {
            this.rows = rows == null ? new ArrayList<Slice>() : rows;
        }
    }

    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class Slice {
        private String label = "";
        private int count;
        private String color = "";

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label == null ? "" : label;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color == null ? "" : color;
        }
    }

    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class Card {
        private int id;
        private String name = "";
        private String objectKey = "";
        private boolean showKey;
        private boolean showStatus;
        private String status = "";
        private String statusLabel = "";
        private String statusCategory = "";
        private String role = "";
        private String typeName = "";
        private String place = "";
        private boolean sharedOnly;
        private String color = "";
        private String icon = "";
        private boolean photo;
        private String updated = "";
        private List<Line> lines = new ArrayList<Line>();

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name == null ? "" : name;
        }

        public String getObjectKey() {
            return objectKey;
        }

        public void setObjectKey(String objectKey) {
            this.objectKey = objectKey == null ? "" : objectKey;
        }

        public boolean isShowKey() {
            return showKey;
        }

        public void setShowKey(boolean showKey) {
            this.showKey = showKey;
        }

        public boolean isShowStatus() {
            return showStatus;
        }

        public void setShowStatus(boolean showStatus) {
            this.showStatus = showStatus;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status == null ? "" : status;
        }

        public String getStatusLabel() {
            return statusLabel;
        }

        public void setStatusLabel(String statusLabel) {
            this.statusLabel = statusLabel == null ? "" : statusLabel;
        }

        public String getStatusCategory() {
            return statusCategory;
        }

        public void setStatusCategory(String statusCategory) {
            this.statusCategory = statusCategory == null ? "" : statusCategory;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role == null ? "" : role;
        }

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName == null ? "" : typeName;
        }

        public String getPlace() {
            return place;
        }

        public void setPlace(String place) {
            this.place = place == null ? "" : place;
        }

        public boolean isSharedOnly() {
            return sharedOnly;
        }

        public void setSharedOnly(boolean sharedOnly) {
            this.sharedOnly = sharedOnly;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color == null ? "" : color;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon == null ? "" : icon;
        }

        public boolean isPhoto() {
            return photo;
        }

        public void setPhoto(boolean photo) {
            this.photo = photo;
        }

        public String getUpdated() {
            return updated;
        }

        public void setUpdated(String updated) {
            this.updated = updated == null ? "" : updated;
        }

        public List<Line> getLines() {
            return lines;
        }

        public void setLines(List<Line> lines) {
            this.lines = lines == null ? new ArrayList<Line>() : lines;
        }
    }

    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class Line {
        private String label = "";
        private String value = "";

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label == null ? "" : label;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value == null ? "" : value;
        }
    }
}
