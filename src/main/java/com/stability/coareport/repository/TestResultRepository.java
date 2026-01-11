package com.stability.coareport.repository;

import com.stability.coareport.entity.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {
    List<TestResult> findByReportId(Long reportId);

    @Query("SELECT tr FROM TestResult tr WHERE tr.report.id = :reportId AND LOWER(tr.test) LIKE LOWER(CONCAT('%', :testName, '%'))")
    List<TestResult> findByReportIdAndTestNameContainingIgnoreCase(Long reportId, String testName);

    @Query("SELECT DISTINCT tr.test FROM TestResult tr JOIN tr.report r WHERE r.productName = :productName")
    List<String> findDistinctTestNamesByProductName(String productName);

    @Query("SELECT DISTINCT tr.specification FROM TestResult tr JOIN tr.report r WHERE r.productName = :productName AND tr.specification IS NOT NULL")
    List<String> findDistinctSpecificationsByProductName(String productName);
}
