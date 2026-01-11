package com.stability.coareport.repository;

import com.stability.coareport.entity.CertificateOfAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CertificateOfAnalysisRepository extends JpaRepository<CertificateOfAnalysis, Long> {
    List<CertificateOfAnalysis> findByProductNameOrderByCreatedAtDesc(String productName);
    List<CertificateOfAnalysis> findByBatchNoOrderByCreatedAtDesc(String batchNo);
    List<CertificateOfAnalysis> findByBranchNameOrderByCreatedAtDesc(String branchName);
    List<CertificateOfAnalysis> findAllByOrderByCreatedAtDesc();
}
