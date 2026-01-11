package com.stability.coareport.controller;

import com.stability.coareport.dto.ApprovalRequest;
import com.stability.coareport.dto.ApprovalTrackingResponse;
import com.stability.coareport.dto.PageResponse;
import com.stability.coareport.service.ApprovalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/approvals")
@CrossOrigin(origins = "*")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping("/process")
    public ResponseEntity<?> processApproval(
            @RequestBody ApprovalRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();

            if ("approve".equalsIgnoreCase(request.getAction())) {
                approvalService.approveReport(request.getReportId(), username);
            } else if ("reject".equalsIgnoreCase(request.getAction())) {
                approvalService.rejectReport(request.getReportId(), username, request.getReason());
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid action. Must be 'approve' or 'reject'"));
            }

            Map<String, String> response = new HashMap<>();
            response.put("message", "Report " + request.getAction() + "d successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/tracking")
    public ResponseEntity<PageResponse<ApprovalTrackingResponse>> getApprovalTracking(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String specification,
            @RequestParam(required = false) String storageCondition,
            @RequestParam(required = false) String market,
            @RequestParam(required = false) String orientation,
            @RequestParam(required = false) String pack) {
        PageResponse<ApprovalTrackingResponse> tracking = approvalService.getApprovalTrackingPaginated(
                page, size, sortBy, sortDirection,
                productName, batchNo, specification, storageCondition, market, orientation, pack);
        return ResponseEntity.ok(tracking);
    }

    @GetMapping("/pending")
    public ResponseEntity<PageResponse<ApprovalTrackingResponse>> getPendingApprovals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        PageResponse<ApprovalTrackingResponse> pending = approvalService.getPendingApprovalsPaginated(
                page, size, sortBy, sortDirection);
        return ResponseEntity.ok(pending);
    }
}
