package com.assetstree.jira.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class LifecycleDraft {
    private String purchased;
    private String supplier;
    private String price;
    private String warrantyUntil;
    private Integer lifeMonths;
    private String residual;
    private String document;
    private String currency;
    private String commissioned;
    private String warrantyService;
    private String approverKey;

    public String getPurchased() { return purchased; }
    public void setPurchased(String purchased) { this.purchased = purchased; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public String getWarrantyUntil() { return warrantyUntil; }
    public void setWarrantyUntil(String warrantyUntil) { this.warrantyUntil = warrantyUntil; }
    public Integer getLifeMonths() { return lifeMonths; }
    public void setLifeMonths(Integer lifeMonths) { this.lifeMonths = lifeMonths; }
    public String getResidual() { return residual; }
    public void setResidual(String residual) { this.residual = residual; }
    public String getDocument() { return document; }
    public void setDocument(String document) { this.document = document; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getCommissioned() { return commissioned; }
    public void setCommissioned(String commissioned) { this.commissioned = commissioned; }
    public String getWarrantyService() { return warrantyService; }
    public void setWarrantyService(String warrantyService) { this.warrantyService = warrantyService; }
    public String getApproverKey() { return approverKey; }
    public void setApproverKey(String approverKey) { this.approverKey = approverKey; }
}
