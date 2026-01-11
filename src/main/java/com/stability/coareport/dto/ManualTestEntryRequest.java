package com.stability.coareport.dto;

import java.util.List;

public class ManualTestEntryRequest {

    private String productName;
    private String productCode;
    private String specificationId;
    private String storageCondition;
    private String sampleOrientation;
    private String batchNo;
    private String batchSize;
    private String mfgDate;
    private String schedulePeriod;
    private String packingType;
    private String arNo;
    private String protocolId;
    private String expDate;
    private String scheduleDate;
    private String packSize;
    private String stpNumber;
    private String hdpeCapDetails;
    private String ldpeNozzleDetails;
    private String ldpeBottleDetails;
    private String branchName;
    private String companyName;
    private List<TestResultDto> testResults;

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

    public String getSpecificationId() {
        return specificationId;
    }

    public void setSpecificationId(String specificationId) {
        this.specificationId = specificationId;
    }

    public String getStorageCondition() {
        return storageCondition;
    }

    public void setStorageCondition(String storageCondition) {
        this.storageCondition = storageCondition;
    }

    public String getSampleOrientation() {
        return sampleOrientation;
    }

    public void setSampleOrientation(String sampleOrientation) {
        this.sampleOrientation = sampleOrientation;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(String batchSize) {
        this.batchSize = batchSize;
    }

    public String getMfgDate() {
        return mfgDate;
    }

    public void setMfgDate(String mfgDate) {
        this.mfgDate = mfgDate;
    }

    public String getSchedulePeriod() {
        return schedulePeriod;
    }

    public void setSchedulePeriod(String schedulePeriod) {
        this.schedulePeriod = schedulePeriod;
    }

    public String getPackingType() {
        return packingType;
    }

    public void setPackingType(String packingType) {
        this.packingType = packingType;
    }

    public String getArNo() {
        return arNo;
    }

    public void setArNo(String arNo) {
        this.arNo = arNo;
    }

    public String getProtocolId() {
        return protocolId;
    }

    public void setProtocolId(String protocolId) {
        this.protocolId = protocolId;
    }

    public String getExpDate() {
        return expDate;
    }

    public void setExpDate(String expDate) {
        this.expDate = expDate;
    }

    public String getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(String scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public String getPackSize() {
        return packSize;
    }

    public void setPackSize(String packSize) {
        this.packSize = packSize;
    }

    public String getStpNumber() {
        return stpNumber;
    }

    public void setStpNumber(String stpNumber) {
        this.stpNumber = stpNumber;
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

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public List<TestResultDto> getTestResults() {
        return testResults;
    }

    public void setTestResults(List<TestResultDto> testResults) {
        this.testResults = testResults;
    }
}
