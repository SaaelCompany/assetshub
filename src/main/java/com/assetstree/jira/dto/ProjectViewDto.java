package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class ProjectViewDto {
    private Boolean treeObjects;
    private List<String> columns;
    private List<String> modules;
    private ModuleSetupDto setup;

    public Boolean getTreeObjects() {
        return treeObjects;
    }

    public void setTreeObjects(Boolean treeObjects) {
        this.treeObjects = treeObjects;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public List<String> getModules() {
        return modules;
    }

    public void setModules(List<String> modules) {
        this.modules = modules;
    }

    public ModuleSetupDto getSetup() {
        return setup;
    }

    public void setSetup(ModuleSetupDto setup) {
        this.setup = setup;
    }
}
