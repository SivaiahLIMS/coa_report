package com.stability.coareport.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportListResponse {
    private Long id;
    private String productName;
    private String batchNo;
    private String storageCondition;
    private String schedulePeriod;
    private String mfgDate;
    private String expDate;
    private LocalDateTime createdAt;
    private String createdBy;
}
