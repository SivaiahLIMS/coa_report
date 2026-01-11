package com.stability.coareport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StabilityReportResponse {
    private List<String> schedulePeriods;
    private List<TestResultRow> testResultRows;
    private Map<String, Object> metadata;
    private List<String> batches;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestResultRow {
        @JsonProperty("sNo")
        private String sNo;
        private String testName;
        private String specification;
        private String specificationVersion;
        private Map<String, String> periodResults;
        private Map<String, String> periodSpecifications;
        private boolean isNumeric;
        private List<ChartDataPoint> chartData;
        private Map<String, Map<String, String>> batchData;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartDataPoint {
        private String period;
        private Double value;
        private String originalResult;
    }
}
