package com.stability.coareport.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "product_code")
    private String productCode;

    @Column(name = "ar_no")
    private String arNo;

    @Column(name = "batch_no")
    private String batchNo;

    @Column(name = "batch_size")
    private String batchSize;

    @Column(name = "mfg_date")
    private String mfgDate;

    @Column(name = "exp_date")
    private String expDate;

    @Column(name = "specification")
    private String specification;

    @Column(name = "storage_condition")
    private String storageCondition;

    @Column(name = "sample_qty")
    private String sampleQty;

    @Column(name = "received_date")
    private String receivedDate;

    @Column(name = "analysis_start_date")
    private String analysisStartDate;

    @Column(name = "analysis_end_date")
    private String analysisEndDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    @JsonIgnore
    private Branch branch;

    @Column(name = "pdf_file_name")
    private String pdfFileName;

    @Column(name = "pdf_file_path")
    private String pdfFilePath;

    @Column(name = "protocol_id")
    private String protocolId;

    @Column(name = "stp_no")
    private String stpNo;

    @Column(name = "schedule_period")
    private String schedulePeriod;

    @Column(name = "packing_type")
    private String packingType;

    @Column(name = "pack_size")
    private String packSize;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "checked_by")
    private String checkedBy;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "check_date")
    private String checkDate;

    @Column(name = "approval_date")
    private String approvalDate;

    @Column(name = "sample_orientation")
    private String sampleOrientation;

    @Column(name = "market")
    private String market;

    @Column(name = "hdpe_cap_depth", length = 500)
    private String hdpeCapDepth;

    @Column(name = "ldpe_nozzle_details", length = 500)
    private String ldpeNozzleDetails;

    @Column(name = "ldpe_bottle_details", length = 500)
    private String ldpeBottleDetails;

    @Column(name = "description", length = 1000)
    private String description;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonManagedReference
    private List<TestResult> testResults;

    @CreatedBy
    @Column(name = "created_by")
    private String createdBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "approval_status")
    private String approvalStatus;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;
}
