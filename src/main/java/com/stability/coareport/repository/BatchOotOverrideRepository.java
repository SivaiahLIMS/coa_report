package com.stability.coareport.repository;

import com.stability.coareport.entity.BatchOotOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BatchOotOverrideRepository extends JpaRepository<BatchOotOverride, Long> {

    List<BatchOotOverride> findByReportId(Long reportId);

    Optional<BatchOotOverride> findByReportIdAndTestName(Long reportId, String testName);

    List<BatchOotOverride> findByApprovalStatus(String approvalStatus);

    boolean existsByReportIdAndTestName(Long reportId, String testName);
}
