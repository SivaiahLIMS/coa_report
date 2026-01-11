package com.stability.coareport.service;

import com.stability.coareport.dto.TestResultDto;
import com.stability.coareport.exception.ScannedPdfNotSupportedException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SecondTableExtractorService {

    private static final Logger logger = LoggerFactory.getLogger(SecondTableExtractorService.class);
    private static final boolean KEEP_PARENT_HEADERS = true;

    static class Cell {
        final float x;
        final String text;
        Cell(float x, String text) { this.x = x; this.text = text; }
    }

    static class Row {
        final float y;
        final List<Cell> cells = new ArrayList<>();
        Row(float y) { this.y = y; }
    }

    static class PageText {
        final List<Row> rows = new ArrayList<>();
    }

    private static class PositionStripper extends PDFTextStripper {
        private final List<PageText> pages = new ArrayList<>();
        private PageText currentPage;

        PositionStripper() throws IOException {
            super();
            setSortByPosition(true);
        }

        List<PageText> getPages() { return pages; }

        @Override
        protected void startPage(org.apache.pdfbox.pdmodel.PDPage page) throws IOException {
            currentPage = new PageText();
            pages.add(currentPage);
            super.startPage(page);
        }

        @Override
        protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
            if (currentPage == null || textPositions.isEmpty()) return;

            Map<Integer, List<TextPosition>> lineBuckets = new LinkedHashMap<>();
            for (TextPosition tp : textPositions) {
                int key = Math.round(tp.getYDirAdj());
                lineBuckets.computeIfAbsent(key, k -> new ArrayList<>()).add(tp);
            }

            for (List<TextPosition> line : lineBuckets.values()) {
                line.sort(Comparator.comparing(TextPosition::getXDirAdj));
                float y = line.get(0).getYDirAdj();
                Row row = new Row(y);

                StringBuilder word = new StringBuilder();
                float wordStartX = -1f;
                float lastRight = -1f;

                for (TextPosition tp : line) {
                    float x = tp.getXDirAdj();
                    float right = x + tp.getWidth();
                    String ch = tp.getUnicode();

                    if (lastRight >= 0) {
                        float gap = x - lastRight;
                        if (gap > tp.getWidthOfSpace() / 2.0) {
                            if (word.length() > 0) {
                                row.cells.add(new Cell(wordStartX >= 0 ? wordStartX : x, word.toString().trim()));
                                word.setLength(0);
                            }
                            wordStartX = x;
                        }
                    } else {
                        wordStartX = x;
                    }

                    word.append(ch);
                    lastRight = right;
                }

                if (word.length() > 0) {
                    row.cells.add(new Cell(wordStartX >= 0 ? wordStartX : 0f, word.toString().trim()));
                }

                if (!row.cells.isEmpty()) currentPage.rows.add(row);
            }
        }
    }

    public List<TestResultDto> extractTestResults(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PositionStripper stripper = new PositionStripper();
            stripper.getText(document);

            List<PageText> pages = stripper.getPages();

            if (pages.isEmpty() || isScannedPdf(pages)) {
                logger.warn("Scanned PDF detected: No extractable text found");
                throw new ScannedPdfNotSupportedException(
                        "This PDF appears to be a scanned document without extractable text. " +
                                "Please upload a digitally generated PDF report instead. " +
                                "Scanned PDFs are not currently supported."
                );
            }

            LinkedHashMap<String, TestResultDto> testResults = new LinkedHashMap<>();

            for (PageText page : pages) {
                parseTestResultsFromPage(page, testResults);
            }

            return new ArrayList<>(testResults.values());
        }
    }

    private boolean isScannedPdf(List<PageText> pages) {
        if (pages.isEmpty()) {
            return true;
        }

        int totalCells = 0;
        for (PageText page : pages) {
            for (Row row : page.rows) {
                totalCells += row.cells.size();
            }
        }

        return totalCells == 0;
    }

    private void parseTestResultsFromPage(PageText page, LinkedHashMap<String, TestResultDto> testResultsOut) {
        logger.info("Parsing test results from page with {} rows", page.rows.size());

        float xSno = 0f, xTest = 120f, xResult = 320f, xSpec = 450f;
        int headerRowIndex = -1;
        boolean isSplitHeader = false;

        for (int i = 0; i < page.rows.size(); i++) {
            Row r = page.rows.get(i);
            String joined = r.cells.stream().map(c -> c.text).collect(Collectors.joining(" "));
            String normalized = joined.replaceAll("\\s+", " ").toLowerCase().trim();

            boolean hasSNo = normalized.matches(".*s\\.?\\s*no\\.?.*");
            boolean hasTest = normalized.contains("test");
            boolean hasResult = normalized.contains("result");
            boolean hasSpec = normalized.contains("spec");

            if (hasTest || hasResult || hasSpec || hasSNo) {
                logger.info("Row {} [cells={}]: '{}' -> normalized: '{}' | S.No={}, Test={}, Result={}, Spec={}",
                        i, r.cells.size(), joined, normalized, hasSNo, hasTest, hasResult, hasSpec);
            }

            // Case 1: All header columns in one row (e.g., "S.No. Test Result Specification")
            if (hasSNo && hasTest && hasResult && hasSpec) {
                headerRowIndex = i;
                logger.info("Found table header at row {}: {}", i, joined);
                List<Cell> hc = new ArrayList<>(r.cells);
                hc.sort(Comparator.comparing(c -> c.x));
                for (Cell c : hc) {
                    String t = c.text.toLowerCase();
                    String normalizedCell = t.replaceAll("[\\s.]+", "");
                    if (normalizedCell.contains("sno") || t.matches(".*s\\.?\\s*no\\.?.*")) {
                        xSno = c.x;
                        logger.info("S.No column at x={}", xSno);
                    }
                    else if (t.contains("test")) {
                        xTest = c.x;
                        logger.info("TEST column at x={}", xTest);
                    }
                    else if (t.contains("result")) {
                        xResult = c.x;
                        logger.info("RESULT column at x={}", xResult);
                    }
                    else if (t.contains("spec")) {
                        xSpec = c.x;
                        logger.info("SPECIFICATION column at x={}", xSpec);
                    }
                }
                break;
            }

            // Case 2: Header columns split across consecutive rows (e.g., Row N: "S. No.", Row N+1: "TEST", etc.)
            if (hasSNo && !hasTest && !hasResult && !hasSpec && i + 3 < page.rows.size()) {
                String row1 = page.rows.get(i + 1).cells.stream().map(c -> c.text).collect(Collectors.joining(" ")).replaceAll("\\s+", " ").toLowerCase().trim();
                String row2 = page.rows.get(i + 2).cells.stream().map(c -> c.text).collect(Collectors.joining(" ")).replaceAll("\\s+", " ").toLowerCase().trim();
                String row3 = page.rows.get(i + 3).cells.stream().map(c -> c.text).collect(Collectors.joining(" ")).replaceAll("\\s+", " ").toLowerCase().trim();

                boolean row1HasTest = row1.contains("test");
                boolean row2HasResult = row2.contains("result");
                boolean row3HasSpec = row3.contains("spec");

                if (row1HasTest && row2HasResult && row3HasSpec) {
                    headerRowIndex = i;
                    isSplitHeader = true;
                    logger.info("Found split table header starting at row {}: S.No={}, TEST={}, RESULT={}, SPEC={}",
                            i, normalized, row1, row2, row3);

                    // Extract X positions from each row's first cell
                    if (!page.rows.get(i).cells.isEmpty()) xSno = page.rows.get(i).cells.get(0).x;
                    if (!page.rows.get(i + 1).cells.isEmpty()) xTest = page.rows.get(i + 1).cells.get(0).x;
                    if (!page.rows.get(i + 2).cells.isEmpty()) xResult = page.rows.get(i + 2).cells.get(0).x;
                    if (!page.rows.get(i + 3).cells.isEmpty()) xSpec = page.rows.get(i + 3).cells.get(0).x;

                    logger.info("Column positions - S.No: {}, TEST: {}, RESULT: {}, SPEC: {}", xSno, xTest, xResult, xSpec);

                    // Skip the next 3 header rows and start parsing from row i+4
                    headerRowIndex = i + 3;
                    break;
                }
            }
        }

        if (headerRowIndex == -1) {
            logger.warn("Table header not found, cannot parse test results");
            return;
        }

        float cut1, cut2, cut3;

        if (isSplitHeader) {
            logger.info("Split header detected - using first data rows to determine column boundaries");
            float dataXSno = -1, dataXTest = -1, dataXResult = -1, dataXSpec = -1;
            int sampleRows = 0;

            for (int i = headerRowIndex + 1; i < Math.min(headerRowIndex + 10, page.rows.size()) && sampleRows < 5; i++) {
                Row r = page.rows.get(i);
                if (r.cells.isEmpty()) continue;
                String joinedLine = r.cells.stream().map(c -> c.text.trim()).collect(Collectors.joining(" "));
                if (joinedLine.isEmpty() || isFooterLine(joinedLine)) continue;

                for (Cell c : r.cells) {
                    String t = c.text.trim();
                    if (t.isEmpty()) continue;

                    if (dataXSno == -1 && c.x < 60) dataXSno = c.x;
                    else if (dataXTest == -1 && c.x >= 60 && c.x < 180) dataXTest = c.x;
                    else if (dataXResult == -1 && c.x >= 180 && c.x < 320) dataXResult = c.x;
                    else if (dataXSpec == -1 && c.x >= 320) dataXSpec = c.x;
                }
                sampleRows++;
            }

            if (dataXSno != -1 && dataXTest != -1) {
                cut1 = (dataXSno + dataXTest) / 2f;
            } else {
                cut1 = (xSno + xTest) / 2f;
            }

            if (dataXTest != -1 && dataXResult != -1) {
                cut2 = (dataXTest + dataXResult) / 2f;
            } else {
                cut2 = (xTest + xResult) / 2f;
            }

            if (dataXResult != -1 && dataXSpec != -1) {
                cut3 = (dataXResult + dataXSpec) / 2f;
            } else {
                cut3 = (xResult + xSpec) / 2f;
            }

            logger.info("Calculated cutoffs from data: cut1={}, cut2={}, cut3={}", cut1, cut2, cut3);
        } else {
            cut1 = (xSno + xTest) / 2f;
            cut2 = (xTest + xResult) / 2f;
            cut3 = (xResult + xSpec) / 2f;
            logger.info("Calculated cutoffs from header: cut1={}, cut2={}, cut3={}", cut1, cut2, cut3);
        }

        logger.info("Column positions - S.No: {}, TEST: {}, RESULT: {}, SPEC: {}", xSno, xTest, xResult, xSpec);
        logger.info("Final cut points - cut1 (SNO/TEST): {}, cut2 (TEST/RESULT): {}, cut3 (RESULT/SPEC): {}", cut1, cut2, cut3);

        String sNoBuf = "", testBuf = "", resultBuf = "", specBuf = "";
        boolean inRow = false;

        logger.info("Starting to parse data rows from row {} onwards", headerRowIndex + 1);

        for (int i = headerRowIndex + 1; i < page.rows.size(); i++) {
            Row r = page.rows.get(i);
            String joinedLine = r.cells.stream().map(c -> c.text.trim()).collect(Collectors.joining(" "));

            if (joinedLine.isEmpty()) {
                logger.debug("Skipping row {}: (empty)", i);
                continue;
            }

            if (isFooterLine(joinedLine)) {
                logger.info("Row {} marked as footer: '{}'", i, joinedLine);
                logger.debug("Skipping row {}: (footer)", i);
                continue;
            }

            logger.info("Row {} [cells={}]: '{}'", i, r.cells.size(), joinedLine);
            for (Cell c : r.cells) {
                logger.info("  Cell at x={}: '{}'", c.x, c.text.trim());
            }

            List<String> snoTokens = new ArrayList<>(), testTokens = new ArrayList<>(),
                    resultTokens = new ArrayList<>(), specTokens = new ArrayList<>();

            for (Cell c : r.cells) {
                String t = c.text.trim();
                if (t.isEmpty()) continue;
                if (c.x <= cut1) {
                    snoTokens.add(t);
                    logger.info("    -> Assigned to S.No: '{}'", t);
                } else if (c.x <= cut2) {
                    testTokens.add(t);
                    logger.info("    -> Assigned to TEST: '{}'", t);
                } else if (c.x <= cut3) {
                    resultTokens.add(t);
                    logger.info("    -> Assigned to RESULT: '{}'", t);
                } else {
                    specTokens.add(t);
                    logger.info("    -> Assigned to SPEC: '{}'", t);
                }
            }

            String rawSnoStr = String.join(" ", snoTokens).trim();
            String snoStr = sanitizeSno(rawSnoStr);
            String testStr = String.join(" ", testTokens).trim();
            String resultStr = String.join(" ", resultTokens).trim();
            String specStr = String.join(" ", specTokens).trim();

            logger.debug("Row {} - Tokens - SNO: {}, TEST: {}, RESULT: {}, SPEC: {}",
                    i, snoTokens, testTokens, resultTokens, specTokens);
            logger.info("Row {} - Joined strings - SNO: '{}', TEST: '{}', RESULT: '{}', SPEC: '{}'",
                    i, snoStr, testStr, resultStr, specStr);

            String trailingFromSno = extractTrailingFromSno(rawSnoStr);
            if (!trailingFromSno.isEmpty()) {
                testStr = append(trailingFromSno, testStr);
            }

            boolean hasSnoToken = !snoStr.isEmpty();
            boolean isParent = snoStr.matches("^\\d+$");
            boolean hasValues = !(resultStr.isEmpty() && specStr.isEmpty());

            if (hasSnoToken) {
                if (inRow) {
                    boolean prevParentOnly = sNoBuf.matches("^\\d+$") && resultBuf.isEmpty() && specBuf.isEmpty();
                    if (!prevParentOnly || KEEP_PARENT_HEADERS) {
                        logger.info("Row {} - Flushing previous row before starting new row", i);
                        flushTestResult(testResultsOut, sNoBuf, testBuf, resultBuf, specBuf);
                    }
                    sNoBuf = testBuf = resultBuf = specBuf = "";
                    inRow = false;
                }

                sNoBuf = snoStr;
                testBuf = testStr;
                resultBuf = resultStr;
                specBuf = specStr;
                inRow = true;

                logger.info("Row {} - Started new row with buffers - SNO: '{}', TEST: '{}', RESULT: '{}', SPEC: '{}'",
                        i, sNoBuf, testBuf, resultBuf, specBuf);

                if (isParent && !hasValues && !KEEP_PARENT_HEADERS) {
                    logger.debug("Row {} - Skipping parent-only row", i);
                    sNoBuf = testBuf = resultBuf = specBuf = "";
                    inRow = false;
                }
            } else {
                if (!testStr.isEmpty() && !isFooterLine(testStr)) {
                    testBuf = append(testBuf, testStr);
                    logger.debug("Row {} - Appended to testBuf, now: '{}'", i, testBuf);
                }
                if (!resultStr.isEmpty() && !isFooterLine(resultStr)) {
                    resultBuf = append(resultBuf, resultStr);
                    logger.debug("Row {} - Appended to resultBuf, now: '{}'", i, resultBuf);
                }
                if (!specStr.isEmpty() && !isFooterLine(specStr)) {
                    specBuf = append(specBuf, specStr);
                    logger.info("Row {} - Appended to specBuf, now: '{}'", i, specBuf);
                }
            }
        }

        if (inRow && !sNoBuf.isEmpty()) {
            boolean parentOnly = sNoBuf.matches("^\\d+$") && resultBuf.isEmpty() && specBuf.isEmpty();
            if (!parentOnly || KEEP_PARENT_HEADERS) {
                logger.info("Flushing final row after loop completion");
                flushTestResult(testResultsOut, sNoBuf, testBuf, resultBuf, specBuf);
            }
        }

        logger.info("Extracted {} test results from page", testResultsOut.size());
    }

    private void flushTestResult(LinkedHashMap<String, TestResultDto> out,
                                 String sNo, String test, String result, String spec) {
        String s = normalize(sNo);
        String t = stripFooterFragments(normalize(test));
        String r = stripFooterFragments(normalize(result));
        String p = stripFooterFragments(normalize(spec));

        logger.info("Flushing test result - SNO: '{}', TEST: '{}', RESULT: '{}', SPEC (before): '{}', SPEC (after): '{}'",
                sNo, test, result, spec, p);

        if (t.isEmpty()) {
            logger.debug("Skipping test result with empty test name");
            return;
        }

        TestResultDto testResult = new TestResultDto();
        testResult.setSNo(s);
        testResult.setTest(t);
        testResult.setResult(r);
        testResult.setSpecification(p);

        String key = testResult.getSNo() + " - " + testResult.getTest();
        int counter = 1;
        String uniqueKey = key;
        while (out.containsKey(uniqueKey)) {
            uniqueKey = key + " #" + (++counter);
        }

        out.put(uniqueKey, testResult);
        logger.info("Added test result: S.No='{}', Test='{}', Result='{}', Specification='{}'", s, t, r, p);
    }

    private String sanitizeSno(String s) {
        if (s == null) return "";
        String[] parts = s.trim().split("\\s+");
        for (String p : parts) {
            if (p.matches("^(\\d+)(?:\\.\\d+)?$")) return p;
        }
        return "";
    }

    private String extractTrailingFromSno(String s) {
        if (s == null) return "";
        String[] parts = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        boolean firstNumFound = false;
        for (String p : parts) {
            if (!firstNumFound && p.matches("^(\\d+)(?:\\.\\d+)?$")) {
                firstNumFound = true;
                continue;
            }
            if (firstNumFound) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(p);
            }
        }
        return sb.toString();
    }

    private String normalize(String text) {
        if (text == null) return "";
        String t = text.trim();
        t = t.replaceAll("[:\\-]+$", "");
        t = t.replaceAll("\\s+", " ");
        t = t.replaceAll("\\(\\s+", "(").replaceAll("\\s+\\)", ")");
        return t;
    }

    private String stripFooterFragments(String text) {
        if (text == null) return "";
        String t = text;

        t = t.replaceAll("(?i)\\s*Remarks:.*$", "");
        t = t.replaceAll("(?i)\\s*Comment\\(s\\):.*$", "");
        t = t.replaceAll("(?i)\\s*Checked [Bb]y.*$", "");
        t = t.replaceAll("(?i)\\s*Approved [Bb]y.*$", "");
        t = t.replaceAll("(?i)\\s*Checked [Oo]n.*$", "");
        t = t.replaceAll("(?i)\\s*Approved [Oo]n.*$", "");
        t = t.replaceAll("(?i)\\s*Printed [Bb]y:.*$", "");
        t = t.replaceAll("(?i)\\s*Printed [Oo]n:.*$", "");
        t = t.replaceAll("(?i)\\s*Copy [Nn]o\\.?:.*$", "");
        t = t.replaceAll("(?i)\\s*Page [Nn]o\\.?:.*$", "");
        t = t.replaceAll("\\s*Date:.*$", "");

        t = t.replaceAll("(?i)\\s*[A-Za-z\\s]+(Pharma|Pharmaceuticals?)\\s+(Limited|Ltd\\.?|Pvt\\.?).*$", "");
        t = t.replaceAll("(?i)\\s*[A-Za-z\\s]+,\\s*Plot\\s+(no\\.?|Nos?\\.?).*$", "");
        t = t.replaceAll("(?i)\\s*[A-Za-z\\s]+\\s*-\\s*\\d{5,6}\\s*$", "");
        t = t.replaceAll("(?i)\\s*[A-Za-z\\s]+,\\s*[A-Za-z\\s]+\\s*-\\s*\\d{5,6}.*$", "");

        return t.trim();
    }

    private boolean isFooterLine(String line) {
        String l = line.trim().toLowerCase();

        if (l.isEmpty()) return false;

        // Match person names like "John.Doe Smith" but NOT numbers like "10.85 and" or "49.87 mg"
        if (l.matches("(?i).*[a-z]+\\.[a-z]+\\s+[a-z]+.*")) {
            return true;
        }

        if (l.matches("(?i).*(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\\s+\\d{1,2}\\s+\\d{4}.*")) {
            return true;
        }

        if (l.matches(".*\\d{1,2}:\\d{2}\\s*(am|pm).*")) {
            return true;
        }

        if (l.startsWith("remarks") || l.startsWith("comment(s)") ||
                l.contains("checked by") || l.contains("approved by") ||
                l.contains("checked on") || l.contains("approved on") ||
                l.contains("printed by") || l.contains("printed on") ||
                l.contains("copy no") || l.contains("page no") ||
                l.matches(".*\\bdate:.*")) {
            return true;
        }

        if (l.matches(".*\\bformat no\\..*") || l.matches(".*generated electronically.*")) {
            return true;
        }

        if (l.matches("(?i).*(pharma|pharmaceuticals?)\\s+(limited|ltd\\.?|pvt\\.?).*")) {
            return true;
        }

        if (l.matches("(?i).*plot\\s+(no\\.?|nos?\\.?|number).*")) {
            return true;
        }

        if (l.matches(".*\\d{5,6}\\s*$")) {
            return true;
        }

        if (l.matches("(?i).*[A-Za-z\\s]+\\s*-\\s*\\d{5,6}.*")) {
            return true;
        }

        if (l.contains(",") && l.split(",").length >= 3) {
            return true;
        }

        String[] companyKeywords = {"pharma", "limited", "ltd", "pvt", "inc", "corporation", "corp", "llc", "llp"};
        String[] addressKeywords = {"plot", "unit", "suite", "floor", "building", "street", "road", "avenue",
                "city", "state", "province", "country", "district", "dist", "mandal"};

        int companyCount = 0;
        int addressCount = 0;

        for (String keyword : companyKeywords) {
            if (l.contains(keyword)) companyCount++;
        }

        for (String keyword : addressKeywords) {
            if (l.contains(keyword)) addressCount++;
        }

        if (companyCount >= 2 || (companyCount >= 1 && addressCount >= 1)) {
            return true;
        }

        return false;
    }

    private String append(String base, String add) {
        String a = (add == null) ? "" : add.trim();
        if (a.isEmpty()) return base == null ? "" : base;
        if (base == null || base.isEmpty()) return a;
        return base + " " + a;
    }
}
