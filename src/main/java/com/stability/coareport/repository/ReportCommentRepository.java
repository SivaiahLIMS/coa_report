package com.stability.coareport.repository;

import com.stability.coareport.entity.ReportComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportCommentRepository extends JpaRepository<ReportComment, Long> {

    List<ReportComment> findByReportIdOrderByCreatedAtDesc(Long reportId);

    List<ReportComment> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByReportId(Long reportId);
}
