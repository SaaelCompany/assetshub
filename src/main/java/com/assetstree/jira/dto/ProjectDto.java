package com.assetstree.jira.dto;

import com.assetstree.jira.model.CardLayout;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class ProjectDto {
    private String key;
    private String name;
    private boolean canEdit;
    private boolean canCreate;
    private boolean canChange;
    private boolean canMove;
    private boolean canRemove;
    private boolean canComment;
    private boolean canConfigure;
    private boolean canGrant;
    private boolean canPlaces;
    private boolean canObjects;
    private boolean canTypes;
    private boolean canAssets;
    private boolean canAdmin;
    private boolean canArrange;
    private boolean canView = true;
    private boolean canReports = true;
    private boolean canMap = true;
    private boolean canScheme = true;
    private boolean canSchemeEdit;
    private String assetKey;
    private boolean treeObjects;
    private List<String> columns = new ArrayList<String>();
    private List<String> modules = new ArrayList<String>();
    private ModuleSetupDto setup;
    private CardLayout card;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public boolean isCanConfigure() {
        return canConfigure;
    }

    public void setCanConfigure(boolean canConfigure) {
        this.canConfigure = canConfigure;
    }

    public boolean isCanCreate() {
        return canCreate;
    }

    public void setCanCreate(boolean canCreate) {
        this.canCreate = canCreate;
    }

    public boolean isCanChange() {
        return canChange;
    }

    public void setCanChange(boolean canChange) {
        this.canChange = canChange;
    }

    public boolean isCanMove() {
        return canMove;
    }

    public void setCanMove(boolean canMove) {
        this.canMove = canMove;
    }

    public boolean isCanRemove() {
        return canRemove;
    }

    public void setCanRemove(boolean canRemove) {
        this.canRemove = canRemove;
    }

    public boolean isCanComment() {
        return canComment;
    }

    public void setCanComment(boolean canComment) {
        this.canComment = canComment;
    }

    public boolean isCanGrant() {
        return canGrant;
    }

    public void setCanGrant(boolean canGrant) {
        this.canGrant = canGrant;
    }

    public boolean isCanPlaces() {
        return canPlaces;
    }

    public void setCanPlaces(boolean canPlaces) {
        this.canPlaces = canPlaces;
    }

    public boolean isCanObjects() {
        return canObjects;
    }

    public void setCanObjects(boolean canObjects) {
        this.canObjects = canObjects;
    }

    public boolean isCanTypes() {
        return canTypes;
    }

    public void setCanTypes(boolean canTypes) {
        this.canTypes = canTypes;
    }

    public boolean isCanAssets() {
        return canAssets;
    }

    public void setCanAssets(boolean canAssets) {
        this.canAssets = canAssets;
    }

    public boolean isCanAdmin() {
        return canAdmin;
    }

    public void setCanAdmin(boolean canAdmin) {
        this.canAdmin = canAdmin;
    }

    public boolean isCanArrange() {
        return canArrange;
    }

    public void setCanArrange(boolean canArrange) {
        this.canArrange = canArrange;
    }

    public boolean isCanView() { return canView; }
    public void setCanView(boolean canView) { this.canView = canView; }
    public boolean isCanReports() { return canReports; }
    public void setCanReports(boolean canReports) { this.canReports = canReports; }
    public boolean isCanMap() { return canMap; }
    public void setCanMap(boolean canMap) { this.canMap = canMap; }
    public boolean isCanScheme() { return canScheme; }
    public void setCanScheme(boolean canScheme) { this.canScheme = canScheme; }
    public boolean isCanSchemeEdit() { return canSchemeEdit; }
    public void setCanSchemeEdit(boolean canSchemeEdit) { this.canSchemeEdit = canSchemeEdit; }

    public String getAssetKey() {
        return assetKey;
    }

    public void setAssetKey(String assetKey) {
        this.assetKey = assetKey;
    }

    public boolean isTreeObjects() {
        return treeObjects;
    }

    public void setTreeObjects(boolean treeObjects) {
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

    public CardLayout getCard() {
        return card;
    }

    public void setCard(CardLayout card) {
        this.card = card;
    }
}
