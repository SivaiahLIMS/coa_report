package com.stability.coareport.repository;

import com.stability.coareport.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByBranchId(Long branchId);

    Page<Report> findByBranchId(Long branchId, Pageable pageable);

    @Query("SELECT DISTINCT r.productName FROM Report r WHERE r.branch.id = :branchId")
    List<String> findDistinctProductNamesByBranchId(Long branchId);

    @Query("SELECT DISTINCT r.productName FROM Report r")
    List<String> findDistinctProductNames();

    List<Report> findByProductName(String productName);

    @Query("SELECT DISTINCT r.batchNo FROM Report r WHERE r.productName = :productName")
    List<String> findDistinctBatchNumbersByProductName(String productName);

    @Query("SELECT DISTINCT r.storageCondition FROM Report r WHERE r.productName = :productName")
    List<String> findDistinctStorageConditionsByProductName(String productName);

    List<Report> findByProductNameAndBatchNoIn(String productName, List<String> batchNumbers);

    List<Report> findByProductNameAndStorageCondition(String productName, String storageCondition);

    List<Report> findByProductNameOrderByCreatedAtAsc(String productName);

    List<Report> findByApprovalStatus(String approvalStatus);

    List<Report> findByProductNameAndSpecificationAndStorageCondition(String productName, String specification, String storageCondition);

    @Query("SELECT DISTINCT r.specification FROM Report r WHERE r.productName = :productName")
    List<String> findDistinctSpecificationsByProductName(String productName);

    @Query("SELECT DISTINCT r.market FROM Report r WHERE r.productName = :productName AND r.market IS NOT NULL")
    List<String> findDistinctMarketsByProductName(String productName);

    @Query("SELECT DISTINCT r.sampleOrientation FROM Report r WHERE r.productName = :productName AND r.sampleOrientation IS NOT NULL")
    List<String> findDistinctPositionsByProductName(String productName);

    @Query("SELECT DISTINCT r.packingType FROM Report r WHERE r.productName = :productName AND r.packingType IS NOT NULL")
    List<String> findDistinctPackingTypesByProductName(String productName);

    @Query("SELECT DISTINCT r.packSize FROM Report r WHERE r.productName = :productName AND r.packSize IS NOT NULL")
    List<String> findDistinctPackSizesByProductName(String productName);

    @Query("SELECT DISTINCT r.schedulePeriod FROM Report r WHERE r.productName = :productName AND r.schedulePeriod IS NOT NULL")
    List<String> findDistinctStationsByProductName(String productName);

    List<Report> findByProductNameAndBatchNoAndStorageCondition(String productName, String batchNo, String storageCondition);

    List<Report> findByProductNameAndBatchNo(String productName, String batchNo);
}
