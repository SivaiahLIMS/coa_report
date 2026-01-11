package com.stability.coareport.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonBackReference
    private Report report;

    @Column(name = "s_no")
    private String sNo;

    @Column(nullable = false, length = 500)
    private String test;

    @Column(length = 1000)
    private String result;

    @Column(length = 1000)
    private String specification;

    @Column(length = 500)
    private String objection;

    @Column(name = "oot_status", length = 50)
    private String ootStatus = "PENDING";

    @Column(name = "percentage_change", precision = 7, scale = 4)
    private BigDecimal percentageChange;

    @Column(name = "previous_period_value", precision = 10, scale = 4)
    private BigDecimal previousPeriodValue;

    @Column(name = "previous_period_date", length = 100)
    private String previousPeriodDate;

    @Column(name = "oot_justification", columnDefinition = "TEXT")
    private String ootJustification;

    @Column(name = "oot_justified_by")
    private String ootJustifiedBy;

    @Column(name = "oot_justified_at")
    private LocalDateTime ootJustifiedAt;

    @Column(name = "oos_status", length = 50)
    private String oosStatus = "PENDING";

    @Column(name = "spec_distance_percent", precision = 7, scale = 2)
    private BigDecimal specDistancePercent;

    @Column(name = "oos_justification", columnDefinition = "TEXT")
    private String oosJustification;

    @Column(name = "oos_justified_by")
    private String oosJustifiedBy;

    @Column(name = "oos_justified_at")
    private LocalDateTime oosJustifiedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
