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
public class StabilityFilterOptionsResponse {
    private List<String> productNames;
    private List<String> specifications;
    private List<String> batchNumbers;
    private List<String> storageConditions;
    private List<String> sampleOrientations;
    private List<String> descriptions;
    private List<String> schedulePeriods;
}
