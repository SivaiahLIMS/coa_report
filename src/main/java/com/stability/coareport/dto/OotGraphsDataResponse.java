package com.stability.coareport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OotGraphsDataResponse {
    private String productName;
    private String batchNo;
    private String storageCondition;
    private List<OotConfigForGraph> ootConfig;
    private Map<String, List<TestTrendData>> testTrends;
    private Map<String, List<PercentChangeData>> percentChanges;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OotConfigForGraph {
        private String testName;
        private String specification;
        private BigDecimal specLowerLimit;
        private BigDecimal specUpperLimit;
        private String specUnit;
        private BigDecimal targetValue;
        private BigDecimal ootLowerLimit;
        private BigDecimal ootUpperLimit;
        private BigDecimal percentageThreshold;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestTrendData {
        private String period;
        private BigDecimal value;
        private String ootStatus;
        private Boolean predicted;
        private BigDecimal confidenceLower;
        private BigDecimal confidenceUpper;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PercentChangeData {
        private String period;
        private BigDecimal percentChange;
        private String fromPeriod;
        private String toPeriod;
        private BigDecimal value;
        private BigDecimal previousValue;
    }
}
