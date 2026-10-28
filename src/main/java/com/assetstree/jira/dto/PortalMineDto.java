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
        private List<Card> assets = new ArrayList<Card>();

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

        public List<Card> getAssets() {
            return assets;
        }

        public void setAssets(List<Card> assets) {
            this.assets = assets == null ? new ArrayList<Card>() : assets;
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
        private String color = "";
        private String icon = "";
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
