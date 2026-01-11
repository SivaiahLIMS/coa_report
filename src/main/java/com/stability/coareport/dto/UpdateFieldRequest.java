package com.stability.coareport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFieldRequest {
    private String productName;
    private String fieldType;
    private String fieldName;
    private String newValue;
    private Long testResultId;
    private String remarks;
    private String evidenceDocumentName;
    private String evidenceDocumentPath;
}
