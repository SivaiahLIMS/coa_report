package com.stability.coareport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OotAnalysisResponse {
    private Long reportId;
    private String productName;
    private String batchNo;
    private String storageCondition;
    private String schedulePeriod;
    private String previousSchedulePeriod;
    private List<OotTestResult> testResults;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OotTestResult {
        private Long testResultId;
        private String sno;
        private String testName;
        private String specification;
        private BigDecimal currentValue;
        private BigDecimal previousValue;
        private String previousPeriodDate;
        private BigDecimal percentageChange;
        private String ootStatus;
        private BigDecimal ootLowerLimit;
        private BigDecimal ootUpperLimit;
        private BigDecimal percentageThreshold;
        private String ootJustification;
        private String ootJustifiedBy;
        private String colorCode;
        private boolean requiresJustification;
    }
}
