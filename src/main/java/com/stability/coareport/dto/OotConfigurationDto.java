package com.stability.coareport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OotConfigurationDto {
    private Long id;
    private Long productId;
    private String sno;
    private String testName;
    private String specification;
    private BigDecimal ootLowerLimit;
    private BigDecimal ootUpperLimit;
    private BigDecimal percentageThreshold;
    private BigDecimal specLowerLimit;
    private BigDecimal specUpperLimit;
    private String specUnit;
    private BigDecimal targetValue;
    private Boolean limsIntegrationEnabled;
    private String limsSpecId;
    private LocalDateTime lastLimsSync;
    private Boolean isActive;
    private String approvalStatus;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
