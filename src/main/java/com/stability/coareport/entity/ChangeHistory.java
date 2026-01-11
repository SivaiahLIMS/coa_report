package com.stability.coareport.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "change_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    @Column(nullable = false)
    private String fieldName;

    @Column(length = 2000)
    private String oldValue;

    @Column(length = 2000)
    private String newValue;

    @Column(nullable = false)
    private String action;

    @CreatedBy
    @Column(name = "modified_by")
    private String modifiedBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime modifiedAt;

    @Column(length = 1000)
    private String remarks;

    @Column(name = "evidence_document_name")
    private String evidenceDocumentName;

    @Column(name = "evidence_document_path")
    private String evidenceDocumentPath;
}
