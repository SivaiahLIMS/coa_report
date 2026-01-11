package com.stability.coareport.controller;

import com.stability.coareport.dto.OotAnalysisResponse;
import com.stability.coareport.dto.OotConfigurationDto;
import com.stability.coareport.dto.OotJustificationRequest;
import com.stability.coareport.dto.OosAnalysisResponse;
import com.stability.coareport.dto.OosParetoResponse;
import com.stability.coareport.security.UserDetailsImpl;
import com.stability.coareport.service.OotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/oot")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OotController {

    private final OotService ootService;

    @PostMapping("/configuration")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'QA')")
    public ResponseEntity<OotConfigurationDto> createOotConfiguration(
            @RequestBody OotConfigurationDto dto,
            Authentication authentication) {
        try {
            OotConfigurationDto created = ootService.createOotConfiguration(dto);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/configuration/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'QA')")
    public ResponseEntity<OotConfigurationDto> updateOotConfiguration(
            @PathVariable Long id,
            @RequestBody OotConfigurationDto dto,
            Authentication authentication) {
        try {
            OotConfigurationDto updated = ootService.updateOotConfiguration(id, dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/configuration/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'QA')")
    public ResponseEntity<Void> deleteOotConfiguration(@PathVariable Long id) {
        try {
            ootService.deleteOotConfiguration(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/configuration/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'QA', 'QC')")
    public ResponseEntity<List<OotConfigurationDto>> getOotConfigurationsByProduct(@PathVariable Long productId) {
        try {
            List<OotConfigurationDto> configs = ootService.getOotConfigurationsByProduct(productId);
            return ResponseEntity.ok(configs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/configuration/{id}/approve")
    @PreAuthorize("hasRole('QA')")
    public ResponseEntity<Void> approveOotConfiguration(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            ootService.approveOotConfiguration(id, userDetails.getUsername());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/analysis/{reportId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'QA', 'QC')")
    public ResponseEntity<?> performOotAnalysis(@PathVariable Long reportId) {
        try {
            OotAnalysisResponse analysis = ootService.performOotAnalysis(reportId);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/justification")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'QA', 'QC')")
    public ResponseEntity<?> submitOotJustifications(
            @RequestBody OotJustificationRequest request,
            Authentication authentication) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            ootService.submitOotJustifications(request, userDetails.getUsername());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/graphs/{productId}/{batchNo}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'QA', 'QC')")
    public ResponseEntity<?> getOotGraphsData(
            @PathVariable Long productId,
            @PathVariable String batchNo) {
        try {
            var graphsData = ootService.getOotGraphsData(productId, batchNo);
            return ResponseEntity.ok(graphsData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/oos/analysis/{reportId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'QA', 'QC')")
    public ResponseEntity<?> performOosAnalysis(@PathVariable Long reportId) {
        try {
            OosAnalysisResponse analysis = ootService.performOosAnalysis(reportId);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/oos/pareto/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'QA', 'QC')")
    public ResponseEntity<?> getOosPareto(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "90") int days) {
        try {
            OosParetoResponse paretoData = ootService.getOosPareto(productId, days);
            return ResponseEntity.ok(paretoData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
