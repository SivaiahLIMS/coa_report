package com.stability.coareport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OosParetoResponse {
    private String productName;
    private String timeRange;
    private List<ParetoItem> paretoData;
    private int totalOosCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParetoItem {
        private String testName;
        private int oosCount;
        private int totalTestCount;
        private double oosRate;
        private double cumulativePercent;
    }
}
