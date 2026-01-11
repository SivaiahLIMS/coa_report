package com.stability.coareport.service;

import com.stability.coareport.dto.ApprovalTrackingResponse;
import com.stability.coareport.dto.PageResponse;
import com.stability.coareport.entity.ChangeHistory;
import com.stability.coareport.entity.Report;
import com.stability.coareport.entity.ReportComment;
import com.stability.coareport.entity.User;
import com.stability.coareport.repository.ChangeHistoryRepository;
import com.stability.coareport.repository.ReportCommentRepository;
import com.stability.coareport.repository.ReportRepository;
import com.stability.coareport.repository.UserRepository;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApprovalService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ChangeHistoryRepository changeHistoryRepository;
    private final ReportCommentRepository reportCommentRepository;
    private final MeterRegistry meterRegistry;

    public ApprovalService(ReportRepository reportRepository, UserRepository userRepository,
                          ChangeHistoryRepository changeHistoryRepository, ReportCommentRepository reportCommentRepository,
                          MeterRegistry meterRegistry) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.changeHistoryRepository = changeHistoryRepository;
        this.reportCommentRepository = reportCommentRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    @Timed(value = "reports.approve", description = "Time taken to approve a report")
    public void approveReport(Long reportId, String username) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found with id: " + reportId));

        User qaUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (!"QA".equalsIgnoreCase(qaUser.getRole().name())) {
            throw new RuntimeException("Only QA users can approve reports");
        }

        report.setApprovalStatus("approved");
        report.setApprovedBy(username);
        report.setApprovedAt(LocalDateTime.now());
        report.setRejectionReason(null);

        reportRepository.save(report);
        meterRegistry.counter("reports.approved.total").increment();
    }

    @Transactional
    @Timed(value = "reports.reject", description = "Time taken to reject a report")
    public void rejectReport(Long reportId, String username, String reason) {
        meterRegistry.counter("reports.rejected.total").increment();
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found with id: " + reportId));

        User qaUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        String userRole = qaUser.getRole().name().toUpperCase().trim();
        if (!"QA".equals(userRole)) {
            throw new RuntimeException("Only QA users can reject reports. Current role: " + userRole);
        }

        report.setApprovalStatus("rejected");
        report.setRejectionReason(reason);
        report.setApprovedBy(username);
        report.setApprovedAt(LocalDateTime.now());

        reportRepository.save(report);

        List<ChangeHistory> changeHistories = changeHistoryRepository.findByEntityTypeAndEntityIdOrderByModifiedAtDesc("Report", reportId);
        if (!changeHistories.isEmpty()) {
            changeHistoryRepository.deleteAll(changeHistories);
        }

        List<ReportComment> comments = reportCommentRepository.findByReportIdOrderByCreatedAtDesc(reportId);
        if (!comments.isEmpty()) {
            reportCommentRepository.deleteAll(comments);
        }
    }

    @Transactional(readOnly = true)
    public List<ApprovalTrackingResponse> getApprovalTracking(
            String productName, String batchNo, String specification,
            String storageCondition, String market, String orientation, String pack) {
        List<Report> reports = reportRepository.findAll();

        return reports.stream()
                .filter(report -> productName == null || productName.isEmpty() || productName.equals(report.getProductName()))
                .filter(report -> batchNo == null || batchNo.isEmpty() || batchNo.equals(report.getBatchNo()))
                .filter(report -> specification == null || specification.isEmpty() || specification.equals(report.getSpecification()))
                .filter(report -> storageCondition == null || storageCondition.isEmpty() || storageCondition.equals(report.getStorageCondition()))
                .filter(report -> market == null || market.isEmpty() || market.equals(report.getMarket()))
                .filter(report -> orientation == null || orientation.isEmpty() || orientation.equals(report.getSampleOrientation()))
                .filter(report -> pack == null || pack.isEmpty() || pack.equals(report.getPackSize()))
                .map(this::convertToApprovalTrackingResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApprovalTrackingResponse> getPendingApprovals() {
        List<Report> reports = reportRepository.findByApprovalStatus("pending");
        return reports.stream()
                .map(this::convertToApprovalTrackingResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<ApprovalTrackingResponse> getApprovalTrackingPaginated(
            int page, int size, String sortBy, String sortDirection,
            String productName, String batchNo, String specification,
            String storageCondition, String market, String orientation, String pack) {

        List<ApprovalTrackingResponse> allTracking = getApprovalTracking(
                productName, batchNo, specification, storageCondition, market, orientation, pack);

        int start = page * size;
        int end = Math.min(start + size, allTracking.size());

        if (start >= allTracking.size()) {
            return new PageResponse<>(List.of(), page, size, allTracking.size(),
                    (int) Math.ceil((double) allTracking.size() / size), true, true);
        }

        List<ApprovalTrackingResponse> pageContent = allTracking.subList(start, end);
        return new PageResponse<>(pageContent, page, size, allTracking.size(),
                (int) Math.ceil((double) allTracking.size() / size),
                page == 0,
                end >= allTracking.size());
    }

    @Transactional(readOnly = true)
    public PageResponse<ApprovalTrackingResponse> getPendingApprovalsPaginated(
            int page, int size, String sortBy, String sortDirection) {

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

        PageRequest pageRequest;
        if (sortBy != null && !sortBy.isEmpty()) {
            pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        } else {
            pageRequest = PageRequest.of(page, size, Sort.by(direction, "id"));
        }

        Page<Report> reportPage = reportRepository.findAll(pageRequest);
        List<ApprovalTrackingResponse> responses = reportPage.getContent().stream()
                .filter(r -> "pending".equals(r.getApprovalStatus()))
                .map(this::convertToApprovalTrackingResponse)
                .collect(Collectors.toList());

        Page<ApprovalTrackingResponse> responsePage = new PageImpl<>(
                responses, pageRequest, reportPage.getTotalElements());
        return PageResponse.fromSpringPage(responsePage);
    }

    private ApprovalTrackingResponse convertToApprovalTrackingResponse(Report report) {
        ApprovalTrackingResponse response = new ApprovalTrackingResponse();
        response.setId(report.getId());
        response.setProductName(report.getProductName());
        response.setProductCode(report.getProductCode());
        response.setBatchNo(report.getBatchNo());
        response.setSpecification(report.getSpecification());
        response.setStorageCondition(report.getStorageCondition());
        response.setUploadedBy(report.getUploadedBy());
        response.setUploadedAt(report.getUploadedAt());
        response.setApprovedBy(report.getApprovedBy());
        response.setApprovedAt(report.getApprovedAt());
        response.setApprovalStatus(report.getApprovalStatus());
        response.setRejectionReason(report.getRejectionReason());
        response.setPdfFileName(report.getPdfFileName());
        response.setPdfFilePath(report.getPdfFilePath());
        response.setMarket(report.getMarket());
        response.setSampleOrientation(report.getSampleOrientation());
        response.setPackSize(report.getPackSize());
        response.setSampleQty(report.getSampleQty());
        return response;
    }
}
