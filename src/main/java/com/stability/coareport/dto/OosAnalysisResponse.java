package com.stability.coareport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OosAnalysisResponse {
    private String productName;
    private String batchNo;
    private String storageCondition;
    private String schedulePeriod;
    private List<OosTestResult> testResults;
    private OosSummary summary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OosTestResult {
        private Long testResultId;
        private String sno;
        private String testName;
        private String specification;
        private BigDecimal value;
        private BigDecimal specLowerLimit;
        private BigDecimal specUpperLimit;
        private String specUnit;
        private BigDecimal targetValue;
        private String oosStatus;
        private BigDecimal specDistancePercent;
        private boolean requiresJustification;
        private String oosJustification;
        private String oosJustifiedBy;
        private String colorCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OosSummary {
        private int totalTests;
        private int inSpecCount;
        private int warningCount;
        private int oosCount;
        private int justifiedCount;
        private double oosRate;
    }
}
