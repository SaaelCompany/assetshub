package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

/** Request types a customer can raise for one piece of equipment. */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class PortalRequestFormDto {
    private int assetId;
    private String assetName = "";
    private String objectKey = "";
    private String projectKey = "";
    private List<Type> types = new ArrayList<Type>();

    public int getAssetId() {
        return assetId;
    }

    public void setAssetId(int assetId) {
        this.assetId = assetId;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName == null ? "" : assetName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey == null ? "" : objectKey;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey == null ? "" : projectKey;
    }

    public List<Type> getTypes() {
        return types;
    }

    public void setTypes(List<Type> types) {
        this.types = types == null ? new ArrayList<Type>() : types;
    }

    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class Type {
        private String id = "";
        private String name = "";
        private String description = "";
        private List<Field> fields = new ArrayList<Field>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id == null ? "" : id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name == null ? "" : name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description == null ? "" : description;
        }

        public List<Field> getFields() {
            return fields;
        }

        public void setFields(List<Field> fields) {
            this.fields = fields == null ? new ArrayList<Field>() : fields;
        }
    }

    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class Field {
        private String id = "";
        private String name = "";
        private boolean required;
        private String kind = "text";
        private List<Option> options = new ArrayList<Option>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id == null ? "" : id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name == null ? "" : name;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind == null || kind.isEmpty() ? "text" : kind;
        }

        public List<Option> getOptions() {
            return options;
        }

        public void setOptions(List<Option> options) {
            this.options = options == null ? new ArrayList<Option>() : options;
        }
    }

    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class Option {
        private String value = "";
        private String label = "";

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value == null ? "" : value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label == null ? "" : label;
        }
    }
}
