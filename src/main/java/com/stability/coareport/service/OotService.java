package com.stability.coareport.service;

import com.stability.coareport.dto.OotAnalysisResponse;
import com.stability.coareport.dto.OotConfigurationDto;
import com.stability.coareport.dto.OotGraphsDataResponse;
import com.stability.coareport.dto.OotJustificationRequest;
import com.stability.coareport.dto.OosAnalysisResponse;
import com.stability.coareport.dto.OosParetoResponse;
import com.stability.coareport.entity.*;
import com.stability.coareport.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OotService {

    private final ProductOotConfigurationRepository ootConfigRepository;
    private final BatchOotOverrideRepository batchOotOverrideRepository;
    private final ReportRepository reportRepository;
    private final TestResultRepository testResultRepository;
    private final ProductRepository productRepository;

    @Transactional
    public OotConfigurationDto createOotConfiguration(OotConfigurationDto dto) {
        ProductOotConfiguration config = new ProductOotConfiguration();
        config.setProductId(dto.getProductId());
        config.setSno(dto.getSno());
        config.setTestName(dto.getTestName());
        config.setSpecification(dto.getSpecification());
        config.setOotLowerLimit(dto.getOotLowerLimit());
        config.setOotUpperLimit(dto.getOotUpperLimit());
        config.setPercentageThreshold(dto.getPercentageThreshold());
        config.setSpecLowerLimit(dto.getSpecLowerLimit());
        config.setSpecUpperLimit(dto.getSpecUpperLimit());
        config.setSpecUnit(dto.getSpecUnit());
        config.setTargetValue(dto.getTargetValue());
        config.setLimsIntegrationEnabled(dto.getLimsIntegrationEnabled());
        config.setLimsSpecId(dto.getLimsSpecId());
        config.setIsActive(true);
        config.setApprovalStatus("PENDING");

        config = ootConfigRepository.save(config);
        return mapToDto(config);
    }

    @Transactional
    public OotConfigurationDto updateOotConfiguration(Long id, OotConfigurationDto dto) {
        ProductOotConfiguration config = ootConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("OOT configuration not found"));

        config.setSno(dto.getSno());
        config.setTestName(dto.getTestName());
        config.setSpecification(dto.getSpecification());
        config.setOotLowerLimit(dto.getOotLowerLimit());
        config.setOotUpperLimit(dto.getOotUpperLimit());
        config.setPercentageThreshold(dto.getPercentageThreshold());
        config.setSpecLowerLimit(dto.getSpecLowerLimit());
        config.setSpecUpperLimit(dto.getSpecUpperLimit());
        config.setSpecUnit(dto.getSpecUnit());
        config.setTargetValue(dto.getTargetValue());
        config.setLimsIntegrationEnabled(dto.getLimsIntegrationEnabled());
        config.setLimsSpecId(dto.getLimsSpecId());
        if (dto.getLimsIntegrationEnabled() != null && dto.getLimsIntegrationEnabled()) {
            config.setLastLimsSync(LocalDateTime.now());
        }
        config.setIsActive(dto.getIsActive());

        config = ootConfigRepository.save(config);
        return mapToDto(config);
    }

    @Transactional
    public void deleteOotConfiguration(Long id) {
        ootConfigRepository.deleteById(id);
    }

    public List<OotConfigurationDto> getOotConfigurationsByProduct(Long productId) {
        return ootConfigRepository.findByProductId(productId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void approveOotConfiguration(Long id, String approvedBy) {
        ProductOotConfiguration config = ootConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("OOT configuration not found"));

        config.setApprovalStatus("APPROVED");
        config.setApprovedBy(approvedBy);
        config.setApprovedAt(LocalDateTime.now());

        ootConfigRepository.save(config);
    }

    @Transactional
    public OotAnalysisResponse performOotAnalysis(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        Product product = productRepository.findByProductName(report.getProductName())
                .orElse(null);

        if (product == null) {
            throw new RuntimeException("Product not found for: " + report.getProductName());
        }

        List<ProductOotConfiguration> ootConfigs = ootConfigRepository
                .findByProductIdAndIsActiveTrue(product.getId());

        Map<String, BatchOotOverride> batchOverrides = batchOotOverrideRepository
                .findByReportId(reportId).stream()
                .collect(Collectors.toMap(BatchOotOverride::getTestName, o -> o));

        Report previousReport = findPreviousPeriodReport(report);

        Map<String, TestResult> previousTestResults = new HashMap<>();
        if (previousReport != null) {
            previousTestResults = previousReport.getTestResults().stream()
                    .collect(Collectors.toMap(TestResult::getTest, tr -> tr));
        }

        List<OotAnalysisResponse.OotTestResult> ootTestResults = new ArrayList<>();

        for (TestResult testResult : report.getTestResults()) {
            ProductOotConfiguration config = ootConfigs.stream()
                    .filter(c -> c.getTestName().equalsIgnoreCase(testResult.getTest()))
                    .findFirst()
                    .orElse(null);

            BatchOotOverride override = batchOverrides.get(testResult.getTest());

            BigDecimal lowerLimit = null;
            BigDecimal upperLimit = null;
            BigDecimal percentageThreshold = BigDecimal.valueOf(10.00);

            if (override != null) {
                lowerLimit = override.getOotLowerLimit();
                upperLimit = override.getOotUpperLimit();
                percentageThreshold = override.getPercentageThreshold();
            } else if (config != null) {
                lowerLimit = config.getOotLowerLimit();
                upperLimit = config.getOotUpperLimit();
                percentageThreshold = config.getPercentageThreshold();
            }

            BigDecimal currentValue = extractNumericValue(testResult.getResult());
            TestResult previousTestResult = previousTestResults.get(testResult.getTest());
            BigDecimal previousValue = previousTestResult != null ?
                    extractNumericValue(previousTestResult.getResult()) : null;

            BigDecimal percentageChange = null;
            if (currentValue != null && previousValue != null && previousValue.compareTo(BigDecimal.ZERO) != 0) {
                percentageChange = currentValue.subtract(previousValue)
                        .divide(previousValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }

            String ootStatus = calculateOotStatus(currentValue, previousValue, percentageChange,
                    lowerLimit, upperLimit, percentageThreshold);

            String colorCode = getColorCode(percentageChange);

            testResult.setOotStatus(ootStatus);
            testResult.setPercentageChange(percentageChange != null ? percentageChange.abs() : null);
            testResult.setPreviousPeriodValue(previousValue);
            if (previousReport != null) {
                testResult.setPreviousPeriodDate(previousReport.getSchedulePeriod());
            }

            testResultRepository.save(testResult);

            OotAnalysisResponse.OotTestResult ootTest = new OotAnalysisResponse.OotTestResult();
            ootTest.setTestResultId(testResult.getId());
            ootTest.setSno(testResult.getSNo());
            ootTest.setTestName(testResult.getTest());
            ootTest.setSpecification(testResult.getSpecification());
            ootTest.setCurrentValue(currentValue);
            ootTest.setPreviousValue(previousValue);
            ootTest.setPreviousPeriodDate(previousReport != null ? previousReport.getSchedulePeriod() : null);
            ootTest.setPercentageChange(percentageChange);
            ootTest.setOotStatus(ootStatus);
            ootTest.setOotLowerLimit(lowerLimit);
            ootTest.setOotUpperLimit(upperLimit);
            ootTest.setPercentageThreshold(percentageThreshold);
            ootTest.setOotJustification(testResult.getOotJustification());
            ootTest.setOotJustifiedBy(testResult.getOotJustifiedBy());
            ootTest.setColorCode(colorCode);
            ootTest.setRequiresJustification("OOT".equals(ootStatus) || "OOT_WARNING".equals(ootStatus));

            ootTestResults.add(ootTest);
        }

        OotAnalysisResponse response = new OotAnalysisResponse();
        response.setReportId(reportId);
        response.setProductName(report.getProductName());
        response.setBatchNo(report.getBatchNo());
        response.setStorageCondition(report.getStorageCondition());
        response.setSchedulePeriod(report.getSchedulePeriod());
        response.setPreviousSchedulePeriod(previousReport != null ? previousReport.getSchedulePeriod() : null);
        response.setTestResults(ootTestResults);

        return response;
    }

    @Transactional
    public void submitOotJustifications(OotJustificationRequest request, String justifiedBy) {
        for (OotJustificationRequest.TestJustification justification : request.getTestJustifications()) {
            TestResult testResult = testResultRepository.findById(justification.getTestResultId())
                    .orElseThrow(() -> new RuntimeException("Test result not found"));

            testResult.setOotJustification(justification.getJustification());
            testResult.setOotJustifiedBy(justifiedBy);
            testResult.setOotJustifiedAt(LocalDateTime.now());

            if ("OOT".equals(testResult.getOotStatus())) {
                testResult.setOotStatus("OOT_JUSTIFIED");
            }

            testResultRepository.save(testResult);
        }
    }

    private Report findPreviousPeriodReport(Report currentReport) {
        Integer currentMonths = parseSchedulePeriodToMonths(currentReport.getSchedulePeriod());
        if (currentMonths == null || currentMonths == 0) {
            return null;
        }

        List<Report> reports = reportRepository.findByProductNameAndBatchNoAndStorageCondition(
                currentReport.getProductName(),
                currentReport.getBatchNo(),
                currentReport.getStorageCondition()
        );

        Report closestReport = null;
        Integer closestMonths = null;

        for (Report report : reports) {
            if (report.getId().equals(currentReport.getId())) {
                continue;
            }

            Integer months = parseSchedulePeriodToMonths(report.getSchedulePeriod());
            if (months != null && months < currentMonths) {
                if (closestMonths == null || months > closestMonths) {
                    closestMonths = months;
                    closestReport = report;
                }
            }
        }

        return closestReport;
    }

    private Integer parseSchedulePeriodToMonths(String schedulePeriod) {
        if (schedulePeriod == null || schedulePeriod.isEmpty()) {
            return null;
        }

        String normalized = schedulePeriod.trim().toLowerCase();

        if (normalized.equals("initial") || normalized.equals("initially")) {
            return 0;
        }

        try {
            String numberPart = normalized.replaceAll("[^0-9]", "");
            if (!numberPart.isEmpty()) {
                return Integer.parseInt(numberPart);
            }
        } catch (NumberFormatException e) {
            log.warn("Could not parse schedule period: {}", schedulePeriod);
        }

        return null;
    }

    private String calculateOotStatus(BigDecimal currentValue, BigDecimal previousValue,
                                       BigDecimal percentageChange, BigDecimal lowerLimit,
                                       BigDecimal upperLimit, BigDecimal percentageThreshold) {
        if (currentValue == null) {
            return "PENDING";
        }

        if (lowerLimit != null && currentValue.compareTo(lowerLimit) < 0) {
            return "OOT";
        }

        if (upperLimit != null && currentValue.compareTo(upperLimit) > 0) {
            return "OOT";
        }

        if (percentageChange != null && percentageThreshold != null) {
            BigDecimal absChange = percentageChange.abs();

            if (absChange.compareTo(percentageThreshold) > 0) {
                return "OOT";
            }

            BigDecimal warningThreshold = percentageThreshold.multiply(BigDecimal.valueOf(0.9));
            if (absChange.compareTo(warningThreshold) > 0) {
                return "OOT_WARNING";
            }
        }

        return "IN_TREND";
    }

    private String getColorCode(BigDecimal percentageChange) {
        if (percentageChange == null) {
            return "#9e9e9e";
        }

        BigDecimal absChange = percentageChange.abs();

        if (absChange.compareTo(BigDecimal.valueOf(5)) < 0) {
            return "#4caf50";
        } else if (absChange.compareTo(BigDecimal.valueOf(10)) < 0) {
            return "#8bc34a";
        } else if (absChange.compareTo(BigDecimal.valueOf(15)) < 0) {
            return "#ffeb3b";
        } else if (absChange.compareTo(BigDecimal.valueOf(20)) < 0) {
            return "#ff9800";
        } else {
            return "#d32f2f";
        }
    }

    private BigDecimal extractNumericValue(String result) {
        if (result == null || result.isEmpty()) {
            return null;
        }

        try {
            String cleaned = result.replaceAll("[^0-9.\\-]", "").trim();
            if (!cleaned.isEmpty()) {
                return new BigDecimal(cleaned);
            }
        } catch (NumberFormatException e) {
            log.warn("Could not extract numeric value from: {}", result);
        }

        return null;
    }

    private OotConfigurationDto mapToDto(ProductOotConfiguration config) {
        OotConfigurationDto dto = new OotConfigurationDto();
        dto.setId(config.getId());
        dto.setProductId(config.getProductId());
        dto.setSno(config.getSno());
        dto.setTestName(config.getTestName());
        dto.setSpecification(config.getSpecification());
        dto.setOotLowerLimit(config.getOotLowerLimit());
        dto.setOotUpperLimit(config.getOotUpperLimit());
        dto.setPercentageThreshold(config.getPercentageThreshold());
        dto.setSpecLowerLimit(config.getSpecLowerLimit());
        dto.setSpecUpperLimit(config.getSpecUpperLimit());
        dto.setSpecUnit(config.getSpecUnit());
        dto.setTargetValue(config.getTargetValue());
        dto.setLimsIntegrationEnabled(config.getLimsIntegrationEnabled());
        dto.setLimsSpecId(config.getLimsSpecId());
        dto.setLastLimsSync(config.getLastLimsSync());
        dto.setIsActive(config.getIsActive());
        dto.setApprovalStatus(config.getApprovalStatus());
        dto.setApprovedBy(config.getApprovedBy());
        dto.setApprovedAt(config.getApprovedAt());
        dto.setCreatedBy(config.getCreatedBy());
        dto.setCreatedAt(config.getCreatedAt());
        dto.setUpdatedBy(config.getUpdatedBy());
        dto.setUpdatedAt(config.getUpdatedAt());
        return dto;
    }

    public OotGraphsDataResponse getOotGraphsData(Long productId, String batchNo) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        List<ProductOotConfiguration> ootConfigs = ootConfigRepository
                .findByProductIdAndIsActiveTrue(productId);

        if (ootConfigs.isEmpty()) {
            throw new RuntimeException("No OOT configuration found for this product");
        }

        List<Report> reports = reportRepository.findByProductNameAndBatchNo(
                product.getProductName(), batchNo);

        if (reports.isEmpty()) {
            throw new RuntimeException("No reports found for this product and batch");
        }

        String storageCondition = reports.get(0).getStorageCondition();

        List<OotGraphsDataResponse.OotConfigForGraph> configList = ootConfigs.stream()
                .map(config -> new OotGraphsDataResponse.OotConfigForGraph(
                        config.getTestName(),
                        config.getSpecification(),
                        config.getSpecLowerLimit(),
                        config.getSpecUpperLimit(),
                        config.getSpecUnit(),
                        config.getTargetValue(),
                        config.getOotLowerLimit(),
                        config.getOotUpperLimit(),
                        config.getPercentageThreshold()
                ))
                .collect(Collectors.toList());

        Map<String, List<OotGraphsDataResponse.TestTrendData>> testTrends = new HashMap<>();
        Map<String, List<OotGraphsDataResponse.PercentChangeData>> percentChanges = new HashMap<>();

        for (ProductOotConfiguration config : ootConfigs) {
            List<TestResult> testResults = new ArrayList<>();
            for (Report report : reports) {
                List<TestResult> reportTests = testResultRepository.findByReportIdAndTestNameContainingIgnoreCase(
                        report.getId(), config.getTestName());
                testResults.addAll(reportTests);
            }

            testResults.sort(Comparator.comparing(tr -> {
                Report r = reportRepository.findById(tr.getReport().getId()).orElse(null);
                return r != null ? r.getSchedulePeriod() : "";
            }));

            List<OotGraphsDataResponse.TestTrendData> trendData = testResults.stream()
                    .map(tr -> {
                        Report r = reportRepository.findById(tr.getReport().getId()).orElse(null);
                        BigDecimal value = extractNumericValue(tr.getResult());
                        return new OotGraphsDataResponse.TestTrendData(
                                r != null ? r.getSchedulePeriod() : "",
                                value,
                                tr.getOotStatus(),
                                false,
                                null,
                                null
                        );
                    })
                    .filter(td -> td.getValue() != null)
                    .collect(Collectors.toList());

            testTrends.put(config.getTestName(), trendData);

            List<OotGraphsDataResponse.PercentChangeData> percentData = new ArrayList<>();
            for (int i = 1; i < trendData.size(); i++) {
                OotGraphsDataResponse.TestTrendData current = trendData.get(i);
                OotGraphsDataResponse.TestTrendData previous = trendData.get(i - 1);

                if (current.getValue() != null && previous.getValue() != null &&
                    previous.getValue().compareTo(BigDecimal.ZERO) != 0) {

                    BigDecimal change = current.getValue().subtract(previous.getValue());
                    BigDecimal percentChange = change.divide(previous.getValue(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));

                    percentData.add(new OotGraphsDataResponse.PercentChangeData(
                            previous.getPeriod() + " → " + current.getPeriod(),
                            percentChange,
                            previous.getPeriod(),
                            current.getPeriod(),
                            current.getValue(),
                            previous.getValue()
                    ));
                }
            }

            percentChanges.put(config.getTestName(), percentData);
        }

        return new OotGraphsDataResponse(
                product.getProductName(),
                batchNo,
                storageCondition,
                configList,
                testTrends,
                percentChanges
        );
    }

    @Transactional
    public OosAnalysisResponse performOosAnalysis(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        Product product = productRepository.findByProductName(report.getProductName())
                .orElseThrow(() -> new RuntimeException("Product not found for: " + report.getProductName()));

        List<ProductOotConfiguration> ootConfigs = ootConfigRepository
                .findByProductIdAndIsActiveTrue(product.getId());

        if (ootConfigs.isEmpty()) {
            throw new RuntimeException("No specification limits configured for this product");
        }

        Map<String, ProductOotConfiguration> configMap = ootConfigs.stream()
                .collect(Collectors.toMap(
                        ProductOotConfiguration::getTestName,
                        config -> config,
                        (existing, replacement) -> existing
                ));

        List<TestResult> testResults = testResultRepository.findByReportId(reportId);
        List<OosAnalysisResponse.OosTestResult> oosResults = new ArrayList<>();

        int inSpecCount = 0;
        int warningCount = 0;
        int oosCount = 0;
        int justifiedCount = 0;

        for (TestResult testResult : testResults) {
            ProductOotConfiguration config = configMap.get(testResult.getTest());

            if (config == null || config.getSpecLowerLimit() == null || config.getSpecUpperLimit() == null) {
                continue;
            }

            BigDecimal value = extractNumericValue(testResult.getResult());
            if (value == null) {
                continue;
            }

            BigDecimal specLower = config.getSpecLowerLimit();
            BigDecimal specUpper = config.getSpecUpperLimit();
            BigDecimal specDistancePercent = calculateSpecDistance(value, specLower, specUpper);

            String oosStatus;
            boolean requiresJustification = false;
            String colorCode;

            if (value.compareTo(specLower) < 0 || value.compareTo(specUpper) > 0) {
                if (testResult.getOosJustification() != null && !testResult.getOosJustification().isEmpty()) {
                    oosStatus = "OOS_JUSTIFIED";
                    justifiedCount++;
                    colorCode = "#2196f3";
                } else {
                    oosStatus = "OOS";
                    requiresJustification = true;
                    oosCount++;
                    colorCode = "#d32f2f";
                }
            } else if (specDistancePercent.compareTo(BigDecimal.valueOf(5)) < 0) {
                oosStatus = "OOS_WARNING";
                warningCount++;
                colorCode = "#ff9800";
            } else {
                oosStatus = "IN_SPEC";
                inSpecCount++;
                colorCode = "#4caf50";
            }

            testResult.setOosStatus(oosStatus);
            testResult.setSpecDistancePercent(specDistancePercent);
            testResultRepository.save(testResult);

            oosResults.add(new OosAnalysisResponse.OosTestResult(
                    testResult.getId(),
                    testResult.getSNo(),
                    testResult.getTest(),
                    testResult.getSpecification(),
                    value,
                    specLower,
                    specUpper,
                    config.getSpecUnit(),
                    config.getTargetValue(),
                    oosStatus,
                    specDistancePercent,
                    requiresJustification,
                    testResult.getOosJustification(),
                    testResult.getOosJustifiedBy(),
                    colorCode
            ));
        }

        int totalTests = oosResults.size();
        double oosRate = totalTests > 0 ? (double) oosCount / totalTests * 100 : 0;

        OosAnalysisResponse.OosSummary summary = new OosAnalysisResponse.OosSummary(
                totalTests, inSpecCount, warningCount, oosCount, justifiedCount, oosRate
        );

        return new OosAnalysisResponse(
                report.getProductName(),
                report.getBatchNo(),
                report.getStorageCondition(),
                report.getSchedulePeriod(),
                oosResults,
                summary
        );
    }

    public OosParetoResponse getOosPareto(Long productId, int days) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        List<Report> reports = reportRepository.findByProductName(product.getProductName());
        reports = reports.stream()
                .filter(r -> r.getCreatedAt().isAfter(startDate))
                .collect(Collectors.toList());

        Map<String, Integer> oosCountByTest = new HashMap<>();
        Map<String, Integer> totalCountByTest = new HashMap<>();

        for (Report report : reports) {
            List<TestResult> testResults = testResultRepository.findByReportId(report.getId());

            for (TestResult tr : testResults) {
                String testName = tr.getTest();
                totalCountByTest.put(testName, totalCountByTest.getOrDefault(testName, 0) + 1);

                if ("OOS".equals(tr.getOosStatus()) || "OOS_JUSTIFIED".equals(tr.getOosStatus())) {
                    oosCountByTest.put(testName, oosCountByTest.getOrDefault(testName, 0) + 1);
                }
            }
        }

        List<OosParetoResponse.ParetoItem> paretoItems = new ArrayList<>();
        int totalOosCount = oosCountByTest.values().stream().mapToInt(Integer::intValue).sum();

        List<Map.Entry<String, Integer>> sortedEntries = oosCountByTest.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .collect(Collectors.toList());

        double cumulativeCount = 0;
        for (Map.Entry<String, Integer> entry : sortedEntries) {
            String testName = entry.getKey();
            int oosCount = entry.getValue();
            int totalCount = totalCountByTest.get(testName);
            double oosRate = (double) oosCount / totalCount * 100;

            cumulativeCount += oosCount;
            double cumulativePercent = (cumulativeCount / totalOosCount) * 100;

            paretoItems.add(new OosParetoResponse.ParetoItem(
                    testName, oosCount, totalCount, oosRate, cumulativePercent
            ));
        }

        String timeRange = days + " days";
        return new OosParetoResponse(product.getProductName(), timeRange, paretoItems, totalOosCount);
    }

    private BigDecimal calculateSpecDistance(BigDecimal value, BigDecimal specLower, BigDecimal specUpper) {
        BigDecimal specRange = specUpper.subtract(specLower);
        BigDecimal midpoint = specLower.add(specUpper).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);

        if (value.compareTo(specLower) < 0) {
            BigDecimal distanceFromLower = specLower.subtract(value);
            return distanceFromLower.divide(specRange, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(-100));
        } else if (value.compareTo(specUpper) > 0) {
            BigDecimal distanceFromUpper = value.subtract(specUpper);
            return distanceFromUpper.divide(specRange, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(-100));
        } else {
            BigDecimal distanceFromMidpoint = value.subtract(midpoint).abs();
            BigDecimal maxDistance = specRange.divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
            BigDecimal relativeDistance = distanceFromMidpoint.divide(maxDistance, 4, RoundingMode.HALF_UP);
            return BigDecimal.valueOf(100).subtract(relativeDistance.multiply(BigDecimal.valueOf(100)));
        }
    }
}
