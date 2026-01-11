package com.stability.coareport.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_oot_configuration")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ProductOotConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sno", nullable = false, length = 50)
    private String sno;

    @Column(name = "test_name", nullable = false, length = 500)
    private String testName;

    @Column(name = "specification", length = 500)
    private String specification;

    @Column(name = "oot_lower_limit", precision = 10, scale = 4)
    private BigDecimal ootLowerLimit;

    @Column(name = "oot_upper_limit", precision = 10, scale = 4)
    private BigDecimal ootUpperLimit;

    @Column(name = "percentage_threshold", precision = 5, scale = 2)
    private BigDecimal percentageThreshold = BigDecimal.valueOf(10.00);

    @Column(name = "spec_lower_limit", precision = 10, scale = 4)
    private BigDecimal specLowerLimit;

    @Column(name = "spec_upper_limit", precision = 10, scale = 4)
    private BigDecimal specUpperLimit;

    @Column(name = "spec_unit", length = 50)
    private String specUnit;

    @Column(name = "target_value", precision = 10, scale = 4)
    private BigDecimal targetValue;

    @Column(name = "lims_integration_enabled")
    private Boolean limsIntegrationEnabled = false;

    @Column(name = "lims_spec_id", length = 100)
    private String limsSpecId;

    @Column(name = "last_lims_sync")
    private LocalDateTime lastLimsSync;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "approval_status", length = 50)
    private String approvalStatus = "PENDING";

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
