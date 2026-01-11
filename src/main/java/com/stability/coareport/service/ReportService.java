package com.stability.coareport.service;

import com.stability.coareport.dto.*;
import com.stability.coareport.entity.Branch;
import com.stability.coareport.entity.ChangeHistory;
import com.stability.coareport.entity.Report;
import com.stability.coareport.entity.TestResult;
import com.stability.coareport.repository.*;
import com.stability.coareport.util.FileStorageUtil;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private final ReportRepository reportRepository;
    private final TestResultRepository testResultRepository;
    private final BranchRepository branchRepository;
    private final PdfParserService pdfParserService;
    private final SecondTableExtractorService secondTableExtractorService;
    private final ChangeHistoryRepository changeHistoryRepository;
    private final MeterRegistry meterRegistry;

    private static final String UPLOAD_DIR = "uploads/";
    private static final String TEMP_UPLOAD_DIR = "temp_uploads/";
    private final Map<String, ParsedPdfData> tempFileStore = new ConcurrentHashMap<>();

    public ReportPreviewResponse uploadForPreview(MultipartFile file) throws IOException {
        ParsedPdfData parsedData = pdfParserService.parsePdf(file);

        String tempFileId = UUID.randomUUID().toString();
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path tempUploadPath = Paths.get(TEMP_UPLOAD_DIR);
        if (!Files.exists(tempUploadPath)) {
            Files.createDirectories(tempUploadPath);
        }
        Path tempFilePath = tempUploadPath.resolve(fileName);
        Files.write(tempFilePath, file.getBytes());

        tempFileStore.put(tempFileId, parsedData);

        ReportPreviewResponse response = new ReportPreviewResponse();
        response.setTempFileId(tempFileId);
        response.setPdfFileName(file.getOriginalFilename());
        response.setPdfFilePath(tempFilePath.toString());
        response.setProductName(parsedData.getProductName());
        response.setProductCode(parsedData.getProductCode());
        response.setArNo(parsedData.getArNo());
        response.setBatchNo(parsedData.getBatchNo());
        response.setBatchSize(parsedData.getBatchSize());
        response.setMfgDate(parsedData.getMfgDate());
        response.setExpDate(parsedData.getExpDate());
        response.setSpecification(parsedData.getSpecification());
        response.setStorageCondition(parsedData.getStorageCondition());
        response.setSampleQty(parsedData.getSampleQty());
        response.setReceivedDate(parsedData.getReceivedDate());
        response.setAnalysisStartDate(parsedData.getAnalysisStartDate());
        response.setAnalysisEndDate(parsedData.getAnalysisEndDate());
        response.setProtocolId(parsedData.getProtocolId());
        response.setStpNo(parsedData.getStpNo());
        response.setSchedulePeriod(parsedData.getSchedulePeriod());
        response.setPackingType(parsedData.getPackingType());
        response.setPackSize(parsedData.getPackSize());
        response.setRemarks(parsedData.getRemarks());
        response.setCheckedBy(parsedData.getCheckedBy());
        response.setApprovedBy(parsedData.getApprovedBy());
        response.setCheckDate(parsedData.getCheckDate());
        response.setApprovalDate(parsedData.getApprovalDate());
        response.setHdpeCapDepth(parsedData.getHdpeCapDepth());
        response.setLdpeNozzleDetails(parsedData.getLdpeNozzleDetails());
        response.setLdpeBottleDetails(parsedData.getLdpeBottleDetails());
        response.setTestResults(parsedData.getTestResults());

        return response;
    }

    public ReportPreviewResponse uploadForProductBasedPreview(MultipartFile file) throws IOException {
        logger.info("Processing product-based preview using SecondTableExtractorService");

        List<TestResultDto> testResults = secondTableExtractorService.extractTestResults(file);
        logger.info("Extracted {} test results using SecondTableExtractorService", testResults.size());

        ReportPreviewResponse response = new ReportPreviewResponse();
        response.setTestResults(testResults);

        return response;
    }

    @Transactional
    @Timed(value = "reports.submit", description = "Time taken to submit a report")
    public Report submitReport(ReportSubmitRequest request) throws IOException {
        meterRegistry.counter("reports.submitted.total").increment();
        validateReportData(request);

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        Path tempFilePath = Paths.get(request.getPdfFilePath());
        if (!Files.exists(tempFilePath)) {
            throw new RuntimeException("Temporary PDF file not found");
        }

        String finalFilePathStr = FileStorageUtil.generateFilePath(
                request.getProductName(),
                request.getBatchNo(),
                request.getStorageCondition(),
                request.getPdfFileName()
        );

        FileStorageUtil.ensureDirectoryExists(finalFilePathStr);
        Path finalFilePath = Paths.get(finalFilePathStr);
        Files.move(tempFilePath, finalFilePath);

        Report report = new Report();
        report.setBranch(branch);
        report.setProductName(request.getProductName());
        report.setProductCode(request.getProductCode());
        report.setArNo(request.getArNo());
        report.setBatchNo(request.getBatchNo());
        report.setBatchSize(request.getBatchSize());
        report.setMfgDate(request.getMfgDate());
        report.setExpDate(request.getExpDate());
        report.setSpecification(request.getSpecification());
        report.setStorageCondition(request.getStorageCondition());
        report.setSampleQty(request.getSampleQty());
        report.setReceivedDate(request.getReceivedDate());
        report.setAnalysisStartDate(request.getAnalysisStartDate());
        report.setAnalysisEndDate(request.getAnalysisEndDate());
        report.setProtocolId(request.getProtocolId());
        report.setStpNo(request.getStpNo());
        report.setSchedulePeriod(request.getSchedulePeriod());
        report.setPackingType(request.getPackingType());
        report.setPackSize(request.getPackSize());
        report.setRemarks(request.getRemarks());
        report.setCheckedBy(request.getCheckedBy());
        report.setApprovedBy(request.getApprovedBy());
        report.setCheckDate(request.getCheckDate());
        report.setApprovalDate(request.getApprovalDate());
        report.setHdpeCapDepth(request.getHdpeCapDepth());
        report.setLdpeNozzleDetails(request.getLdpeNozzleDetails());
        report.setLdpeBottleDetails(request.getLdpeBottleDetails());
        report.setPdfFileName(request.getPdfFileName());
        report.setPdfFilePath(finalFilePath.toString());
        report.setUploadedBy(request.getUploadedBy());
        report.setUploadedAt(java.time.LocalDateTime.now());
        report.setApprovalStatus("pending");

        report = reportRepository.save(report);

        List<TestResult> testResults = new ArrayList<>();
        for (TestResultDto dto : request.getTestResults()) {
            TestResult testResult = new TestResult();
            testResult.setReport(report);
            testResult.setSNo(dto.getSNo());
            testResult.setTest(dto.getTest());
            testResult.setResult(dto.getResult());
            testResult.setSpecification(dto.getSpecification());
            testResult.setObjection(dto.getObjection());
            testResults.add(testResult);
        }

        testResultRepository.saveAll(testResults);
        report.setTestResults(testResults);

        if (request.getTempFileId() != null) {
            tempFileStore.remove(request.getTempFileId());
        }

        return report;
    }

    @Transactional
    public Report createManualTestEntry(ManualTestEntryRequest request, String username) {
        Branch branch = branchRepository.findByName(request.getBranchName())
                .orElseThrow(() -> new RuntimeException("Branch not found: " + request.getBranchName()));

        Report report = new Report();
        report.setBranch(branch);
        report.setProductName(request.getProductName());
        report.setProductCode(request.getProductCode());
        report.setArNo(request.getArNo());
        report.setBatchNo(request.getBatchNo());
        report.setBatchSize(request.getBatchSize());
        report.setMfgDate(request.getMfgDate());
        report.setExpDate(request.getExpDate());
        report.setSpecification(request.getSpecificationId());
        report.setStorageCondition(request.getStorageCondition());
        report.setSampleOrientation(request.getSampleOrientation());
        report.setProtocolId(request.getProtocolId());
        report.setStpNo(request.getStpNumber());
        report.setSchedulePeriod(request.getSchedulePeriod());
        report.setPackingType(request.getPackingType());
        report.setPackSize(request.getPackSize());
        report.setHdpeCapDepth(request.getHdpeCapDetails());
        report.setLdpeNozzleDetails(request.getLdpeNozzleDetails());
        report.setLdpeBottleDetails(request.getLdpeBottleDetails());
        report.setUploadedBy(username);
        report.setUploadedAt(java.time.LocalDateTime.now());
        report.setApprovalStatus("pending");

        report = reportRepository.save(report);

        List<TestResult> testResults = new ArrayList<>();
        for (TestResultDto dto : request.getTestResults()) {
            TestResult testResult = new TestResult();
            testResult.setReport(report);
            testResult.setSNo(dto.getSNo());
            testResult.setTest(dto.getTest());
            testResult.setResult(dto.getResult());
            testResult.setSpecification(dto.getSpecification());
            testResults.add(testResult);
        }

        testResultRepository.saveAll(testResults);
        report.setTestResults(testResults);

        logger.info("Manual test entry created successfully for batch: {}", request.getBatchNo());
        return report;
    }

    @Transactional
    public Report processProductBasedUpload(
            MultipartFile file,
            String productName,
            String productCode,
            String batchNo,
            String arNo,
            String specificationId,
            String batchSize,
            String storageCondition,
            String sampleOrientation,
            String schedulePeriod,
            Long companyId,
            Long branchId,
            String uploadedBy
    ) throws IOException {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        String finalFilePathStr = FileStorageUtil.generateFilePath(
                productName,
                batchNo,
                storageCondition,
                file.getOriginalFilename()
        );

        FileStorageUtil.ensureDirectoryExists(finalFilePathStr);
        Path finalFilePath = Paths.get(finalFilePathStr);
        Files.write(finalFilePath, file.getBytes());

        List<TestResultDto> testResultDtos = secondTableExtractorService.extractTestResults(file);

        Report report = new Report();
        report.setBranch(branch);
        report.setProductName(productName);
        report.setProductCode(productCode);
        report.setBatchNo(batchNo);
        report.setArNo(arNo);
        report.setSpecification(specificationId);
        report.setBatchSize(batchSize);
        report.setStorageCondition(storageCondition);
        report.setSampleOrientation(sampleOrientation);
        report.setSchedulePeriod(schedulePeriod);
        report.setPdfFileName(file.getOriginalFilename());
        report.setPdfFilePath(finalFilePath.toString());
        report.setUploadedBy(uploadedBy);
        report.setUploadedAt(java.time.LocalDateTime.now());
        report.setApprovalStatus("pending");

        report = reportRepository.save(report);

        if (testResultDtos != null && !testResultDtos.isEmpty()) {
            List<TestResult> testResults = new ArrayList<>();
            for (TestResultDto dto : testResultDtos) {
                TestResult testResult = new TestResult();
                testResult.setReport(report);
                testResult.setSNo(dto.getSNo());
                testResult.setTest(dto.getTest());
                testResult.setResult(dto.getResult());
                testResult.setSpecification(dto.getSpecification());
                testResult.setObjection(dto.getObjection());
                testResults.add(testResult);
            }
            testResultRepository.saveAll(testResults);
            report.setTestResults(testResults);
        }

        logger.info("Product-based upload processed successfully for product: {} batch: {}", productName, batchNo);
        return report;
    }

    public List<Report> getReportsByBranch(Long branchId) {
        return reportRepository.findByBranchId(branchId);
    }

    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    public PageResponse<Report> getAllReportsPaginated(int page, int size, String sortBy, String sortDirection) {
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
        return PageResponse.fromSpringPage(reportPage);
    }

    public PageResponse<Report> getReportsByBranchPaginated(Long branchId, int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        PageRequest pageRequest;
        if (sortBy != null && !sortBy.isEmpty()) {
            pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        } else {
            pageRequest = PageRequest.of(page, size, Sort.by(direction, "id"));
        }

        Page<Report> reportPage = reportRepository.findByBranchId(branchId, pageRequest);
        return PageResponse.fromSpringPage(reportPage);
    }

    public Report getReportById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));
    }

    public List<String> getDistinctProductNames(Long branchId) {
        if (branchId != null) {
            return reportRepository.findDistinctProductNamesByBranchId(branchId);
        }
        return reportRepository.findDistinctProductNames();
    }

    @Transactional
    public Report updateField(UpdateFieldRequest request) {
        Report report = reportRepository.findByProductName(request.getProductName())
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Report not found for product: " + request.getProductName()));

        if ("SPECIFICATION".equals(request.getFieldType())) {
            String oldValue = report.getSpecification();
            report.setSpecification(request.getNewValue());
            saveChangeHistory("Report", report.getId(), "specification", oldValue, request.getNewValue(), "UPDATE",
                    request.getRemarks(), request.getEvidenceDocumentName(), request.getEvidenceDocumentPath());
        } else if ("STORAGE_CONDITION".equals(request.getFieldType())) {
            String oldValue = report.getStorageCondition();
            report.setStorageCondition(request.getNewValue());
            saveChangeHistory("Report", report.getId(), "storageCondition", oldValue, request.getNewValue(), "UPDATE",
                    request.getRemarks(), request.getEvidenceDocumentName(), request.getEvidenceDocumentPath());
        } else if ("TEST_RESULT".equals(request.getFieldType()) && request.getTestResultId() != null) {
            TestResult testResult = testResultRepository.findById(request.getTestResultId())
                    .orElseThrow(() -> new RuntimeException("Test result not found"));

            String oldValue = null;
            if ("result".equals(request.getFieldName())) {
                oldValue = testResult.getResult();
                testResult.setResult(request.getNewValue());
            } else if ("specification".equals(request.getFieldName())) {
                oldValue = testResult.getSpecification();
                testResult.setSpecification(request.getNewValue());
            } else if ("objection".equals(request.getFieldName())) {
                oldValue = testResult.getObjection();
                testResult.setObjection(request.getNewValue());
            }

            testResultRepository.save(testResult);
            saveChangeHistory("TestResult", testResult.getId(), request.getFieldName(), oldValue, request.getNewValue(), "UPDATE",
                    request.getRemarks(), request.getEvidenceDocumentName(), request.getEvidenceDocumentPath());
        }

        return reportRepository.save(report);
    }

    private void saveChangeHistory(String entityType, Long entityId, String fieldName, String oldValue, String newValue, String action) {
        saveChangeHistory(entityType, entityId, fieldName, oldValue, newValue, action, null, null, null);
    }

    private void saveChangeHistory(String entityType, Long entityId, String fieldName, String oldValue, String newValue, String action,
                                   String remarks, String evidenceDocumentName, String evidenceDocumentPath) {
        ChangeHistory history = new ChangeHistory();
        history.setEntityType(entityType);
        history.setEntityId(entityId);
        history.setFieldName(fieldName);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setAction(action);
        history.setRemarks(remarks);
        history.setEvidenceDocumentName(evidenceDocumentName);
        history.setEvidenceDocumentPath(evidenceDocumentPath);
        changeHistoryRepository.save(history);
    }

    public List<ChangeHistory> getChangeHistory(String entityType, Long entityId) {
        return changeHistoryRepository.findByEntityTypeAndEntityIdOrderByModifiedAtDesc(entityType, entityId);
    }

    public List<ChangeHistoryResponse> getChangeHistoryByProduct(String productName) {
        List<Report> reports = reportRepository.findByProductName(productName);
        List<Long> reportIds = reports.stream().map(Report::getId).collect(Collectors.toList());

        List<ChangeHistory> allHistory = new ArrayList<>();
        for (Long reportId : reportIds) {
            allHistory.addAll(changeHistoryRepository.findByEntityTypeAndEntityIdOrderByModifiedAtDesc("Report", reportId));

            Report report = reportRepository.findById(reportId).orElse(null);
            if (report != null) {
                for (TestResult testResult : report.getTestResults()) {
                    allHistory.addAll(changeHistoryRepository.findByEntityTypeAndEntityIdOrderByModifiedAtDesc("TestResult", testResult.getId()));
                }
            }
        }

        allHistory.sort(Comparator.comparing(ChangeHistory::getModifiedAt).reversed());

        return allHistory.stream().map(history -> {
            ChangeHistoryResponse response = new ChangeHistoryResponse();
            response.setId(history.getId());
            response.setEntityType(history.getEntityType());
            response.setEntityId(history.getEntityId());
            response.setFieldName(history.getFieldName());
            response.setOldValue(history.getOldValue());
            response.setNewValue(history.getNewValue());
            response.setAction(history.getAction());
            response.setModifiedBy(history.getModifiedBy());
            response.setModifiedAt(history.getModifiedAt());
            response.setRemarks(history.getRemarks());
            response.setEvidenceDocumentName(history.getEvidenceDocumentName());
            response.setEvidenceDocumentPath(history.getEvidenceDocumentPath());

            if ("Report".equals(history.getEntityType())) {
                Report report = reportRepository.findById(history.getEntityId()).orElse(null);
                if (report != null) {
                    response.setProductName(report.getProductName());
                    response.setBatchNo(report.getBatchNo());
                }
            } else if ("TestResult".equals(history.getEntityType())) {
                TestResult testResult = testResultRepository.findById(history.getEntityId()).orElse(null);
                if (testResult != null && testResult.getReport() != null) {
                    response.setProductName(testResult.getReport().getProductName());
                    response.setBatchNo(testResult.getReport().getBatchNo());
                }
            }

            return response;
        }).collect(Collectors.toList());
    }

    public List<String> getBatchNumbersByProduct(String productName) {
        return reportRepository.findDistinctBatchNumbersByProductName(productName);
    }

    public List<String> getStorageConditionsByProduct(String productName) {
        return reportRepository.findDistinctStorageConditionsByProductName(productName);
    }

    public FilterOptionsResponse getFilterOptions(String productName) {
        List<String> testNames = testResultRepository.findDistinctTestNamesByProductName(productName);
        List<String> specifications = testResultRepository.findDistinctSpecificationsByProductName(productName);
        List<String> markets = reportRepository.findDistinctMarketsByProductName(productName);
        List<String> positions = reportRepository.findDistinctPositionsByProductName(productName);
        List<String> packingTypes = reportRepository.findDistinctPackingTypesByProductName(productName);
        List<String> packSizes = reportRepository.findDistinctPackSizesByProductName(productName);
        List<String> stations = reportRepository.findDistinctStationsByProductName(productName);
        List<String> storageConditions = reportRepository.findDistinctStorageConditionsByProductName(productName);

        return new FilterOptionsResponse(
                testNames, specifications, markets, positions,
                packingTypes, packSizes, stations, storageConditions
        );
    }

    public ComparisonResponse compareReports(String productName, List<String> batchNumbers,
                                             String storageCondition, String testName, String specification,
                                             String market, String position, String packType, String packValue,
                                             List<String> stations, boolean invert) {
        List<Report> reports = reportRepository.findByProductNameOrderByCreatedAtAsc(productName);

        if (batchNumbers != null && !batchNumbers.isEmpty()) {
            reports = reports.stream()
                    .filter(r -> batchNumbers.contains(r.getBatchNo()))
                    .collect(Collectors.toList());
        }
        if (storageCondition != null && !storageCondition.isEmpty()) {
            reports = reports.stream()
                    .filter(r -> storageCondition.equals(r.getStorageCondition()))
                    .collect(Collectors.toList());
        }
        if (market != null && !market.isEmpty()) {
            reports = reports.stream()
                    .filter(r -> market.equals(r.getMarket()))
                    .collect(Collectors.toList());
        }
        if (position != null && !position.isEmpty()) {
            reports = reports.stream()
                    .filter(r -> position.equals(r.getSampleOrientation()))
                    .collect(Collectors.toList());
        }
        if (packType != null && packValue != null && !packType.isEmpty() && !packValue.isEmpty()) {
            if ("packingType".equals(packType)) {
                reports = reports.stream()
                        .filter(r -> packValue.equals(r.getPackingType()))
                        .collect(Collectors.toList());
            } else if ("packSize".equals(packType)) {
                reports = reports.stream()
                        .filter(r -> packValue.equals(r.getPackSize()))
                        .collect(Collectors.toList());
            }
        }
        if (stations != null && !stations.isEmpty()) {
            reports = reports.stream()
                    .filter(r -> r.getSchedulePeriod() != null && stations.contains(r.getSchedulePeriod()))
                    .collect(Collectors.toList());
        }

        if (reports.isEmpty()) {
            throw new RuntimeException("No reports found for the given criteria");
        }

        Set<String> allTestNames = new LinkedHashSet<>();
        Set<String> allStations = new LinkedHashSet<>();
        for (Report report : reports) {
            if (report.getSchedulePeriod() != null) {
                allStations.add(report.getSchedulePeriod());
            }
            for (TestResult tr : report.getTestResults()) {
                if (testName == null || testName.isEmpty() || testName.equals(tr.getTest())) {
                    if (specification == null || specification.isEmpty() || specification.equals(tr.getSpecification())) {
                        allTestNames.add(tr.getTest());
                    }
                }
            }
        }

        Map<String, ComparisonResponse.BatchData> batchDataMap = new LinkedHashMap<>();

        for (Report report : reports) {
            String batchNo = report.getBatchNo();
            String station = report.getSchedulePeriod() != null ? report.getSchedulePeriod() : "Initial";

            ComparisonResponse.BatchData batchData = batchDataMap.computeIfAbsent(batchNo,
                    k -> new ComparisonResponse.BatchData(
                            batchNo,
                            report.getMfgDate(),
                            report.getExpDate(),
                            report.getStorageCondition(),
                            new LinkedHashMap<>()
                    ));

            Map<String, ComparisonResponse.TestValue> testResultMap = new HashMap<>();

            for (TestResult tr : report.getTestResults()) {
                if (testName == null || testName.isEmpty() || testName.equals(tr.getTest())) {
                    if (specification == null || specification.isEmpty() || specification.equals(tr.getSpecification())) {
                        Double numericValue = extractNumericValue(tr.getResult());
                        if (invert && numericValue != null) {
                            numericValue = -numericValue;
                        }

                        testResultMap.put(tr.getTest(), new ComparisonResponse.TestValue(
                                tr.getResult(),
                                numericValue,
                                tr.getSpecification()
                        ));
                    }
                }
            }

            batchData.getStationData().put(station, new ComparisonResponse.StationData(
                    station,
                    testResultMap
            ));
        }

        List<String> sortedStations = new ArrayList<>(allStations);
        sortedStations.sort((s1, s2) -> {
            if (s1.equals("Initial") || s1.equals("Initially")) return -1;
            if (s2.equals("Initial") || s2.equals("Initially")) return 1;
            Integer m1 = extractMonthsFromStation(s1);
            Integer m2 = extractMonthsFromStation(s2);
            if (m1 == null && m2 == null) return s1.compareTo(s2);
            if (m1 == null) return 1;
            if (m2 == null) return -1;
            return m1.compareTo(m2);
        });

        return new ComparisonResponse(
                productName,
                new ArrayList<>(allTestNames),
                sortedStations,
                new ArrayList<>(batchDataMap.values())
        );
    }

    private Integer extractMonthsFromStation(String station) {
        if (station == null) return null;
        try {
            return Integer.parseInt(station.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public PredictionResponse getPrediction(String productName, String storageCondition) {
        List<Report> reports;

        if (storageCondition != null && !storageCondition.isEmpty()) {
            reports = reportRepository.findByProductNameAndStorageCondition(productName, storageCondition);
        } else {
            reports = reportRepository.findByProductNameOrderByCreatedAtAsc(productName);
        }

        if (reports.size() < 2) {
            throw new RuntimeException("Need at least 2 reports for prediction");
        }

        reports.sort(Comparator.comparing(Report::getCreatedAt));

        Map<String, Map<Integer, PredictionResponse.DataPoint>> testDataMap = new HashMap<>();

        for (Report report : reports) {
            Integer months = parseSchedulePeriodToMonths(report.getSchedulePeriod());
            if (months == null) continue;

            for (TestResult tr : report.getTestResults()) {
                Double value = extractNumericValue(tr.getResult());
                if (value != null) {
                    PredictionResponse.DataPoint dataPoint = new PredictionResponse.DataPoint();
                    dataPoint.setBatchNumber(report.getBatchNo());
                    dataPoint.setDate(report.getMfgDate() != null ? report.getMfgDate() : report.getCreatedAt().toString());
                    dataPoint.setValue(value);
                    dataPoint.setPredicted(false);
//                    dataPoint.setIsPredicted(false);
                    dataPoint.setMonthsFromStart(months);
                    dataPoint.setSchedulePeriod(report.getSchedulePeriod());

                    testDataMap
                            .computeIfAbsent(tr.getTest(), k -> new HashMap<>())
                            .put(months, dataPoint);
                }
            }
        }

        List<PredictionResponse.TestPrediction> predictions = new ArrayList<>();

        for (Map.Entry<String, Map<Integer, PredictionResponse.DataPoint>> entry : testDataMap.entrySet()) {
            String testName = entry.getKey();
            Map<Integer, PredictionResponse.DataPoint> dataByMonth = entry.getValue();

            if (dataByMonth.size() < 2) continue;

            List<Integer> sortedMonths = new ArrayList<>(dataByMonth.keySet());
            Collections.sort(sortedMonths);

            List<PredictionResponse.DataPoint> historicalData = sortedMonths.stream()
                    .map(dataByMonth::get)
                    .collect(Collectors.toList());

            double[] xValues = sortedMonths.stream().mapToDouble(Integer::doubleValue).toArray();
            double[] yValues = sortedMonths.stream()
                    .map(dataByMonth::get)
                    .mapToDouble(PredictionResponse.DataPoint::getValue)
                    .toArray();

            double[] linearRegression = calculateLinearRegression(xValues, yValues);
            double intercept = linearRegression[0];
            double slope = linearRegression[1];

            String trendDirection;
            if (Math.abs(slope) < 0.01) {
                trendDirection = "STABLE";
            } else if (slope > 0) {
                trendDirection = "INCREASING";
            } else {
                trendDirection = "DECREASING";
            }

            int maxMonth = Collections.max(sortedMonths);
            List<Integer> futurePeriods = new ArrayList<>();
            for (int month : new int[]{13, 18, 24, 36}) {
                if (month > maxMonth) {
                    futurePeriods.add(month);
                }
            }

            List<PredictionResponse.DataPoint> predictedData = new ArrayList<>();
            for (Integer futureMonth : futurePeriods) {
                double predictedValue = intercept + (slope * futureMonth);

                PredictionResponse.DataPoint prediction = new PredictionResponse.DataPoint();
                prediction.setBatchNumber("Predicted");
                prediction.setDate("Future");
                prediction.setValue(Math.round(predictedValue * 100.0) / 100.0);
                prediction.setPredicted(true);
//                prediction.setIsPredicted(true);
                prediction.setMonthsFromStart(futureMonth);
                prediction.setSchedulePeriod(futureMonth + " Month(s)");

                predictedData.add(prediction);
            }

            double confidence = calculateConfidenceFromRegression(xValues, yValues, intercept, slope);

            predictions.add(new PredictionResponse.TestPrediction(
                    testName,
                    historicalData,
                    predictedData,
                    trendDirection,
                    confidence
            ));
        }

        return new PredictionResponse(
                productName,
                storageCondition != null ? storageCondition : "All conditions",
                predictions
        );
    }

    private Integer parseSchedulePeriodToMonths(String schedulePeriod) {
        if (schedulePeriod == null || schedulePeriod.isEmpty()) {
            return null;
        }

        String normalized = schedulePeriod.trim().toLowerCase();

        if (normalized.equals("initial")) {
            return 0;
        }

        try {
            String numberPart = normalized.replaceAll("[^0-9]", "");
            if (!numberPart.isEmpty()) {
                return Integer.parseInt(numberPart);
            }
        } catch (NumberFormatException e) {
        }

        return null;
    }

    private double[] calculateLinearRegression(double[] x, double[] y) {
        int n = x.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i];
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        return new double[]{intercept, slope};
    }

    private double calculateConfidenceFromRegression(double[] x, double[] y, double intercept, double slope) {
        int n = x.length;

        double[] predicted = new double[n];
        for (int i = 0; i < n; i++) {
            predicted[i] = intercept + slope * x[i];
        }

        double ssTotal = 0;
        double ssResidual = 0;
        double meanY = Arrays.stream(y).average().orElse(0);

        for (int i = 0; i < n; i++) {
            ssTotal += Math.pow(y[i] - meanY, 2);
            ssResidual += Math.pow(y[i] - predicted[i], 2);
        }

        double rSquared = 1 - (ssResidual / (ssTotal + 0.0001));
        double dataConfidence = Math.min(1.0, n / 5.0);
        double confidence = (rSquared * 0.7 + dataConfidence * 0.3);

        return Math.round(Math.max(0.3, Math.min(1.0, confidence)) * 100) / 100.0;
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

    private double calculateSlope(double[] values) {
        int n = values.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values[i];
            sumXY += i * values[i];
            sumX2 += i * i;
        }

        return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
    }

    private double calculateConfidence(double[] values, double slope) {
        int n = values.length;
        double mean = Arrays.stream(values).average().orElse(0);
        double variance = Arrays.stream(values)
                .map(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);

        double baseConfidence = Math.max(0.5, 1.0 - (variance / (mean * mean + 1)));
        double slopeConfidence = Math.max(0.5, 1.0 - Math.abs(slope) / 10);
        double dataConfidence = Math.min(1.0, n / 10.0);

        return Math.round((baseConfidence * 0.4 + slopeConfidence * 0.3 + dataConfidence * 0.3) * 100) / 100.0;
    }

    public String uploadEvidenceDocument(MultipartFile file) throws IOException {
        String uploadDir = System.getProperty("java.io.tmpdir") + "/evidence-documents/";
        Files.createDirectories(Paths.get(uploadDir));

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir + fileName);
        Files.write(filePath, file.getBytes());

        return "/evidence-documents/" + fileName;
    }

    private void validateReportData(ReportSubmitRequest request) {
        List<String> warnings = new ArrayList<>();

        if (request.getBatchSize() == null || request.getBatchSize().trim().isEmpty()) {
            warnings.add("Batch Size is empty - this field was not populated from the PDF");
        }

        if (request.getStorageCondition() == null || request.getStorageCondition().trim().isEmpty()) {
            warnings.add("Storage Condition is empty - this field was not populated from the PDF");
        }

        if (request.getMfgDate() == null || request.getMfgDate().trim().isEmpty()) {
            warnings.add("Manufacturing Date is empty - this field was not populated from the PDF");
        } else {
            validateMfgDate(request.getMfgDate(), warnings);
        }

        if (request.getExpDate() == null || request.getExpDate().trim().isEmpty()) {
            warnings.add("Expiry Date is empty - this field was not populated from the PDF");
        } else {
            validateExpDate(request.getExpDate(), warnings);
        }

        if (request.getReceivedDate() == null || request.getReceivedDate().trim().isEmpty()) {
            warnings.add("Received Date (Schedule Date) is empty - this field was not populated from the PDF");
        }

        if (!warnings.isEmpty()) {
            logger.warn("PDF Parsing Warnings for Product {}: {}", request.getProductName(), String.join(", ", warnings));
        }
    }

    private void validateMfgDate(String mfgDateStr, List<String> warnings) {
        try {
            DateTimeFormatter[] formatters = {
                    DateTimeFormatter.ofPattern("dd-MMM-yyyy"),
                    DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                    DateTimeFormatter.ofPattern("MMM-yyyy")
            };

            LocalDate mfgDate = null;
            for (DateTimeFormatter formatter : formatters) {
                try {
                    mfgDate = LocalDate.parse(mfgDateStr, formatter);
                    break;
                } catch (DateTimeParseException e) {
                }
            }

            if (mfgDate == null) {
                warnings.add("Manufacturing Date format could not be validated: " + mfgDateStr + " (may be month-year format)");
                return;
            }

            LocalDate today = LocalDate.now();
            if (mfgDate.isAfter(today)) {
                warnings.add("Manufacturing Date (" + mfgDateStr + ") appears to be in the future");
            }
        } catch (Exception e) {
            warnings.add("Could not validate Manufacturing Date: " + e.getMessage());
        }
    }

    private void validateExpDate(String expDateStr, List<String> warnings) {
        try {
            DateTimeFormatter[] formatters = {
                    DateTimeFormatter.ofPattern("dd-MMM-yyyy"),
                    DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                    DateTimeFormatter.ofPattern("MMM-yyyy")
            };

            LocalDate expDate = null;
            for (DateTimeFormatter formatter : formatters) {
                try {
                    expDate = LocalDate.parse(expDateStr, formatter);
                    break;
                } catch (DateTimeParseException e) {
                }
            }

            if (expDate == null) {
                warnings.add("Expiry Date format could not be validated: " + expDateStr + " (may be month-year format)");
                return;
            }

            LocalDate today = LocalDate.now();
            if (expDate.isBefore(today)) {
                warnings.add("Expiry Date (" + expDateStr + ") appears to have already passed");
            }
        } catch (Exception e) {
            warnings.add("Could not validate Expiry Date: " + e.getMessage());
        }
    }
}
