package com.stability.coareport.controller;

import com.stability.coareport.dto.ProductResponse;
import com.stability.coareport.entity.Report;
import com.stability.coareport.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ReportRepository reportRepository;

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<ProductResponse>> getProductsByBranch(@PathVariable Long branchId) {
        List<Report> reports = reportRepository.findByBranchId(branchId);

        Map<String, List<Report>> productGroups = reports.stream()
                .collect(Collectors.groupingBy(r -> r.getProductName() + "|" + r.getProductCode()));

        List<ProductResponse> productResponses = productGroups.entrySet().stream()
                .map(entry -> {
                    String[] keys = entry.getKey().split("\\|");
                    String productName = keys[0];
                    String productCode = keys.length > 1 ? keys[1] : "";

                    List<Report> productReports = entry.getValue();

                    Report latestReport = productReports.stream()
                            .max(Comparator.comparing(r -> r.getCreatedAt() != null ? r.getCreatedAt() : java.time.LocalDateTime.MIN))
                            .orElse(null);

                    return new ProductResponse(
                            productName,
                            productCode,
                            (long) productReports.size(),
                            latestReport != null ? latestReport.getBatchNo() : null,
                            latestReport != null ? latestReport.getReceivedDate() : null
                    );
                })
                .sorted(Comparator.comparing(ProductResponse::getProductName))
                .collect(Collectors.toList());

        return ResponseEntity.ok(productResponses);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<Report> reports = reportRepository.findAll();

        Map<String, List<Report>> productGroups = reports.stream()
                .collect(Collectors.groupingBy(r -> r.getProductName() + "|" + r.getProductCode()));

        List<ProductResponse> productResponses = productGroups.entrySet().stream()
                .map(entry -> {
                    String[] keys = entry.getKey().split("\\|");
                    String productName = keys[0];
                    String productCode = keys.length > 1 ? keys[1] : "";

                    List<Report> productReports = entry.getValue();

                    Report latestReport = productReports.stream()
                            .max(Comparator.comparing(r -> r.getCreatedAt() != null ? r.getCreatedAt() : java.time.LocalDateTime.MIN))
                            .orElse(null);

                    return new ProductResponse(
                            productName,
                            productCode,
                            (long) productReports.size(),
                            latestReport != null ? latestReport.getBatchNo() : null,
                            latestReport != null ? latestReport.getReceivedDate() : null
                    );
                })
                .sorted(Comparator.comparing(ProductResponse::getProductName))
                .collect(Collectors.toList());

        return ResponseEntity.ok(productResponses);
    }

    @GetMapping("/names/branch/{branchId}")
    public ResponseEntity<List<String>> getProductNamesByBranch(@PathVariable Long branchId) {
        List<String> productNames = reportRepository.findDistinctProductNamesByBranchId(branchId);
        Collections.sort(productNames);
        return ResponseEntity.ok(productNames);
    }

    @GetMapping("/names")
    public ResponseEntity<List<String>> getAllProductNames() {
        List<String> productNames = reportRepository.findDistinctProductNames();
        Collections.sort(productNames);
        return ResponseEntity.ok(productNames);
    }
}
