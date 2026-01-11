package com.stability.coareport.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_code", nullable = false, length = 100)
    private String productCode;

    @Column(name = "batch_no", nullable = false, length = 100)
    private String batchNo;

    @Column(name = "ar_no", length = 100)
    private String arNo;

    @Column(name = "drug_product_mfg_location")
    private String drugProductMfgLocation;

    @Column(name = "drug_substance_mfg_location")
    private String drugSubstanceMfgLocation;

    @Column(name = "drug_substance_batch_no", length = 100)
    private String drugSubstanceBatchNo;

    @Column(name = "specification_id", length = 100)
    private String specificationId;

    @Column(name = "batch_size", length = 100)
    private String batchSize;

    @Column(name = "protocol_id", length = 100)
    private String protocolId;

    @Column(name = "stp_no", length = 100)
    private String stpNo;

    @Column(name = "mfg_date", nullable = false)
    private LocalDate mfgDate;

    @Column(name = "exp_date", nullable = false)
    private LocalDate expDate;

    @Column(name = "storage_condition", nullable = false)
    private String storageCondition;

    @Column(name = "schedule_date")
    private LocalDate scheduleDate;

    @Column(name = "sample_orientation", length = 100)
    private String sampleOrientation;

    @Column(name = "package_type", length = 100)
    private String packageType;

    @Column(name = "pack_size", length = 100)
    private String packSize;

    @Column(name = "hdpe_cap_details", columnDefinition = "TEXT")
    private String hdpeCapDetails;

    @Column(name = "ldpe_nozzle_details", columnDefinition = "TEXT")
    private String ldpeNozzleDetails;

    @Column(name = "ldpe_bottle_details", columnDefinition = "TEXT")
    private String ldpeBottleDetails;

    @Column(name = "market", length = 100)
    private String market;

    @Column(name = "count", length = 100)
    private String count;

    @Column(name = "status", length = 50)
    private String status = "Active";

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
