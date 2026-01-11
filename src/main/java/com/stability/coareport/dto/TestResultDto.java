package com.stability.coareport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestResultDto {
    @JsonProperty("sNo")
    private String sNo;
    private String test;
    private String result;
    private String specification;
    private String objection;
}
