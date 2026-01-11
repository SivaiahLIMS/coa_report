package com.stability.coareport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonResponse {
    private String productName;
    private List<String> testNames;
    private List<String> stations;
    private List<BatchData> batches;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchData {
        private String batchNumber;
        private String mfgDate;
        private String expDate;
        private String storageCondition;
        private Map<String, StationData> stationData;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StationData {
        private String station;
        private Map<String, TestValue> testResults;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestValue {
        private String result;
        private Double numericValue;
        private String specification;
    }
}
