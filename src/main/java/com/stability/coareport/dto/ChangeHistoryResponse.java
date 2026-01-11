package com.stability.coareport.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeHistoryResponse {
    private Long id;
    private String entityType;
    private Long entityId;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private String action;
    private String modifiedBy;
    private LocalDateTime modifiedAt;
    private String productName;
    private String batchNo;
    private String remarks;
    private String evidenceDocumentName;
    private String evidenceDocumentPath;
}
