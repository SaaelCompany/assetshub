package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class LifecycleDto {
    private int assetId;
    private String purchased;
    private String supplier;
    private String price;
    private String warrantyUntil;
    private int lifeMonths;
    private String residual;
    private String monthly;
    private String book;
    private boolean warrantyActive;
    private String writeoffState;
    private String writeoffReason;
    private String requesterKey;
    private String requesterName;
    private String approverKey;
    private String approverName;
    private String decidedAt;
    private String document;
    private String currency;
    private String commissioned;
    private String warrantyService;

    public int getAssetId() { return assetId; }
    public void setAssetId(int assetId) { this.assetId = assetId; }
    public String getPurchased() { return purchased; }
    public void setPurchased(String purchased) { this.purchased = purchased; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public String getWarrantyUntil() { return warrantyUntil; }
    public void setWarrantyUntil(String warrantyUntil) { this.warrantyUntil = warrantyUntil; }
    public int getLifeMonths() { return lifeMonths; }
    public void setLifeMonths(int lifeMonths) { this.lifeMonths = lifeMonths; }
    public String getResidual() { return residual; }
    public void setResidual(String residual) { this.residual = residual; }
    public String getMonthly() { return monthly; }
    public void setMonthly(String monthly) { this.monthly = monthly; }
    public String getBook() { return book; }
    public void setBook(String book) { this.book = book; }
    public boolean isWarrantyActive() { return warrantyActive; }
    public void setWarrantyActive(boolean warrantyActive) { this.warrantyActive = warrantyActive; }
    public String getWriteoffState() { return writeoffState; }
    public void setWriteoffState(String writeoffState) { this.writeoffState = writeoffState; }
    public String getWriteoffReason() { return writeoffReason; }
    public void setWriteoffReason(String writeoffReason) { this.writeoffReason = writeoffReason; }
    public String getRequesterKey() { return requesterKey; }
    public void setRequesterKey(String requesterKey) { this.requesterKey = requesterKey; }
    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
    public String getApproverKey() { return approverKey; }
    public void setApproverKey(String approverKey) { this.approverKey = approverKey; }
    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }
    public String getDecidedAt() { return decidedAt; }
    public void setDecidedAt(String decidedAt) { this.decidedAt = decidedAt; }
    public String getDocument() { return document; }
    public void setDocument(String document) { this.document = document; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getCommissioned() { return commissioned; }
    public void setCommissioned(String commissioned) { this.commissioned = commissioned; }
    public String getWarrantyService() { return warrantyService; }
    public void setWarrantyService(String warrantyService) { this.warrantyService = warrantyService; }
}
