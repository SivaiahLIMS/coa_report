package com.stability.coareport.controller;

import com.stability.coareport.dto.PageResponse;
import com.stability.coareport.dto.ReportPreviewResponse;
import com.stability.coareport.dto.ReportSubmitRequest;
import com.stability.coareport.dto.UpdateFieldRequest;
import com.stability.coareport.entity.ChangeHistory;
import com.stability.coareport.entity.Report;
import com.stability.coareport.exception.ScannedPdfNotSupportedException;
import com.stability.coareport.security.UserDetailsImpl;
import com.stability.coareport.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'QC', 'QA')")
    public ResponseEntity<?> uploadForPreview(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            ReportPreviewResponse preview = reportService.uploadForPreview(file);
            return ResponseEntity.ok(preview);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error processing PDF: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'QC', 'QA')")
    public ResponseEntity<?> submitReport(
            @RequestBody ReportSubmitRequest request,
            Authentication authentication
    ) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            Report report = reportService.submitReport(request);
            return ResponseEntity.ok(report);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error saving report: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'QC', 'QA')")
    public ResponseEntity<?> createManualTestEntry(
            @RequestBody com.stability.coareport.dto.ManualTestEntryRequest request,
            Authentication authentication
    ) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String username = userDetails.getUsername();

            Report report = reportService.createManualTestEntry(request, username);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating manual test entry: " + e.getMessage());
        }
    }

    @PostMapping("/product-based-preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'QC', 'QA')")
    public ResponseEntity<?> productBasedPreview(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            ReportPreviewResponse preview = reportService.uploadForProductBasedPreview(file);
            return ResponseEntity.ok(preview);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error processing PDF: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/product-based-upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'QC', 'QA')")
    public ResponseEntity<?> productBasedUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("productName") String productName,
            @RequestParam("productCode") String productCode,
            @RequestParam("batchNo") String batchNo,
            @RequestParam("arNo") String arNo,
            @RequestParam("specificationId") String specificationId,
            @RequestParam("batchSize") String batchSize,
            @RequestParam("storageCondition") String storageCondition,
            @RequestParam("sampleOrientation") String sampleOrientation,
            @RequestParam("schedulePeriod") String schedulePeriod,
            @RequestParam("companyId") Long companyId,
            @RequestParam("branchId") Long branchId,
            Authentication authentication
    ) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            Report report = reportService.processProductBasedUpload(
                    file, productName, productCode, batchNo, arNo, specificationId,
                    batchSize, storageCondition, sampleOrientation, schedulePeriod,
                    companyId, branchId, userDetails.getUsername()
            );

            return ResponseEntity.ok().body(new java.util.HashMap<String, Object>() {{
                put("success", true);
                put("message", "Report uploaded successfully");
                put("reportId", report.getId());
            }});
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new java.util.HashMap<String, Object>() {{
                put("success", false);
                put("message", "Error processing upload: " + e.getMessage());
            }});
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'USER', 'QC', 'QA')")
    public ResponseEntity<PageResponse<Report>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        PageResponse<Report> reports;
        if (isAdmin) {
            reports = reportService.getAllReportsPaginated(page, size, sortBy, sortDirection);
        } else {
            reports = reportService.getReportsByBranchPaginated(userDetails.getBranchId(), page, size, sortBy, sortDirection);
        }

        return ResponseEntity.ok(reports);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'USER', 'QC', 'QA')")
    public ResponseEntity<Report> getReportById(@PathVariable Long id) {
        Report report = reportService.getReportById(id);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/product-names")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'USER', 'QC', 'QA')")
    public ResponseEntity<List<String>> getProductNames(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<String> productNames;
        if (isAdmin) {
            productNames = reportService.getDistinctProductNames(null);
        } else {
            productNames = reportService.getDistinctProductNames(userDetails.getBranchId());
        }

        return ResponseEntity.ok(productNames);
    }

    @PutMapping("/update-field")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'QC', 'QA')")
    public ResponseEntity<?> updateField(@RequestBody UpdateFieldRequest request) {
        try {
            Report report = reportService.updateField(request);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/history/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'QC', 'QA')")
    public ResponseEntity<List<ChangeHistory>> getChangeHistory(
            @PathVariable String entityType,
            @PathVariable Long entityId
    ) {
        List<ChangeHistory> history = reportService.getChangeHistory(entityType, entityId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/history/by-product")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'QC', 'QA')")
    public ResponseEntity<?> getChangeHistoryByProduct(@RequestParam String productName) {
        try {
            var history = reportService.getChangeHistoryByProduct(productName);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/analytics/batch-numbers")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'USER', 'QC', 'QA')")
    public ResponseEntity<List<String>> getBatchNumbersByProduct(@RequestParam String productName) {
        List<String> batchNumbers = reportService.getBatchNumbersByProduct(productName);
        return ResponseEntity.ok(batchNumbers);
    }

    @GetMapping("/analytics/storage-conditions")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'USER', 'QC', 'QA')")
    public ResponseEntity<List<String>> getStorageConditionsByProduct(@RequestParam String productName) {
        List<String> storageConditions = reportService.getStorageConditionsByProduct(productName);
        return ResponseEntity.ok(storageConditions);
    }

    @GetMapping("/analytics/filter-options")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'USER', 'QC', 'QA')")
    public ResponseEntity<?> getFilterOptions(@RequestParam String productName) {
        try {
            return ResponseEntity.ok(reportService.getFilterOptions(productName));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/analytics/compare")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'USER', 'QC', 'QA')")
    public ResponseEntity<?> compareReports(
            @RequestParam String productName,
            @RequestParam(required = false) List<String> batchNumbers,
            @RequestParam(required = false) String storageCondition,
            @RequestParam(required = false) String testName,
            @RequestParam(required = false) String specification,
            @RequestParam(required = false) String market,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String packType,
            @RequestParam(required = false) String packValue,
            @RequestParam(required = false) List<String> stations,
            @RequestParam(required = false, defaultValue = "false") boolean invert
    ) {
        try {
            return ResponseEntity.ok(reportService.compareReports(
                    productName, batchNumbers, storageCondition, testName, specification,
                    market, position, packType, packValue, stations, invert
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/analytics/prediction")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'USER', 'QC', 'QA')")
    public ResponseEntity<?> getPrediction(
            @RequestParam String productName,
            @RequestParam(required = false) String storageCondition
    ) {
        try {
            return ResponseEntity.ok(reportService.getPrediction(productName, storageCondition));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/upload-evidence")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'QC', 'QA')")
    public ResponseEntity<?> uploadEvidence(@RequestParam("file") MultipartFile file) {
        try {
            String filePath = reportService.uploadEvidenceDocument(file);
            return ResponseEntity.ok(java.util.Map.of(
                "fileName", file.getOriginalFilename(),
                "filePath", filePath
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error uploading evidence: " + e.getMessage());
        }
    }

    @GetMapping("/evidence-documents/{filename}")
    public ResponseEntity<?> downloadEvidence(@PathVariable String filename) {
        try {
            String uploadDir = System.getProperty("java.io.tmpdir") + "/evidence-documents/";
            Path filePath = Paths.get(uploadDir + filename);

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error downloading evidence: " + e.getMessage());
        }
    }

    @GetMapping("/pdf/view/{reportId}")
    public ResponseEntity<?> viewPdf(@PathVariable Long reportId) {
        try {
            com.stability.coareport.entity.Report report = reportService.getReportById(reportId);
            if (report == null) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = Paths.get(report.getPdfFilePath());

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + report.getPdfFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error viewing PDF: " + e.getMessage());
        }
    }

    @GetMapping("/pdf/download/{reportId}")
    public ResponseEntity<?> downloadPdf(@PathVariable Long reportId) {
        try {
            com.stability.coareport.entity.Report report = reportService.getReportById(reportId);
            if (report == null) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = Paths.get(report.getPdfFilePath());

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + report.getPdfFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error downloading PDF: " + e.getMessage());
        }
    }

    @ExceptionHandler(ScannedPdfNotSupportedException.class)
    public ResponseEntity<?> handleScannedPdfException(ScannedPdfNotSupportedException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "SCANNED_PDF_NOT_SUPPORTED");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("supported", false);
        errorResponse.put("suggestion", "Please convert your scanned PDF to a digitally generated PDF or use a tool that extracts text from the original document.");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }
}
