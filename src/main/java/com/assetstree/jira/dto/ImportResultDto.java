package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class ImportResultDto {
    private int created;
    private int updated;
    private List<ImportIssueDto> errors = new ArrayList<ImportIssueDto>();
    private String uploadId;
    private int nextOffset;
    private int totalRows;
    private boolean done = true;
    private List<Line> lines = new ArrayList<Line>();

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public int getUpdated() {
        return updated;
    }

    public void setUpdated(int updated) {
        this.updated = updated;
    }

    public List<ImportIssueDto> getErrors() {
        return errors;
    }

    public void setErrors(List<ImportIssueDto> errors) {
        this.errors = errors == null ? new ArrayList<ImportIssueDto>() : errors;
    }

    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }
    public int getNextOffset() { return nextOffset; }
    public void setNextOffset(int nextOffset) { this.nextOffset = nextOffset; }
    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public List<Line> getLines() {
        return lines;
    }

    public void setLines(List<Line> lines) {
        this.lines = lines == null ? new ArrayList<Line>() : lines;
    }

    /** How many rows of one kind were created and how many were updated. */
    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class Line {
        private String label;
        private int created;
        private int updated;

        public Line() {
        }

        public Line(String label, int created, int updated) {
            this.label = label;
            this.created = created;
            this.updated = updated;
        }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public int getCreated() { return created; }
        public void setCreated(int created) { this.created = created; }
        public int getUpdated() { return updated; }
        public void setUpdated(int updated) { this.updated = updated; }
    }
}
