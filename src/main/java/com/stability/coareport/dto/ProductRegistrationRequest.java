package com.stability.coareport.dto;

import java.time.LocalDate;

public class ProductRegistrationRequest {

    private String productName;
    private String productCode;
    private String batchNo;
    private String arNo;
    private String drugProductMfgLocation;
    private String drugSubstanceMfgLocation;
    private String drugSubstanceBatchNo;
    private String specificationId;
    private String batchSize;
    private String protocolId;
    private String stpNo;
    private LocalDate mfgDate;
    private LocalDate expDate;
    private String storageCondition;
    private LocalDate scheduleDate;
    private String sampleOrientation;
    private String packageType;
    private String packSize;
    private String hdpeCapDetails;
    private String ldpeNozzleDetails;
    private String ldpeBottleDetails;
    private String market;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getArNo() {
        return arNo;
    }

    public void setArNo(String arNo) {
        this.arNo = arNo;
    }

    public String getDrugProductMfgLocation() {
        return drugProductMfgLocation;
    }

    public void setDrugProductMfgLocation(String drugProductMfgLocation) {
        this.drugProductMfgLocation = drugProductMfgLocation;
    }

    public String getDrugSubstanceMfgLocation() {
        return drugSubstanceMfgLocation;
    }

    public void setDrugSubstanceMfgLocation(String drugSubstanceMfgLocation) {
        this.drugSubstanceMfgLocation = drugSubstanceMfgLocation;
    }

    public String getDrugSubstanceBatchNo() {
        return drugSubstanceBatchNo;
    }

    public void setDrugSubstanceBatchNo(String drugSubstanceBatchNo) {
        this.drugSubstanceBatchNo = drugSubstanceBatchNo;
    }

    public String getSpecificationId() {
        return specificationId;
    }

    public void setSpecificationId(String specificationId) {
        this.specificationId = specificationId;
    }

    public String getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(String batchSize) {
        this.batchSize = batchSize;
    }

    public String getProtocolId() {
        return protocolId;
    }

    public void setProtocolId(String protocolId) {
        this.protocolId = protocolId;
    }

    public String getStpNo() {
        return stpNo;
    }

    public void setStpNo(String stpNo) {
        this.stpNo = stpNo;
    }

    public LocalDate getMfgDate() {
        return mfgDate;
    }

    public void setMfgDate(LocalDate mfgDate) {
        this.mfgDate = mfgDate;
    }

    public LocalDate getExpDate() {
        return expDate;
    }

    public void setExpDate(LocalDate expDate) {
        this.expDate = expDate;
    }

    public String getStorageCondition() {
        return storageCondition;
    }

    public void setStorageCondition(String storageCondition) {
        this.storageCondition = storageCondition;
    }

    public LocalDate getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(LocalDate scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public String getSampleOrientation() {
        return sampleOrientation;
    }

    public void setSampleOrientation(String sampleOrientation) {
        this.sampleOrientation = sampleOrientation;
    }

    public String getPackageType() {
        return packageType;
    }

    public void setPackageType(String packageType) {
        this.packageType = packageType;
    }

    public String getPackSize() {
        return packSize;
    }

    public void setPackSize(String packSize) {
        this.packSize = packSize;
    }

    public String getHdpeCapDetails() {
        return hdpeCapDetails;
    }

    public void setHdpeCapDetails(String hdpeCapDetails) {
        this.hdpeCapDetails = hdpeCapDetails;
    }

    public String getLdpeNozzleDetails() {
        return ldpeNozzleDetails;
    }

    public void setLdpeNozzleDetails(String ldpeNozzleDetails) {
        this.ldpeNozzleDetails = ldpeNozzleDetails;
    }

    public String getLdpeBottleDetails() {
        return ldpeBottleDetails;
    }

    public void setLdpeBottleDetails(String ldpeBottleDetails) {
        this.ldpeBottleDetails = ldpeBottleDetails;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }
}
