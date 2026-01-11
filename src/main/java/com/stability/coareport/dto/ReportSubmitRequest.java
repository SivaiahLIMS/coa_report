package com.stability.coareport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportSubmitRequest {
    private String tempFileId;
    private Long branchId;
    private String pdfFileName;
    private String pdfFilePath;
    private String productName;
    private String productCode;
    private String arNo;
    private String batchNo;
    private String batchSize;
    private String mfgDate;
    private String expDate;
    private String specification;
    private String storageCondition;
    private String sampleQty;
    private String receivedDate;
    private String analysisStartDate;
    private String analysisEndDate;
    private String protocolId;
    private String stpNo;
    private String schedulePeriod;
    private String packingType;
    private String packSize;
    private String remarks;
    private String checkedBy;
    private String approvedBy;
    private String checkDate;
    private String approvalDate;
    private String hdpeCapDepth;
    private String ldpeNozzleDetails;
    private String ldpeBottleDetails;
    private String uploadedBy;
    private List<TestResultDto> testResults;
}
