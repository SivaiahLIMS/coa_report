package com.stability.coareport.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "batch_oot_override")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchOotOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "test_name", nullable = false, length = 500)
    private String testName;

    @Column(name = "oot_lower_limit", precision = 10, scale = 4)
    private BigDecimal ootLowerLimit;

    @Column(name = "oot_upper_limit", precision = 10, scale = 4)
    private BigDecimal ootUpperLimit;

    @Column(name = "percentage_threshold", precision = 5, scale = 2)
    private BigDecimal percentageThreshold;

    @Column(name = "override_reason", columnDefinition = "TEXT")
    private String overrideReason;

    @Column(name = "overridden_by")
    private String overriddenBy;

    @Column(name = "overridden_at")
    private LocalDateTime overriddenAt;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approval_status", length = 50)
    private String approvalStatus = "PENDING";
}
