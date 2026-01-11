package com.stability.coareport.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificate_of_analysis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CertificateOfAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "product_code")
    private String productCode;

    @Column(name = "specification_id")
    private String specificationId;

    @Column(name = "storage_condition")
    private String storageCondition;

    @Column(name = "sample_orientation")
    private String sampleOrientation;

    @Column(name = "batch_no")
    private String batchNo;

    @Column(name = "batch_size")
    private String batchSize;

    @Column(name = "mfg_date")
    private LocalDate mfgDate;

    @Column(name = "schedule_period")
    private String schedulePeriod;

    @Column(name = "packing_type")
    private String packingType;

    @Column(name = "ar_no")
    private String arNo;

    @Column(name = "protocol_id")
    private String protocolId;

    @Column(name = "exp_date")
    private LocalDate expDate;

    @Column(name = "schedule_date")
    private LocalDate scheduleDate;

    @Column(name = "pack_size")
    private String packSize;

    @Column(name = "stp_number")
    private String stpNumber;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "branch_name")
    private String branchName;

    @Column(name = "hdpe_cap_details")
    private String hdpeCapDetails;

    @Column(name = "ldpe_nozzle_details")
    private String ldpeNozzleDetails;

    @Column(name = "ldpe_bottle_details")
    private String ldpeBottleDetails;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by")
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
}
