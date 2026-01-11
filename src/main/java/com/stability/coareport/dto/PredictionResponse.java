package com.stability.coareport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResponse {
    private String productName;
    private String storageCondition;
    private List<TestPrediction> predictions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestPrediction {
        private String testName;
        private List<DataPoint> historicalData;
        private List<DataPoint> predictedData;
        private String trendDirection;
        private Double confidence;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPoint {
        private String batchNumber;
        private String date;
        private Double value;
        private boolean isPredicted;
        private Integer monthsFromStart;
        private String schedulePeriod;

        public DataPoint(String batchNumber, String date, Double value, boolean isPredicted) {
            this.batchNumber = batchNumber;
            this.date = date;
            this.value = value;
            this.isPredicted = isPredicted;
        }
    }
}
