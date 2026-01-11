package com.stability.coareport.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StabilityReportRequest {
    private String productName;
    private String specification;
    private String batchNo;
    private String batchNumber;
    private List<String> batchNumbers;
    private String storageCondition;
    private String sampleOrientation;
    private String description;
    private String schedulePeriod;
}
