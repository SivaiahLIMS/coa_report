package com.stability.coareport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private String productName;
    private String productCode;
    private Long totalReports;
    private String latestBatchNo;
    private String latestReceivedDate;
}
