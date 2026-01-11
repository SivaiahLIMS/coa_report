package com.stability.coareport.service;

import com.stability.coareport.dto.StabilityFilterOptionsResponse;
import com.stability.coareport.dto.StabilityReportRequest;
import com.stability.coareport.dto.StabilityReportResponse;
import com.stability.coareport.entity.Report;
import com.stability.coareport.entity.TestResult;
import com.stability.coareport.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StabilityReportService {

    private final ReportRepository reportRepository;

    /**
     * Natural sort comparator for S.No values like "1", "2", "10", "3.1", "3.2", etc.
     * Handles both integer (1, 2, 10) and decimal (3.1, 3.2) formats.
     */
    private static final Comparator<String> SNO_COMPARATOR = (s1, s2) -> {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return 1;
        if (s2 == null) return -1;
        if (s1.isEmpty() && s2.isEmpty()) return 0;
        if (s1.isEmpty()) return 1;
        if (s2.isEmpty()) return -1;

        // Split by dot to handle decimal S.No like "3.1", "3.2"
        String[] parts1 = s1.split("\\.");
        String[] parts2 = s2.split("\\.");

        // Compare each part numerically
        int minLength = Math.min(parts1.length, parts2.length);
        for (int i = 0; i < minLength; i++) {
            try {
                double num1 = Double.parseDouble(parts1[i]);
                double num2 = Double.parseDouble(parts2[i]);
                int cmp = Double.compare(num1, num2);
                if (cmp != 0) return cmp;
            } catch (NumberFormatException e) {
                // If not numeric, fall back to string comparison
                int cmp = parts1[i].compareTo(parts2[i]);
                if (cmp != 0) return cmp;
            }
        }

        // If all compared parts are equal, the one with fewer parts comes first
        return Integer.compare(parts1.length, parts2.length);
    };

    @Transactional(readOnly = true)
    public StabilityFilterOptionsResponse getFilterOptions() {
        List<Report> allReports = reportRepository.findAll();

        Set<String> productNames = new TreeSet<>();
        Set<String> specifications = new TreeSet<>();
        Set<String> batchNumbers = new TreeSet<>();
        Set<String> storageConditions = new TreeSet<>();
        Set<String> sampleOrientations = new TreeSet<>();
        Set<String> descriptions = new TreeSet<>();
        Set<String> schedulePeriods = new TreeSet<>(Comparator.comparingInt(this::parseSchedulePeriod));

        for (Report report : allReports) {
            if (report.getProductName() != null) productNames.add(report.getProductName());
            if (report.getSpecification() != null) specifications.add(report.getSpecification());
            if (report.getBatchNo() != null) batchNumbers.add(report.getBatchNo());
            if (report.getStorageCondition() != null) storageConditions.add(report.getStorageCondition());
            if (report.getSampleOrientation() != null) sampleOrientations.add(report.getSampleOrientation());
            if (report.getDescription() != null) descriptions.add(report.getDescription());
            if (report.getSchedulePeriod() != null) schedulePeriods.add(report.getSchedulePeriod());
        }

        return new StabilityFilterOptionsResponse(
                new ArrayList<>(productNames),
                new ArrayList<>(specifications),
                new ArrayList<>(batchNumbers),
                new ArrayList<>(storageConditions),
                new ArrayList<>(sampleOrientations),
                new ArrayList<>(descriptions),
                new ArrayList<>(schedulePeriods)
        );
    }

    @Transactional(readOnly = true)
    public StabilityFilterOptionsResponse getFilterOptionsForProduct(String productName) {
        List<Report> productReports = reportRepository.findAll().stream()
                .filter(report -> productName.equals(report.getProductName()))
                .collect(Collectors.toList());

        Set<String> specifications = new TreeSet<>();
        Set<String> batchNumbers = new TreeSet<>();
        Set<String> storageConditions = new TreeSet<>();
        Set<String> sampleOrientations = new TreeSet<>();
        Set<String> descriptions = new TreeSet<>();
        Set<String> schedulePeriods = new TreeSet<>(Comparator.comparingInt(this::parseSchedulePeriod));

        for (Report report : productReports) {
            if (report.getSpecification() != null) specifications.add(report.getSpecification());
            if (report.getBatchNo() != null) batchNumbers.add(report.getBatchNo());
            if (report.getStorageCondition() != null) storageConditions.add(report.getStorageCondition());
            if (report.getSampleOrientation() != null) sampleOrientations.add(report.getSampleOrientation());
            if (report.getDescription() != null) descriptions.add(report.getDescription());
            if (report.getSchedulePeriod() != null) schedulePeriods.add(report.getSchedulePeriod());
        }

        return new StabilityFilterOptionsResponse(
                List.of(productName),
                new ArrayList<>(specifications),
                new ArrayList<>(batchNumbers),
                new ArrayList<>(storageConditions),
                new ArrayList<>(sampleOrientations),
                new ArrayList<>(descriptions),
                new ArrayList<>(schedulePeriods)
        );
    }

    @Transactional(readOnly = true)
    public StabilityReportResponse generateStabilityReport(StabilityReportRequest request) {
        List<Report> reports = findReportsByFilters(request);

        if (reports.isEmpty()) {
            return new StabilityReportResponse(new ArrayList<>(), new ArrayList<>(), new HashMap<>(), null);
        }

        List<String> schedulePeriods = extractSchedulePeriods(reports);
        List<StabilityReportResponse.TestResultRow> testResultRows = buildTestResultRows(reports, schedulePeriods);

        Report firstReport = reports.get(0);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("productName", request.getProductName());
        metadata.put("totalReports", reports.size());
        metadata.put("analysisStartDate", firstReport.getAnalysisStartDate());
        metadata.put("analysisEndDate", firstReport.getAnalysisEndDate());
        metadata.put("packingType", firstReport.getPackingType());
        metadata.put("hdpeCapDepth", firstReport.getHdpeCapDepth());
        metadata.put("ldpeNozzleDetails", firstReport.getLdpeNozzleDetails());
        metadata.put("ldpeBottleDetails", firstReport.getLdpeBottleDetails());
        metadata.put("protocolId", firstReport.getProtocolId());
        metadata.put("remarks", firstReport.getRemarks());
        metadata.put("createdBy", firstReport.getCreatedBy());
        metadata.put("createdAt", firstReport.getCreatedAt());

        Map<String, Map<String, String>> periodAnalysisDates = new HashMap<>();
        for (Report report : reports) {
            String period = report.getSchedulePeriod();
            if (period != null && !periodAnalysisDates.containsKey(period)) {
                Map<String, String> dates = new HashMap<>();
                dates.put("startDate", report.getAnalysisStartDate());
                dates.put("endDate", report.getAnalysisEndDate());
                periodAnalysisDates.put(period, dates);
            }
        }
        metadata.put("periodAnalysisDates", periodAnalysisDates);

        return new StabilityReportResponse(schedulePeriods, testResultRows, metadata, null);
    }

    private List<Report> findReportsByFilters(StabilityReportRequest request) {
        List<Report> reports = reportRepository.findAll();

        return reports.stream()
                .filter(report -> matchesFilters(report, request))
                .sorted(Comparator.comparing(Report::getSchedulePeriod, Comparator.nullsLast(
                        Comparator.comparingInt(this::parseSchedulePeriod))))
                .collect(Collectors.toList());
    }

    private boolean matchesFilters(Report report, StabilityReportRequest request) {
        if (request.getProductName() != null && !request.getProductName().equals(report.getProductName())) {
            return false;
        }
        if (request.getSpecification() != null && !request.getSpecification().isEmpty()
                && !request.getSpecification().equals(report.getSpecification())) {
            return false;
        }
        if (request.getBatchNo() != null && !request.getBatchNo().isEmpty()
                && !request.getBatchNo().equals(report.getBatchNo())) {
            return false;
        }
        if (request.getStorageCondition() != null && !request.getStorageCondition().isEmpty()
                && !request.getStorageCondition().equals(report.getStorageCondition())) {
            return false;
        }
        if (request.getSampleOrientation() != null && !request.getSampleOrientation().isEmpty()
                && !request.getSampleOrientation().equals(report.getSampleOrientation())) {
            return false;
        }
        if (request.getDescription() != null && !request.getDescription().isEmpty()
                && !request.getDescription().equals(report.getDescription())) {
            return false;
        }

        // Cumulative period filtering: if a period is specified, include all periods up to and including that period
        if (request.getSchedulePeriod() != null && !request.getSchedulePeriod().isEmpty()) {
            int requestedPeriodValue = parseSchedulePeriod(request.getSchedulePeriod());
            int reportPeriodValue = parseSchedulePeriod(report.getSchedulePeriod());

            // Include reports from Initial up to the requested period
            if (reportPeriodValue > requestedPeriodValue) {
                return false;
            }
        }

        return true;
    }

    private List<String> extractSchedulePeriods(List<Report> reports) {
        return reports.stream()
                .map(Report::getSchedulePeriod)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparingInt(this::parseSchedulePeriod))
                .collect(Collectors.toList());
    }

    private int parseSchedulePeriod(String period) {
        if (period == null || period.isEmpty()) return Integer.MAX_VALUE;

        // Handle "Initial" as the first period (value = 0)
        if (period.toUpperCase().contains("INITIAL")) {
            return 0;
        }

        try {
            String numPart = period.replaceAll("[^0-9]", "");
            if (numPart.isEmpty()) return Integer.MAX_VALUE;

            int value = Integer.parseInt(numPart);

            if (period.toUpperCase().contains("M") && !period.toUpperCase().contains("MONTH")) {
                return value;
            } else if (period.toUpperCase().contains("Y")) {
                return value * 12;
            } else if (period.toUpperCase().contains("W")) {
                return (int) (value * 0.23);
            } else if (period.toUpperCase().contains("D")) {
                return (int) (value * 0.033);
            }
            return value;
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private List<StabilityReportResponse.TestResultRow> buildTestResultRows(List<Report> reports, List<String> schedulePeriods) {
        Map<String, StabilityReportResponse.TestResultRow> testRowMap = new LinkedHashMap<>();

        // Sort reports by schedule period to get the earliest report first (for serial number ordering)
        List<Report> sortedReports = reports.stream()
                .sorted(Comparator.comparing(Report::getSchedulePeriod, Comparator.nullsLast(
                        Comparator.comparingInt(this::parseSchedulePeriod))))
                .collect(Collectors.toList());

        // Track which S.No values are already used from the first report
        Set<String> usedSNos = new HashSet<>();

        // First pass: Initialize rows in order from the earliest report (maintains serial number order)
        if (!sortedReports.isEmpty() && sortedReports.get(0).getTestResults() != null) {
            List<TestResult> firstReportTests = sortedReports.get(0).getTestResults();
            // Sort by sNo to maintain original order
            firstReportTests.stream()
                    .filter(tr -> tr.getTest() != null && !tr.getTest().trim().isEmpty())
                    .sorted(Comparator.comparing(TestResult::getSNo, Comparator.nullsLast(SNO_COMPARATOR)))
                    .forEach(testResult -> {
                        String testName = testResult.getTest().trim();
                        if (!testRowMap.containsKey(testName)) {
                            StabilityReportResponse.TestResultRow row = new StabilityReportResponse.TestResultRow();
                            row.setSNo(testResult.getSNo());
                            row.setTestName(testName);
                            row.setSpecification("");
                            row.setSpecificationVersion("");
                            row.setPeriodResults(new LinkedHashMap<>());
                            row.setPeriodSpecifications(new LinkedHashMap<>());
                            row.setNumeric(false);
                            testRowMap.put(testName, row);

                            // Track used S.No from first report
                            if (testResult.getSNo() != null && !testResult.getSNo().trim().isEmpty()) {
                                usedSNos.add(testResult.getSNo().trim());
                            }
                        }
                    });
        }

        // Second pass: Populate data from all reports
        for (Report report : sortedReports) {
            if (report.getTestResults() == null) continue;

            String schedulePeriod = report.getSchedulePeriod();

            for (TestResult testResult : report.getTestResults()) {
                String testName = testResult.getTest();
                String specification = testResult.getSpecification() != null ? testResult.getSpecification() : "N/A";
                String result = testResult.getResult();

                if (testName == null || testName.trim().isEmpty()) continue;

                testName = testName.trim();

                // Add row if it wasn't in the first report (new test discovered in later period)
                if (!testRowMap.containsKey(testName)) {
                    StabilityReportResponse.TestResultRow row = new StabilityReportResponse.TestResultRow();

                    // Check if this is a child test (has parent in hierarchy)
                    String pdfSNo = testResult.getSNo();
                    String assignedSNo = null;

                    if (pdfSNo != null && !pdfSNo.trim().isEmpty() && pdfSNo.contains(".")) {
                        // This is a child test (e.g., "3.1", "3.2")
                        String[] parts = pdfSNo.split("\\.");
                        if (parts.length >= 2) {
                            String parentSNo = parts[0];
                            // Check if parent exists in first report
                            if (usedSNos.contains(parentSNo) && !usedSNos.contains(pdfSNo.trim())) {
                                // Parent exists and this S.No is not already used - keep it!
                                assignedSNo = pdfSNo.trim();
                                usedSNos.add(assignedSNo);
                            }
                        }
                    }

                    row.setSNo(assignedSNo);
                    row.setTestName(testName);
                    row.setSpecification("");
                    row.setSpecificationVersion("");
                    row.setPeriodResults(new LinkedHashMap<>());
                    row.setPeriodSpecifications(new LinkedHashMap<>());
                    row.setNumeric(false);
                    testRowMap.put(testName, row);
                }

                StabilityReportResponse.TestResultRow row = testRowMap.get(testName);
                if (schedulePeriod != null && !schedulePeriod.isEmpty()) {
                    row.getPeriodResults().put(schedulePeriod, result);
                    row.getPeriodSpecifications().put(schedulePeriod, specification);
                }
            }
        }

        // Now determine if each test is numeric and build chart data
        for (StabilityReportResponse.TestResultRow row : testRowMap.values()) {
            boolean hasNumericValue = false;
            List<StabilityReportResponse.ChartDataPoint> chartData = new ArrayList<>();

            for (String period : schedulePeriods) {
                String result = row.getPeriodResults().get(period);
                if (result != null) {
                    Double numericValue = extractNumericValue(result);
                    if (numericValue != null) {
                        hasNumericValue = true;
                        chartData.add(new StabilityReportResponse.ChartDataPoint(period, numericValue, result));
                    } else {
                        chartData.add(new StabilityReportResponse.ChartDataPoint(period, null, result));
                    }
                }
            }

            row.setNumeric(hasNumericValue);
            row.setChartData(chartData);
        }

        List<StabilityReportResponse.TestResultRow> resultList = new ArrayList<>();
        for (StabilityReportResponse.TestResultRow row : testRowMap.values()) {
            for (String period : schedulePeriods) {
                if (!row.getPeriodResults().containsKey(period)) {
                    row.getPeriodResults().put(period, "NA");
                }
                if (!row.getPeriodSpecifications().containsKey(period)) {
                    row.getPeriodSpecifications().put(period, "N/A");
                }
            }
            resultList.add(row);
        }

        // Sort by S.No to maintain hierarchy (3, 3.1, 3.2, etc.)
        resultList.sort(Comparator.comparing(
                StabilityReportResponse.TestResultRow::getSNo,
                Comparator.nullsLast(SNO_COMPARATOR)
        ));

        // Find the highest sequential number from existing S.No values
        int maxSequentialNo = 0;
        for (StabilityReportResponse.TestResultRow row : resultList) {
            if (row.getSNo() != null && !row.getSNo().trim().isEmpty()) {
                try {
                    // Extract integer part from S.No (e.g., "3" from "3", "3.1", "3.2")
                    String[] parts = row.getSNo().split("\\.");
                    int mainNumber = Integer.parseInt(parts[0]);
                    maxSequentialNo = Math.max(maxSequentialNo, mainNumber);
                } catch (NumberFormatException e) {
                    // Skip non-numeric S.No values
                }
            }
        }

        // Start assigning from next available number
        int sequentialNo = maxSequentialNo + 1;

        // Generate sequential S.No only for missing values (new tests from later periods)
        for (StabilityReportResponse.TestResultRow row : resultList) {
            if (row.getSNo() == null || row.getSNo().trim().isEmpty()) {
                // Assign next sequential number (avoids all conflicts)
                row.setSNo(String.valueOf(sequentialNo));
                sequentialNo++;
            }
        }

        // Final sort to ensure proper order after filling missing S.No
        resultList.sort(Comparator.comparing(
                StabilityReportResponse.TestResultRow::getSNo,
                Comparator.nullsLast(SNO_COMPARATOR)
        ));

        return resultList;
    }

    private Double extractNumericValue(String result) {
        if (result == null || result.isEmpty()) {
            return null;
        }

        try {
            String cleaned = result.replaceAll("[^0-9.\\-]", "");
            if (!cleaned.isEmpty()) {
                return Double.parseDouble(cleaned);
            }
        } catch (NumberFormatException e) {
        }

        return null;
    }

    @Transactional(readOnly = true)
    public StabilityReportResponse generateBatchComparisonReport(StabilityReportRequest request) {
        List<String> batchNumbers = request.getBatchNumbers();
        if (batchNumbers == null || batchNumbers.isEmpty()) {
            return new StabilityReportResponse(new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new ArrayList<>());
        }

        Map<String, List<Report>> batchReportsMap = new LinkedHashMap<>();
        Set<String> allSchedulePeriods = new TreeSet<>(Comparator.comparingInt(this::parseSchedulePeriod));

        for (String batchNumber : batchNumbers) {
            StabilityReportRequest batchRequest = new StabilityReportRequest();
            batchRequest.setProductName(request.getProductName());
            batchRequest.setSpecification(request.getSpecification());
            batchRequest.setBatchNo(batchNumber);
            batchRequest.setStorageCondition(request.getStorageCondition());
            batchRequest.setSampleOrientation(request.getSampleOrientation());

            List<Report> batchReports = findReportsByFilters(batchRequest);
            batchReportsMap.put(batchNumber, batchReports);

            batchReports.stream()
                    .map(Report::getSchedulePeriod)
                    .filter(Objects::nonNull)
                    .forEach(allSchedulePeriods::add);
        }

        List<String> schedulePeriods = new ArrayList<>(allSchedulePeriods);
        List<StabilityReportResponse.TestResultRow> testResultRows = buildBatchComparisonRows(batchReportsMap, schedulePeriods, batchNumbers);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("productName", request.getProductName());
        metadata.put("batchNumbers", batchNumbers);

        if (!batchReportsMap.isEmpty() && !batchReportsMap.values().iterator().next().isEmpty()) {
            Report firstReport = batchReportsMap.values().iterator().next().get(0);
            metadata.put("analysisStartDate", firstReport.getAnalysisStartDate());
            metadata.put("analysisEndDate", firstReport.getAnalysisEndDate());
            metadata.put("packingType", firstReport.getPackingType());
            metadata.put("hdpeCapDepth", firstReport.getHdpeCapDepth());
            metadata.put("ldpeNozzleDetails", firstReport.getLdpeNozzleDetails());
            metadata.put("ldpeBottleDetails", firstReport.getLdpeBottleDetails());
            metadata.put("protocolId", firstReport.getProtocolId());
            metadata.put("remarks", firstReport.getRemarks());
            metadata.put("createdBy", firstReport.getCreatedBy());
            metadata.put("createdAt", firstReport.getCreatedAt());
        }

        Map<String, Map<String, Map<String, String>>> batchPeriodAnalysisDates = new HashMap<>();
        for (Map.Entry<String, List<Report>> entry : batchReportsMap.entrySet()) {
            String batchNumber = entry.getKey();
            Map<String, Map<String, String>> periodDates = new HashMap<>();

            for (Report report : entry.getValue()) {
                String period = report.getSchedulePeriod();
                if (period != null && !periodDates.containsKey(period)) {
                    Map<String, String> dates = new HashMap<>();
                    dates.put("startDate", report.getAnalysisStartDate());
                    dates.put("endDate", report.getAnalysisEndDate());
                    periodDates.put(period, dates);
                }
            }
            batchPeriodAnalysisDates.put(batchNumber, periodDates);
        }
        metadata.put("batchPeriodAnalysisDates", batchPeriodAnalysisDates);

        return new StabilityReportResponse(schedulePeriods, testResultRows, metadata, batchNumbers);
    }

    private List<StabilityReportResponse.TestResultRow> buildBatchComparisonRows(
            Map<String, List<Report>> batchReportsMap,
            List<String> schedulePeriods,
            List<String> batchNumbers) {

        Map<String, StabilityReportResponse.TestResultRow> testRowMap = new LinkedHashMap<>();

        // Track which S.No values are already used from the first report
        Set<String> usedSNos = new HashSet<>();

        // First pass: Initialize rows in order from the earliest report of the first batch (maintains serial number order)
        if (!batchReportsMap.isEmpty()) {
            List<Report> firstBatchReports = batchReportsMap.values().iterator().next();
            if (!firstBatchReports.isEmpty()) {
                List<Report> sortedReports = firstBatchReports.stream()
                        .sorted(Comparator.comparing(Report::getSchedulePeriod, Comparator.nullsLast(
                                Comparator.comparingInt(this::parseSchedulePeriod))))
                        .collect(Collectors.toList());

                if (!sortedReports.isEmpty() && sortedReports.get(0).getTestResults() != null) {
                    List<TestResult> firstReportTests = sortedReports.get(0).getTestResults();
                    // Sort by sNo to maintain original order
                    firstReportTests.stream()
                            .filter(tr -> tr.getTest() != null && !tr.getTest().trim().isEmpty())
                            .sorted(Comparator.comparing(TestResult::getSNo, Comparator.nullsLast(SNO_COMPARATOR)))
                            .forEach(testResult -> {
                                String testName = testResult.getTest().trim();
                                if (!testRowMap.containsKey(testName)) {
                                    StabilityReportResponse.TestResultRow row = new StabilityReportResponse.TestResultRow();
                                    row.setSNo(testResult.getSNo());
                                    row.setTestName(testName);
                                    row.setSpecification("");
                                    row.setSpecificationVersion("");
                                    row.setPeriodResults(new LinkedHashMap<>());
                                    row.setPeriodSpecifications(new LinkedHashMap<>());
                                    row.setNumeric(false);
                                    row.setBatchData(new LinkedHashMap<>());

                                    for (String batch : batchNumbers) {
                                        row.getBatchData().put(batch, new LinkedHashMap<>());
                                    }

                                    testRowMap.put(testName, row);

                                    // Track used S.No from first report
                                    if (testResult.getSNo() != null && !testResult.getSNo().trim().isEmpty()) {
                                        usedSNos.add(testResult.getSNo().trim());
                                    }
                                }
                            });
                }
            }
        }

        // Second pass: Populate data from all batch reports
        for (Map.Entry<String, List<Report>> entry : batchReportsMap.entrySet()) {
            String batchNumber = entry.getKey();
            List<Report> reports = entry.getValue();

            for (Report report : reports) {
                if (report.getTestResults() == null) continue;

                String schedulePeriod = report.getSchedulePeriod();

                for (TestResult testResult : report.getTestResults()) {
                    String testName = testResult.getTest();
                    String specification = testResult.getSpecification() != null ? testResult.getSpecification() : "N/A";
                    String result = testResult.getResult();

                    if (testName == null || testName.trim().isEmpty()) continue;

                    testName = testName.trim();

                    // Add row if it wasn't in the first report (new test discovered in later period)
                    if (!testRowMap.containsKey(testName)) {
                        StabilityReportResponse.TestResultRow row = new StabilityReportResponse.TestResultRow();

                        // Check if this is a child test (has parent in hierarchy)
                        String pdfSNo = testResult.getSNo();
                        String assignedSNo = null;

                        if (pdfSNo != null && !pdfSNo.trim().isEmpty() && pdfSNo.contains(".")) {
                            // This is a child test (e.g., "3.1", "3.2")
                            String[] parts = pdfSNo.split("\\.");
                            if (parts.length >= 2) {
                                String parentSNo = parts[0];
                                // Check if parent exists in first report
                                if (usedSNos.contains(parentSNo) && !usedSNos.contains(pdfSNo.trim())) {
                                    // Parent exists and this S.No is not already used - keep it!
                                    assignedSNo = pdfSNo.trim();
                                    usedSNos.add(assignedSNo);
                                }
                            }
                        }

                        row.setSNo(assignedSNo);
                        row.setTestName(testName);
                        row.setSpecification(specification);
                        row.setSpecificationVersion("");
                        row.setPeriodResults(new LinkedHashMap<>());
                        row.setPeriodSpecifications(new LinkedHashMap<>());
                        row.setNumeric(false);
                        row.setBatchData(new LinkedHashMap<>());

                        for (String batch : batchNumbers) {
                            row.getBatchData().put(batch, new LinkedHashMap<>());
                        }

                        testRowMap.put(testName, row);
                    }

                    StabilityReportResponse.TestResultRow row = testRowMap.get(testName);
                    if (schedulePeriod != null && !schedulePeriod.isEmpty()) {
                        row.getBatchData().get(batchNumber).put(schedulePeriod, result);
                    }
                }
            }
        }

        for (StabilityReportResponse.TestResultRow row : testRowMap.values()) {
            boolean hasNumericValue = false;

            for (String batchNumber : batchNumbers) {
                Map<String, String> batchResults = row.getBatchData().get(batchNumber);
                for (String result : batchResults.values()) {
                    if (extractNumericValue(result) != null) {
                        hasNumericValue = true;
                        break;
                    }
                }
                if (hasNumericValue) break;
            }

            row.setNumeric(hasNumericValue);
        }

        List<StabilityReportResponse.TestResultRow> resultList = new ArrayList<>(testRowMap.values());

        // Sort by S.No to maintain hierarchy (3, 3.1, 3.2, etc.)
        resultList.sort(Comparator.comparing(
                StabilityReportResponse.TestResultRow::getSNo,
                Comparator.nullsLast(SNO_COMPARATOR)
        ));

        // Find the highest sequential number from existing S.No values
        int maxSequentialNo = 0;
        for (StabilityReportResponse.TestResultRow row : resultList) {
            if (row.getSNo() != null && !row.getSNo().trim().isEmpty()) {
                try {
                    // Extract integer part from S.No (e.g., "3" from "3", "3.1", "3.2")
                    String[] parts = row.getSNo().split("\\.");
                    int mainNumber = Integer.parseInt(parts[0]);
                    maxSequentialNo = Math.max(maxSequentialNo, mainNumber);
                } catch (NumberFormatException e) {
                    // Skip non-numeric S.No values
                }
            }
        }

        // Start assigning from next available number
        int sequentialNo = maxSequentialNo + 1;

        // Generate sequential S.No only for missing values (new tests from later periods)
        for (StabilityReportResponse.TestResultRow row : resultList) {
            if (row.getSNo() == null || row.getSNo().trim().isEmpty()) {
                // Assign next sequential number (avoids all conflicts)
                row.setSNo(String.valueOf(sequentialNo));
                sequentialNo++;
            }
        }

        // Final sort to ensure proper order after filling missing S.No
        resultList.sort(Comparator.comparing(
                StabilityReportResponse.TestResultRow::getSNo,
                Comparator.nullsLast(SNO_COMPARATOR)
        ));

        return resultList;
    }
}
