package com.stability.coareport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilterOptionsResponse {
    private List<String> testNames;
    private List<String> specifications;
    private List<String> markets;
    private List<String> positions;
    private List<String> packingTypes;
    private List<String> packSizes;
    private List<String> stations;
    private List<String> storageConditions;
}
