package com.stability.coareport.controller;

import com.stability.coareport.dto.StabilityFilterOptionsResponse;
import com.stability.coareport.dto.StabilityReportRequest;
import com.stability.coareport.dto.StabilityReportResponse;
import com.stability.coareport.service.StabilityReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stability-reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StabilityReportController {

    private final StabilityReportService stabilityReportService;

    @GetMapping("/filter-options")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'USER', 'QC', 'QA')")
    public ResponseEntity<StabilityFilterOptionsResponse> getFilterOptions(
            @RequestParam(required = false) String productName) {
        if (productName != null && !productName.isEmpty()) {
            return ResponseEntity.ok(stabilityReportService.getFilterOptionsForProduct(productName));
        }
        return ResponseEntity.ok(stabilityReportService.getFilterOptions());
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'USER', 'QC', 'QA')")
    public ResponseEntity<StabilityReportResponse> generateReport(@RequestBody StabilityReportRequest request) {
        if (request.getProductName() == null || request.getProductName().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(stabilityReportService.generateStabilityReport(request));
    }

    @PostMapping("/batch-comparison")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'USER', 'QC', 'QA')")
    public ResponseEntity<StabilityReportResponse> generateBatchComparison(@RequestBody StabilityReportRequest request) {
        if (request.getProductName() == null || request.getProductName().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (request.getBatchNumbers() == null || request.getBatchNumbers().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(stabilityReportService.generateBatchComparisonReport(request));
    }
}
