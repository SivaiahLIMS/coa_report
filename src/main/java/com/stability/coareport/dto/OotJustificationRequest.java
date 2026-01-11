package com.stability.coareport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OotJustificationRequest {
    private Long reportId;
    private List<TestJustification> testJustifications;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestJustification {
        private Long testResultId;
        private String justification;
    }
}
