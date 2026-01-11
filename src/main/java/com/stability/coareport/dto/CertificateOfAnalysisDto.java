package com.stability.coareport.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CertificateOfAnalysisDto {
    private Long id;
    private String productName;
    private String productCode;
    private String specificationId;
    private String storageCondition;
    private String sampleOrientation;
    private String batchNo;
    private String batchSize;
    private LocalDate mfgDate;
    private String schedulePeriod;
    private String packingType;
    private String arNo;
    private String protocolId;
    private LocalDate expDate;
    private LocalDate scheduleDate;
    private String packSize;
    private String stpNumber;
    private String companyName;
    private String branchName;
    private String hdpeCapDetails;
    private String ldpeNozzleDetails;
    private String ldpeBottleDetails;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
