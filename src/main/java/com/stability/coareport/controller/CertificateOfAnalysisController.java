package com.stability.coareport.controller;

import com.stability.coareport.dto.CertificateOfAnalysisDto;
import com.stability.coareport.service.CertificateOfAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CertificateOfAnalysisController {

    private final CertificateOfAnalysisService coaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'QC', 'QA')")
    public ResponseEntity<CertificateOfAnalysisDto> createCertificate(@RequestBody CertificateOfAnalysisDto dto) {
        try {
            CertificateOfAnalysisDto created = coaService.createCertificate(dto);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'QC', 'QA')")
    public ResponseEntity<CertificateOfAnalysisDto> updateCertificate(
            @PathVariable Long id,
            @RequestBody CertificateOfAnalysisDto dto) {
        try {
            CertificateOfAnalysisDto updated = coaService.updateCertificate(id, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'USER', 'QC', 'QA')")
    public ResponseEntity<CertificateOfAnalysisDto> getCertificateById(@PathVariable Long id) {
        try {
            CertificateOfAnalysisDto certificate = coaService.getCertificateById(id);
            return ResponseEntity.ok(certificate);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'USER', 'QC', 'QA')")
    public ResponseEntity<List<CertificateOfAnalysisDto>> getAllCertificates() {
        List<CertificateOfAnalysisDto> certificates = coaService.getAllCertificates();
        return ResponseEntity.ok(certificates);
    }

    @GetMapping("/by-product/{productName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'USER', 'QC', 'QA')")
    public ResponseEntity<List<CertificateOfAnalysisDto>> getCertificatesByProductName(
            @PathVariable String productName) {
        List<CertificateOfAnalysisDto> certificates = coaService.getCertificatesByProductName(productName);
        return ResponseEntity.ok(certificates);
    }

    @GetMapping("/by-batch/{batchNo}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN', 'MANAGER', 'USER', 'QC', 'QA')")
    public ResponseEntity<List<CertificateOfAnalysisDto>> getCertificatesByBatchNo(
            @PathVariable String batchNo) {
        List<CertificateOfAnalysisDto> certificates = coaService.getCertificatesByBatchNo(batchNo);
        return ResponseEntity.ok(certificates);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_ADMIN')")
    public ResponseEntity<Void> deleteCertificate(@PathVariable Long id) {
        try {
            coaService.deleteCertificate(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
