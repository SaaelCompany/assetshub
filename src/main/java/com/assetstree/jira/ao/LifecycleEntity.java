package com.assetstree.jira.ao;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Table("AST_LIFE")
public interface LifecycleEntity extends Entity {
    @Indexed
    int getAssetId();

    void setAssetId(int assetId);

    @StringLength(16)
    String getPurchased();

    void setPurchased(String purchased);

    @StringLength(120)
    String getSupplier();

    void setSupplier(String supplier);

    long getPriceMinor();

    void setPriceMinor(long priceMinor);

    @StringLength(16)
    String getWarrantyUntil();

    void setWarrantyUntil(String warrantyUntil);

    int getLifeMonths();

    void setLifeMonths(int lifeMonths);

    long getResidualMinor();

    void setResidualMinor(long residualMinor);

    @StringLength(16)
    String getWriteoffState();

    void setWriteoffState(String writeoffState);

    @StringLength(StringLength.UNLIMITED)
    String getWriteoffReason();

    void setWriteoffReason(String writeoffReason);

    @StringLength(255)
    String getRequesterKey();

    void setRequesterKey(String requesterKey);

    @StringLength(255)
    String getApproverKey();

    void setApproverKey(String approverKey);

    @StringLength(16)
    String getDecidedAt();

    void setDecidedAt(String decidedAt);

    @StringLength(160)
    String getDocument();

    void setDocument(String document);

    @StringLength(8)
    String getCurrency();

    void setCurrency(String currency);

    @StringLength(16)
    String getCommissioned();

    void setCommissioned(String commissioned);

    @StringLength(160)
    String getWarrantyService();

    void setWarrantyService(String warrantyService);

    /** Book value in minor units after the latest monthly pass. */
    long getBookMinor();

    void setBookMinor(long bookMinor);

    /** Month of that pass, yyyy-MM. Empty until the first pass. */
    @StringLength(16)
    String getBookMonth();

    void setBookMonth(String bookMonth);

    /** Forecast replacement day, yyyy-MM-dd. */
    @StringLength(16)
    String getReplaceOn();

    void setReplaceOn(String replaceOn);

    /** Contractor that services this asset. Empty when no contract is stored. */
    @StringLength(120)
    String getContractor();

    void setContractor(String contractor);

    @StringLength(160)
    String getContractNo();

    void setContractNo(String contractNo);

    /** First day of contractor service, yyyy-MM-dd. */
    @StringLength(16)
    String getContractFrom();

    void setContractFrom(String contractFrom);

    /** Last day of contractor service, yyyy-MM-dd. */
    @StringLength(16)
    String getContractUntil();

    void setContractUntil(String contractUntil);
}
