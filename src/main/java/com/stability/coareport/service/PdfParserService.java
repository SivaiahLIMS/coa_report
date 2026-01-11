package com.stability.coareport.service;

import com.stability.coareport.dto.ParsedPdfData;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PdfParserService {

    private static final Logger logger = LoggerFactory.getLogger(PdfParserService.class);
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

    public ParsedPdfData parsePdf(MultipartFile file) throws IOException {
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

            ParsedPdfData parsedData = new ParsedPdfData();
            LinkedHashMap<String, String> firstTable = new LinkedHashMap<>();
            LinkedHashMap<String, TestResultDto> secondTable = new LinkedHashMap<>();

            for (PageText page : pages) {
                parsePage(page, firstTable, secondTable);
            }

            mapFirstTableToParsedData(firstTable, parsedData);
            parsedData.setTestResults(new ArrayList<>(secondTable.values()));

            return parsedData;
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

    private void parsePage(PageText page, LinkedHashMap<String, String> firstTableOut,
                           LinkedHashMap<String, TestResultDto> secondTableOut) {
        int headerIdx = findSecondTableHeader(page.rows);

        if (firstTableOut.isEmpty()) {
            int endIdx = (headerIdx >= 0) ? headerIdx : page.rows.size();
            List<String> rawLines = page.rows.subList(0, endIdx).stream()
                    .map(r -> r.cells.stream().map(c -> c.text).collect(Collectors.joining(" ")).trim())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            List<String> mergedLines = mergeMultilineFields(rawLines);
            logger.info("=== MERGED LINES FOR HEADER PARSING (Total: {}) ===", mergedLines.size());
            for (int i = 0; i < Math.min(mergedLines.size(), 40); i++) {
                logger.info("Line {}: {}", i, mergedLines.get(i));
            }
            logger.info("=== END MERGED LINES ===");
            parseFirstTableDeterministic(mergedLines, firstTableOut);
        }

        parseSecondTableRows(page.rows, secondTableOut);
    }

    private List<String> mergeMultilineFields(List<String> lines) {
        List<String> merged = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String currentLine = lines.get(i);

            if (currentLine.equals("Condition period Date") && !merged.isEmpty()) {
                String prevLine = merged.get(merged.size() - 1);
                if (prevLine.startsWith("Storage ")) {
                    // Expected format: "Storage 25C/60%RH Schedule 646 DayS Schedule 15-DEC-2024"
                    // Need to produce: "Storage Condition 25C/60%RH Schedule period 646 DayS Schedule Date 15-DEC-2024"

                    String afterStorage = prevLine.substring("Storage ".length()).trim();

                    // Find first "Schedule"
                    int firstScheduleIdx = afterStorage.indexOf("Schedule ");
                    if (firstScheduleIdx > 0) {
                        String storageValue = afterStorage.substring(0, firstScheduleIdx).trim();
                        String afterFirstSchedule = afterStorage.substring(firstScheduleIdx + "Schedule ".length()).trim();

                        // Find second "Schedule"
                        int secondScheduleIdx = afterFirstSchedule.indexOf("Schedule ");
                        if (secondScheduleIdx > 0) {
                            String periodValue = afterFirstSchedule.substring(0, secondScheduleIdx).trim();
                            String dateValue = afterFirstSchedule.substring(secondScheduleIdx + "Schedule ".length()).trim();

                            String combined = String.format("Storage Condition %s Schedule period %s Schedule Date %s",
                                    storageValue, periodValue, dateValue);
                            logger.info("Merged 'Condition period Date' with previous line: {} -> {}", prevLine, combined);
                            merged.set(merged.size() - 1, combined);
                            continue;
                        }
                    }
                }
            }

            if (currentLine.startsWith("Condition ") && currentLine.contains("period") && currentLine.contains("Date") && !merged.isEmpty()) {
                String prevLine = merged.get(merged.size() - 1);
                if (prevLine.startsWith("Storage ")) {
                    String storageValue = prevLine.substring("Storage ".length()).trim();
                    String beforePeriod = storageValue.substring(0, storageValue.indexOf("Schedule")).trim();
                    String afterSchedule = storageValue.substring(storageValue.indexOf("Schedule")).trim();
                    String combined = "Storage Condition " + beforePeriod + " " +
                            afterSchedule.replace("Schedule ", "Schedule period ")
                                    .replace("Schedule period Schedule ", "Schedule period ");
                    merged.set(merged.size() - 1, combined + " Schedule Date");
                    continue;
                }
            }

            if (currentLine.equals("orientation Type") && !merged.isEmpty()) {
                String prevLine = merged.get(merged.size() - 1);
                if (prevLine.startsWith("Sample ")) {
                    String combined = prevLine.replace("Sample ", "Sample orientation ")
                            .replace("Packing ", "Packing Type ")
                            .replace("Packing Type Type ", "Packing Type ");
                    logger.info("Merged 'orientation Type' with previous line: {} -> {}", prevLine, combined);
                    merged.set(merged.size() - 1, combined);
                    continue;
                }
            }

            if (currentLine.startsWith("orientation ") && currentLine.contains("Type") && !merged.isEmpty()) {
                String prevLine = merged.get(merged.size() - 1);
                if (prevLine.startsWith("Sample ")) {
                    String combined = prevLine.replace("Sample ", "Sample orientation ")
                            .replace("Packing ", "Packing Type ");
                    merged.set(merged.size() - 1, combined);
                    continue;
                }
            }

            if (currentLine.equals("Name") && !merged.isEmpty()) {
                String prevLine = merged.get(merged.size() - 1);
                if (prevLine.startsWith("Product ")) {
                    String combined = "Product Name " + prevLine.substring("Product ".length()).trim();
                    merged.set(merged.size() - 1, combined);
                    continue;
                }
            }

            if (currentLine.equals("Code") && !merged.isEmpty()) {
                String prevLine = merged.get(merged.size() - 1);
                if (prevLine.startsWith("Product ")) {
                    String combined = "Product Code " + prevLine.substring("Product ".length()).trim();
                    merged.set(merged.size() - 1, combined);
                    continue;
                }
            }

            if (currentLine.startsWith("ID ") && !merged.isEmpty()) {
                String prevLine = merged.get(merged.size() - 1);
                if (prevLine.startsWith("Specification ")) {
                    String combined = "Specification ID " + prevLine.substring("Specification ".length()).trim() + " " + currentLine.substring("ID ".length()).trim();
                    merged.set(merged.size() - 1, combined);
                    continue;
                } else if (prevLine.contains("Protocol ") && !prevLine.contains("Protocol ID")) {
                    String combined = prevLine.replace("Protocol ", "Protocol ID ");
                    if (currentLine.length() > "ID ".length()) {
                        String restOfId = currentLine.substring("ID ".length()).trim();
                        combined = combined + restOfId;
                    }
                    merged.set(merged.size() - 1, combined);
                    continue;
                }
            }

            if (currentLine.equals("Condition") && !merged.isEmpty()) {
                String prevLine = merged.get(merged.size() - 1);
                if (prevLine.equals("Storage")) {
                    merged.set(merged.size() - 1, "Storage Condition");
                    continue;
                }
            }

            if (currentLine.startsWith("Condition ") && !merged.isEmpty()) {
                String prevLine = merged.get(merged.size() - 1);
                if (prevLine.equals("Storage")) {
                    merged.set(merged.size() - 1, "Storage Condition " + currentLine.substring("Condition ".length()).trim());
                    continue;
                }
            }

            if (currentLine.equals("orientation") && !merged.isEmpty()) {
                String prevLine = merged.get(merged.size() - 1);
                if (prevLine.equals("Sample")) {
                    merged.set(merged.size() - 1, "Sample orientation");
                    continue;
                }
            }

            if (currentLine.startsWith("orientation ") && !merged.isEmpty()) {
                String prevLine = merged.get(merged.size() - 1);
                if (prevLine.equals("Sample")) {
                    merged.set(merged.size() - 1, "Sample orientation " + currentLine.substring("orientation ".length()).trim());
                    continue;
                }
            }

            if (currentLine.equals("Type") && !merged.isEmpty()) {
                String prevLine = merged.get(merged.size() - 1);
                if (prevLine.endsWith("Packing")) {
                    merged.set(merged.size() - 1, prevLine + " Type");
                    continue;
                }
            }

            if (currentLine.startsWith("Type ") && !merged.isEmpty()) {
                String prevLine = merged.get(merged.size() - 1);
                if (prevLine.endsWith("Packing")) {
                    merged.set(merged.size() - 1, prevLine + " " + currentLine);
                    continue;
                }
            }

            if (currentLine.equals("period") && !merged.isEmpty()) {
                String prevLine = merged.get(merged.size() - 1);
                if (prevLine.contains("Schedule")) {
                    merged.set(merged.size() - 1, prevLine + " period");
                    continue;
                }
            }

            if (currentLine.startsWith("period ") && !merged.isEmpty()) {
                String prevLine = merged.get(merged.size() - 1);
                if (prevLine.contains("Schedule")) {
                    merged.set(merged.size() - 1, prevLine + " " + currentLine);
                    continue;
                }
            }

            if (currentLine.equals("Date") && !merged.isEmpty()) {
                String prevLine = merged.get(merged.size() - 1);
                if (prevLine.contains("Schedule")) {
                    merged.set(merged.size() - 1, prevLine + " Date");
                    continue;
                }
            }

            if (currentLine.startsWith("Date ") && !merged.isEmpty()) {
                String prevLine = merged.get(merged.size() - 1);
                if (prevLine.contains("Schedule")) {
                    merged.set(merged.size() - 1, prevLine + " " + currentLine);
                    continue;
                }
            }

            if (currentLine.equals("Size") && !merged.isEmpty()) {
                String prevLine = merged.get(merged.size() - 1);
                if (prevLine.endsWith("Pack")) {
                    merged.set(merged.size() - 1, prevLine + " Size");
                    continue;
                }
            }

            if (currentLine.startsWith("Size ") && !merged.isEmpty()) {
                String prevLine = merged.get(merged.size() - 1);
                if (prevLine.endsWith("Pack")) {
                    merged.set(merged.size() - 1, prevLine + " " + currentLine);
                    continue;
                }
            }

            if (currentLine.equals("vials") && !merged.isEmpty()) {
                String prevLine = merged.get(merged.size() - 1);
                if (prevLine.matches(".*\\d+$")) {
                    merged.set(merged.size() - 1, prevLine + " vials");
                    continue;
                }
            }

            if (currentLine.startsWith("vials ") && !merged.isEmpty()) {
                String prevLine = merged.get(merged.size() - 1);
                if (prevLine.contains("Batch Size") && prevLine.matches(".*\\d+$")) {
                    merged.set(merged.size() - 1, prevLine + " " + currentLine);
                    continue;
                }
            }

            // Skip footer lines
            if (isFooterLine(currentLine)) {
                logger.info("Skipping footer line: {}", currentLine);
                continue;
            }

            merged.add(currentLine);
        }

        return merged;
    }

    private int findSecondTableHeader(List<Row> rows) {
        for (int i = 0; i < rows.size(); i++) {
            String line = rows.get(i).cells.stream().map(c -> c.text).collect(Collectors.joining(" "));
            String normalized = line.replaceAll("\\s+", " ").toLowerCase().trim();

            boolean hasSNo = normalized.matches(".*s\\.?\\s*no\\.?.*");
            boolean hasTest = normalized.contains("test");
            boolean hasResult = normalized.contains("result");
            boolean hasSpec = normalized.contains("spec");

            if (hasTest || hasResult || hasSpec || hasSNo) {
                logger.info("Row {} [cells={}]: '{}' -> normalized: '{}' | S.No={}, Test={}, Result={}, Spec={}",
                        i, rows.get(i).cells.size(), line, normalized, hasSNo, hasTest, hasResult, hasSpec);
            }

            // Case 1: All header columns in one row
            if (hasSNo && hasTest && hasResult && hasSpec) {
                logger.info("Found table header at row {}: {}", i, line);
                return i;
            }

            // Case 2: Header columns split across consecutive rows
            if (hasSNo && !hasTest && !hasResult && !hasSpec && i + 3 < rows.size()) {
                String row1 = rows.get(i + 1).cells.stream().map(c -> c.text).collect(Collectors.joining(" ")).replaceAll("\\s+", " ").toLowerCase().trim();
                String row2 = rows.get(i + 2).cells.stream().map(c -> c.text).collect(Collectors.joining(" ")).replaceAll("\\s+", " ").toLowerCase().trim();
                String row3 = rows.get(i + 3).cells.stream().map(c -> c.text).collect(Collectors.joining(" ")).replaceAll("\\s+", " ").toLowerCase().trim();

                boolean row1HasTest = row1.contains("test");
                boolean row2HasResult = row2.contains("result");
                boolean row3HasSpec = row3.contains("spec");

                if (row1HasTest && row2HasResult && row3HasSpec) {
                    logger.info("Found split table header starting at row {}: S.No={}, TEST={}, RESULT={}, SPEC={}",
                            i, normalized, row1, row2, row3);
                    // Return the last header row (SPECIFICATION row) so data parsing starts after it
                    return i + 3;
                }
            }
        }
        logger.warn("Could not find table header row");
        return -1;
    }

    private void parseFirstTableDeterministic(List<String> lines, LinkedHashMap<String,String> out) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();

            // Check for standalone storage condition like "25C/60%RH"
            if (line.matches("^\\d+C/\\d+%RH$")) {
                logger.info("Line {} '{}' matches storage pattern! Already have Storage Condition? {}", i, line, out.containsKey("Storage Condition"));
                if (!out.containsKey("Storage Condition")) {
                    out.put("Storage Condition", line);
                    logger.info("Found standalone Storage Condition (exact match): {}", line);
                    continue;
                } else {
                    logger.info("Skipping - Storage Condition already set to: '{}'", out.get("Storage Condition"));
                }
            }

            // Also check for variations with degree symbol or spaces
            if (line.matches("^\\d+°?C\\s*/\\s*\\d+%\\s*RH$") && !out.containsKey("Storage Condition")) {
                out.put("Storage Condition", line);
                logger.info("Found standalone Storage Condition (variant): {}", line);
                continue;
            }

            // Check for Specification followed by ID on next line or line after that
            if (line.startsWith("Specification ") && !line.contains("Batch") && !out.containsKey("Specification ID")) {
                String specValue = line.substring("Specification ".length()).trim();

                // Handle "Specification ID" prefix specially
                if (specValue.startsWith("ID ")) {
                    specValue = specValue.substring("ID ".length()).trim();
                }

                logger.info("Found Specification line: '{}', extracted value: '{}'", line, specValue);

                // Check next line (i+1)
                if (i + 1 < lines.size()) {
                    String nextLine = lines.get(i + 1).trim();
                    logger.info("Checking next line (i+1): '{}'", nextLine);

                    // Case 1: Next line starts with "ID "
                    if (nextLine.startsWith("ID ")) {
                        String idValue = nextLine.substring("ID ".length()).trim();
                        String fullSpec = specValue + idValue;
                        out.put("Specification ID", fullSpec);
                        logger.info("Found Specification ID from merged lines (i+1): {}", fullSpec);
                        i++;
                        continue;
                    }
                    // Case 2: Next line is Batch Size, check i+2 for continuation
                    else if (nextLine.contains("Batch Size")) {
                        if (i + 2 < lines.size()) {
                            String lineAfterNext = lines.get(i + 2).trim();
                            logger.info("Checking line after next (i+2): '{}'", lineAfterNext);

                            // Extract Batch Size and Protocol ID from i+1
                            if (nextLine.contains("Protocol")) {
                                String afterBatchLabel = nextLine.substring(nextLine.indexOf("Batch Size") + "Batch Size".length()).trim();
                                String batchSize = before(afterBatchLabel, "Protocol").trim();
                                out.put("Batch Size", batchSize);
                                logger.info("Extracted Batch Size from line i+1: {}", batchSize);

                                String protocolMarker = nextLine.contains("Protocol ID") ? "Protocol ID" : "Protocol ";
                                int protocolIndex = afterBatchLabel.indexOf(protocolMarker);
                                if (protocolIndex >= 0) {
                                    String protocolId = afterBatchLabel.substring(protocolIndex + protocolMarker.length()).trim();
                                    out.put("Protocol ID", protocolId);
                                    logger.info("Extracted Protocol ID from line i+1: {}", protocolId);
                                }
                            }

                            // Check if i+2 is the continuation of spec ID (with or without "ID " prefix)
                            if (!lineAfterNext.isEmpty() && !lineAfterNext.contains("Storage") &&
                                    !lineAfterNext.contains("Schedule") && !lineAfterNext.contains("Sample")) {
                                String idValue = lineAfterNext.startsWith("ID ") ?
                                        lineAfterNext.substring("ID ".length()).trim() : lineAfterNext;
                                // Extract only the first word if there are spaces
                                idValue = idValue.contains(" ") ? idValue.substring(0, idValue.indexOf(" ")).trim() : idValue;
                                String fullSpec = specValue + idValue;
                                out.put("Specification ID", fullSpec);
                                logger.info("Found Specification ID from merged lines (i+2): {} (from full line: '{}')", fullSpec, lineAfterNext);
                                i += 2;
                                continue;
                            }
                        }
                    }
                    // Case 3: Next line is directly the continuation (no "ID " prefix, not Batch Size)
                    else if (!nextLine.isEmpty() && !nextLine.contains("Protocol") &&
                            !nextLine.contains("Storage") && !nextLine.contains("Schedule")) {
                        String idValue = nextLine.contains(" ") ? nextLine.substring(0, nextLine.indexOf(" ")).trim() : nextLine;
                        String fullSpec = specValue + idValue;
                        out.put("Specification ID", fullSpec);
                        logger.info("Found Specification ID from continuation line (no ID prefix): {}", fullSpec);
                        i++;
                        continue;
                    }
                }
            }

            if (line.startsWith("Product Name")) {
                String afterLabel = line.substring("Product Name".length()).trim();
                out.put("Product Name", afterLabel);
            } else if (line.startsWith("Product ") && line.endsWith(" Name")) {
                out.put("Product Name", line.substring("Product ".length(), line.length() - " Name".length()).trim());
            }
            if (line.startsWith("Product Code") && line.contains("B.No.") && line.contains("AR No.")) {
                String afterProduct = line.substring("Product Code".length()).trim();
                String code = before(afterProduct, "B.No.").trim();
                out.put("Product Code", code);
                String afterBnoLabel = afterProduct.substring(afterProduct.indexOf("B.No.") + "B.No.".length()).trim();
                String bno = before(afterBnoLabel, "AR No.").trim();
                out.put("B.No.", bno);
                String arNo = afterBnoLabel.substring(afterBnoLabel.indexOf("AR No.") + "AR No.".length()).trim();
                out.put("AR No.", arNo);
            } else if (line.startsWith("Product ") && line.contains("B.No.") && line.contains("AR No.")) {
                String afterProduct = line.substring("Product ".length()).trim();
                String code = before(afterProduct, "B.No.").trim();
                out.put("Product Code", code);
                String afterBnoLabel = afterProduct.substring(afterProduct.indexOf("B.No.") + "B.No.".length()).trim();
                String bno = before(afterBnoLabel, "AR No.").trim();
                out.put("B.No.", bno);
                String arNo = afterBnoLabel.substring(afterBnoLabel.indexOf("AR No.") + "AR No.".length()).trim();
                out.put("AR No.", arNo);
            }
            if (line.startsWith("Specification ID") && line.contains("Batch Size") && (line.contains("Protocol ID") || line.contains("Protocol "))) {
                String afterSpec = line.substring("Specification ID".length()).trim();
                String specId = before(afterSpec, "Batch Size").trim();
                out.put("Specification ID", specId);
                String afterBatchLabel = afterSpec.substring(afterSpec.indexOf("Batch Size") + "Batch Size".length()).trim();

                String protocolMarker = line.contains("Protocol ID") ? "Protocol ID" : "Protocol ";
                String batchSize = before(afterBatchLabel, protocolMarker).trim();
                out.put("Batch Size", batchSize);

                int protocolIndex = afterBatchLabel.indexOf(protocolMarker);
                if (protocolIndex >= 0) {
                    String afterProtocolLabel = afterBatchLabel.substring(protocolIndex + protocolMarker.length()).trim();
                    out.put("Protocol ID", afterProtocolLabel);
                }
            } else if (line.startsWith("Specification ") && line.contains("Batch Size") && (line.contains("Protocol ID") || line.contains("Protocol "))) {
                String afterSpec = line.substring("Specification ".length()).trim();
                String specId = before(afterSpec, "Batch Size").trim();
                out.put("Specification ID", specId);
                String afterBatchLabel = afterSpec.substring(afterSpec.indexOf("Batch Size") + "Batch Size".length()).trim();

                String protocolMarker = line.contains("Protocol ID") ? "Protocol ID" : "Protocol ";
                String batchSize = before(afterBatchLabel, protocolMarker).trim();
                out.put("Batch Size", batchSize);

                int protocolIndex = afterBatchLabel.indexOf(protocolMarker);
                if (protocolIndex >= 0) {
                    String afterProtocolLabel = afterBatchLabel.substring(protocolIndex + protocolMarker.length()).trim();
                    out.put("Protocol ID", afterProtocolLabel);
                }
            }
            if (line.startsWith("Batch Size") && !out.containsKey("Batch Size")) {
                String afterBatch = line.substring("Batch Size".length()).trim();
                String batchSize;
                if (afterBatch.contains("Protocol")) {
                    batchSize = before(afterBatch, "Protocol").trim();
                } else {
                    batchSize = afterBatch;
                }
                out.put("Batch Size", batchSize);
                logger.info("Found Batch Size: {}", batchSize);

                if (afterBatch.contains("Protocol ID")) {
                    String protocolId = afterBatch.substring(afterBatch.indexOf("Protocol ID") + "Protocol ID".length()).trim();
                    out.put("Protocol ID", protocolId);
                    logger.info("Found Protocol ID: {}", protocolId);
                } else if (afterBatch.contains("Protocol ")) {
                    String protocolId = afterBatch.substring(afterBatch.indexOf("Protocol ") + "Protocol ".length()).trim();
                    out.put("Protocol ID", protocolId);
                    logger.info("Found Protocol ID: {}", protocolId);
                }
            }
            if (line.startsWith("STP No. ")) {
                out.put("STP No.", line.substring("STP No. ".length()).trim());
            }
            if (line.startsWith("Mfg Date ")) {
                String afterMfg = line.substring("Mfg Date ".length()).trim();
                String mfg = before(afterMfg, "Exp.Date").trim();
                out.put("Mfg Date", mfg);
                String exp = afterMfg.substring(afterMfg.indexOf("Exp.Date") + "Exp.Date".length()).trim();
                out.put("Exp.Date", exp);
            }
            if (line.startsWith("Storage Condition") && line.contains("Schedule")) {
                logger.info("Found Storage Condition line: {}", line);
                String afterStorage = line.substring("Storage Condition".length()).trim();
                String cond = before(afterStorage, "Schedule").trim();
                // Only set Storage Condition if it's not empty
                if (!cond.isEmpty() && !out.containsKey("Storage Condition")) {
                    String normalizedCond = normalizeStorageCondition(cond);
                    out.put("Storage Condition", normalizedCond);
                    logger.info("Extracted Storage Condition: {} (normalized: {})", cond, normalizedCond);
                } else {
                    logger.info("Storage Condition extracted as empty or already set, skipping: '{}'", cond);
                }

                String afterFirstScheduleLabel = afterStorage.substring(afterStorage.indexOf("Schedule") + "Schedule".length()).trim();
                if (afterFirstScheduleLabel.startsWith("period ")) {
                    afterFirstScheduleLabel = afterFirstScheduleLabel.substring("period ".length()).trim();
                }

                if (afterFirstScheduleLabel.contains("Schedule")) {
                    String period = before(afterFirstScheduleLabel, "Schedule").trim();
                    out.put("Schedule period", period);
                    logger.info("Extracted Schedule period: {}", period);

                    String afterSecondSchedule = afterFirstScheduleLabel.substring(afterFirstScheduleLabel.indexOf("Schedule") + "Schedule".length()).trim();
                    if (afterSecondSchedule.startsWith("Date ")) {
                        afterSecondSchedule = afterSecondSchedule.substring("Date ".length()).trim();
                    }
                    // Remove trailing "period Date" text if present
                    if (afterSecondSchedule.endsWith("period Date")) {
                        afterSecondSchedule = afterSecondSchedule.substring(0, afterSecondSchedule.length() - "period Date".length()).trim();
                    }
                    out.put("Schedule Date", afterSecondSchedule);
                    logger.info("Extracted Schedule Date: {}", afterSecondSchedule);
                } else {
                    out.put("Schedule period", afterFirstScheduleLabel);
                    logger.info("Extracted Schedule period (no second Schedule): {}", afterFirstScheduleLabel);
                }
            } else if (line.startsWith("Storage ") && line.contains("Schedule")) {
                logger.info("Found Storage line: {}", line);
                String afterStorage = line.substring("Storage ".length()).trim();
                String cond = before(afterStorage, "Schedule").trim();
                // Only set Storage Condition if it's not empty
                if (!cond.isEmpty() && !out.containsKey("Storage Condition")) {
                    String normalizedCond = normalizeStorageCondition(cond);
                    out.put("Storage Condition", normalizedCond);
                    logger.info("Extracted Storage Condition from 'Storage ...' line: {} (normalized: {})", cond, normalizedCond);
                } else {
                    logger.info("Storage Condition extracted as empty or already set, checking next line: '{}'", cond);
                    // Check if next line contains storage condition pattern (e.g., "25C/60%RH")
                    if (cond.isEmpty() && i + 1 < lines.size()) {
                        String nextLine = lines.get(i + 1).trim();
                        logger.info("Checking next line for standalone storage condition: '{}'", nextLine);

                        // Log character codes to debug special characters
                        StringBuilder charCodes = new StringBuilder();
                        for (int j = 0; j < Math.min(nextLine.length(), 10); j++) {
                            charCodes.append(String.format("'%c'=%d ", nextLine.charAt(j), (int)nextLine.charAt(j)));
                        }
                        logger.info("First 10 character codes: {}", charCodes.toString());

                        // Check if line looks like a storage condition (contains digits, /, %)
                        // This is more flexible than strict regex matching
                        boolean looksLikeStorageCondition = nextLine.contains("/") &&
                                nextLine.contains("%") &&
                                nextLine.matches(".*\\d+.*");
                        logger.info("Looks like storage condition: {}", looksLikeStorageCondition);

                        if (looksLikeStorageCondition) {
                            // Normalize storage condition: replace special characters with degree symbol
                            String normalizedCondition = normalizeStorageCondition(nextLine);
                            out.put("Storage Condition", normalizedCondition);
                            logger.info("Found standalone Storage Condition on next line: {} (normalized: {})", nextLine, normalizedCondition);
                        } else {
                            logger.info("Line '{}' did not match storage condition pattern", nextLine);
                        }
                    }
                }

                String afterFirstScheduleLabel = afterStorage.substring(afterStorage.indexOf("Schedule") + "Schedule".length()).trim();
                if (afterFirstScheduleLabel.contains("Schedule")) {
                    String period = before(afterFirstScheduleLabel, "Schedule").trim();
                    out.put("Schedule period", period);
                    String scheduleDate = afterFirstScheduleLabel.substring(afterFirstScheduleLabel.indexOf("Schedule") + "Schedule".length()).trim();
                    // Remove trailing "period Date" text if present
                    if (scheduleDate.endsWith("period Date")) {
                        scheduleDate = scheduleDate.substring(0, scheduleDate.length() - "period Date".length()).trim();
                    }
                    out.put("Schedule Date", scheduleDate);
                } else {
                    out.put("Schedule period", afterFirstScheduleLabel);
                }

                // If we found storage condition on next line, skip it
                if (out.containsKey("Storage Condition") && !out.get("Storage Condition").isEmpty() && i + 1 < lines.size()) {
                    String nextLine = lines.get(i + 1).trim();
                    if (nextLine.equals(out.get("Storage Condition"))) {
                        i++; // Skip the next line since we've processed it
                    }
                }
            }
            if (line.startsWith("Sample orientation") && line.contains("Packing")) {
                logger.info("Found Sample orientation line: {}", line);
                String afterSample = line.substring("Sample orientation".length()).trim();
                String orientation = before(afterSample, "Packing").trim();
                out.put("Sample orientation", orientation);

                String afterPackingLabel = afterSample.substring(afterSample.indexOf("Packing") + "Packing".length()).trim();
                if (afterPackingLabel.startsWith("Type ")) {
                    afterPackingLabel = afterPackingLabel.substring("Type ".length()).trim();
                }

                if (afterPackingLabel.contains("Pack Size")) {
                    String packType = before(afterPackingLabel, "Pack Size").trim();
                    out.put("Packing Type", packType);
                    logger.info("Extracted Packing Type: {}", packType);

                    String afterPackSizeLabel = afterPackingLabel.substring(afterPackingLabel.indexOf("Pack Size") + "Pack Size".length()).trim();
                    out.put("Pack Size", afterPackSizeLabel);
                    logger.info("Extracted Pack Size: {}", afterPackSizeLabel);
                } else {
                    out.put("Packing Type", afterPackingLabel);
                    logger.info("Extracted Packing Type (no Pack Size): {}", afterPackingLabel);
                }
            } else if (line.startsWith("Sample ") && line.contains("Packing")) {
                logger.info("Found Sample line: {}", line);
                String afterSample = line.substring("Sample ".length()).trim();
                String orientation = before(afterSample, "Packing").trim();
                out.put("Sample orientation", orientation);

                String afterPackingLabel = afterSample.substring(afterSample.indexOf("Packing") + "Packing".length()).trim();
                if (afterPackingLabel.contains("Pack Size")) {
                    String packType = before(afterPackingLabel, "Pack Size").trim();
                    out.put("Packing Type", packType);

                    String packSize = afterPackingLabel.substring(afterPackingLabel.indexOf("Pack Size") + "Pack Size".length()).trim();
                    out.put("Pack Size", packSize);
                } else {
                    out.put("Packing Type", afterPackingLabel);
                }
            }
            if (line.startsWith("Remarks:")) {
                out.put("Remarks", line.substring("Remarks:".length()).trim());
            }
            if (line.contains("Checked by") && line.contains("Approved by")) {
                String afterChecked = line.substring(line.indexOf("Checked by") + "Checked by".length()).trim();
                if (afterChecked.startsWith(":")) afterChecked = afterChecked.substring(1).trim();
                String checkedBy = before(afterChecked, "Approved by").trim();
                out.put("Checked by", checkedBy);
                String afterApproved = afterChecked.substring(afterChecked.indexOf("Approved by") + "Approved by".length()).trim();
                if (afterApproved.startsWith(":")) afterApproved = afterApproved.substring(1).trim();
                out.put("Approved by", afterApproved);
            }
        }

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            String lineLower = line.toLowerCase();

            if (!out.containsKey("Schedule period")) {
                if (line.startsWith("Schedule period ")) {
                    out.put("Schedule period", line.substring("Schedule period ".length()).trim());
                    logger.info("Fallback extracted Schedule period: {}", out.get("Schedule period"));
                } else if (lineLower.contains("schedule") && lineLower.contains("period")) {
                    String schedulePattern = "(?i)schedule\\s+period\\s+(.+?)(?=\\s+schedule\\s+date|$)";
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(schedulePattern);
                    java.util.regex.Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        out.put("Schedule period", matcher.group(1).trim());
                        logger.info("Regex extracted Schedule period: {}", out.get("Schedule period"));
                    }
                }
            }

            if (!out.containsKey("Schedule Date")) {
                if (line.startsWith("Schedule Date ")) {
                    out.put("Schedule Date", line.substring("Schedule Date ".length()).trim());
                    logger.info("Fallback extracted Schedule Date: {}", out.get("Schedule Date"));
                } else if (lineLower.contains("schedule") && lineLower.contains("date")) {
                    String datePattern = "(?i)schedule\\s+date\\s+(.+)";
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(datePattern);
                    java.util.regex.Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        out.put("Schedule Date", matcher.group(1).trim());
                        logger.info("Regex extracted Schedule Date: {}", out.get("Schedule Date"));
                    }
                }
            }

            if (!out.containsKey("Packing Type")) {
                if (line.startsWith("Packing Type ")) {
                    String afterPackingType = line.substring("Packing Type ".length()).trim();
                    if (afterPackingType.contains("Pack Size")) {
                        String packType = before(afterPackingType, "Pack Size").trim();
                        out.put("Packing Type", packType);
                        String packSize = afterPackingType.substring(afterPackingType.indexOf("Pack Size") + "Pack Size".length()).trim();
                        out.put("Pack Size", packSize);
                        logger.info("Fallback extracted Packing Type: {} and Pack Size: {}", packType, packSize);
                    } else {
                        out.put("Packing Type", afterPackingType);
                        logger.info("Fallback extracted Packing Type: {}", afterPackingType);
                    }
                } else if (lineLower.contains("packing") && lineLower.contains("type")) {
                    String packingPattern = "(?i)packing\\s+type\\s+(.+?)(?=\\s+pack\\s+size|$)";
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(packingPattern);
                    java.util.regex.Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        out.put("Packing Type", matcher.group(1).trim());
                        logger.info("Regex extracted Packing Type: {}", out.get("Packing Type"));
                    }
                }
            }

            if (!out.containsKey("Pack Size")) {
                if (line.startsWith("Pack Size ")) {
                    out.put("Pack Size", line.substring("Pack Size ".length()).trim());
                    logger.info("Fallback extracted Pack Size: {}", out.get("Pack Size"));
                } else if (lineLower.contains("pack") && lineLower.contains("size")) {
                    String sizePattern = "(?i)pack\\s+size\\s+(.+)";
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(sizePattern);
                    java.util.regex.Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        out.put("Pack Size", matcher.group(1).trim());
                        logger.info("Regex extracted Pack Size: {}", out.get("Pack Size"));
                    }
                }
            }

            if (line.startsWith("Date:")) {
                String dateStr = line.substring("Date:".length()).trim();
                if (!out.containsKey("Check Date")) {
                    out.put("Check Date", dateStr);
                } else if (!out.containsKey("Approval Date")) {
                    out.put("Approval Date", dateStr);
                }
            }
        }
    }

    private void parseSecondTableRows(List<Row> rows, LinkedHashMap<String, TestResultDto> out) {
        float xSno = 0f, xTest = 120f, xResult = 320f, xSpec = 450f;
        int headerRowIndex = -1;

        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            String joined = r.cells.stream().map(c -> c.text).collect(Collectors.joining(" "));
            String normalized = joined.replaceAll("\\s+", " ").toLowerCase();

            boolean hasSNo = normalized.matches(".*s\\.?\\s*no\\.?.*");
            boolean hasTest = normalized.contains("test");
            boolean hasResult = normalized.contains("result");
            boolean hasSpec = normalized.contains("spec");

            // Case 1: All header columns in one row
            if (hasSNo && hasTest && hasResult && hasSpec) {
                headerRowIndex = i;
                logger.info("Found table header for column detection at row {}: {}", i, joined);
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

            // Case 2: Header columns split across consecutive rows
            if (hasSNo && !hasTest && !hasResult && !hasSpec && i + 3 < rows.size()) {
                String row1 = rows.get(i + 1).cells.stream().map(c -> c.text).collect(Collectors.joining(" ")).replaceAll("\\s+", " ").toLowerCase().trim();
                String row2 = rows.get(i + 2).cells.stream().map(c -> c.text).collect(Collectors.joining(" ")).replaceAll("\\s+", " ").toLowerCase().trim();
                String row3 = rows.get(i + 3).cells.stream().map(c -> c.text).collect(Collectors.joining(" ")).replaceAll("\\s+", " ").toLowerCase().trim();

                boolean row1HasTest = row1.contains("test");
                boolean row2HasResult = row2.contains("result");
                boolean row3HasSpec = row3.contains("spec");

                if (row1HasTest && row2HasResult && row3HasSpec) {
                    headerRowIndex = i + 3;
                    logger.info("Found split table header for column detection starting at row {}: S.No={}, TEST={}, RESULT={}, SPEC={}",
                            i, normalized, row1, row2, row3);

                    // Extract X positions from each row's first cell
                    if (!rows.get(i).cells.isEmpty()) xSno = rows.get(i).cells.get(0).x;
                    if (!rows.get(i + 1).cells.isEmpty()) xTest = rows.get(i + 1).cells.get(0).x;
                    if (!rows.get(i + 2).cells.isEmpty()) xResult = rows.get(i + 2).cells.get(0).x;
                    if (!rows.get(i + 3).cells.isEmpty()) xSpec = rows.get(i + 3).cells.get(0).x;

                    logger.info("Column positions - S.No: {}, TEST: {}, RESULT: {}, SPEC: {}", xSno, xTest, xResult, xSpec);
                    break;
                }
            }
        }

        if (headerRowIndex == -1) {
            logger.warn("Table header not found, cannot parse test results");
            return;
        }

        float cut1 = (xSno + xTest) / 2f;
        float cut2 = (xTest + xResult) / 2f;
        float cut3 = (xResult + xSpec) / 2f;

        logger.info("Column positions - S.No: {}, TEST: {}, RESULT: {}, SPEC: {}", xSno, xTest, xResult, xSpec);
        logger.info("Cut points - cut1 (SNO/TEST): {}, cut2 (TEST/RESULT): {}, cut3 (RESULT/SPEC): {}", cut1, cut2, cut3);

        String sNoBuf = "", testBuf = "", resultBuf = "", specBuf = "";
        boolean inRow = false;

        logger.info("Starting to parse data rows from row {} onwards", headerRowIndex + 1);

        for (int i = headerRowIndex + 1; i < rows.size(); i++) {
            Row r = rows.get(i);
            String joinedLine = r.cells.stream().map(c -> c.text.trim()).collect(Collectors.joining(" "));
            if (joinedLine.isEmpty() || isFooterLine(joinedLine)) {
                logger.debug("Skipping row {}: {}", i, joinedLine.isEmpty() ? "(empty)" : "(footer)");
                continue;
            }

            List<String> snoTokens = new ArrayList<>(), testTokens = new ArrayList<>(),
                    resultTokens = new ArrayList<>(), specTokens = new ArrayList<>();
            for (Cell c : r.cells) {
                String t = c.text.trim();
                if (t.isEmpty()) continue;
                if (c.x <= cut1) {
                    snoTokens.add(t);
                    logger.trace("Row {} - Cell at x={} ('{}') -> SNO column", i, c.x, t);
                }
                else if (c.x <= cut2) {
                    testTokens.add(t);
                    logger.trace("Row {} - Cell at x={} ('{}') -> TEST column", i, c.x, t);
                }
                else if (c.x <= cut3) {
                    resultTokens.add(t);
                    logger.trace("Row {} - Cell at x={} ('{}') -> RESULT column", i, c.x, t);
                }
                else {
                    specTokens.add(t);
                    logger.trace("Row {} - Cell at x={} ('{}') -> SPEC column", i, c.x, t);
                }
            }

            String rawSnoStr = String.join(" ", snoTokens).trim();
            String snoStr = sanitizeSno(rawSnoStr);
            String testStr = String.join(" ", testTokens).trim();
            String resultStr = String.join(" ", resultTokens).trim();
            String specStr = String.join(" ", specTokens).trim();

            logger.debug("Row {} - SNO tokens: {}, TEST tokens: {}, RESULT tokens: {}, SPEC tokens: {}",
                    i, snoTokens, testTokens, resultTokens, specTokens);
            logger.debug("Row {} - Joined - SNO: '{}', TEST: '{}', RESULT: '{}', SPEC: '{}'",
                    i, snoStr, testStr, resultStr, specStr);

            String trailingFromSno = trailingFromSnoIntoTest(rawSnoStr);
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
                        logger.info("Flushing previous row before starting new row");
                        flushSecondTableRow(out, sNoBuf, testBuf, resultBuf, specBuf);
                    }
                    sNoBuf = testBuf = resultBuf = specBuf = "";
                    inRow = false;
                }

                sNoBuf = snoStr;
                testBuf = testStr;
                resultBuf = resultStr;
                specBuf = specStr;
                inRow = true;

                logger.debug("Row {} - Started new row with buffers - SNO: '{}', TEST: '{}', RESULT: '{}', SPEC: '{}'",
                        i, sNoBuf, testBuf, resultBuf, specBuf);

                if (isParent && !hasValues && !KEEP_PARENT_HEADERS) {
                    logger.debug("Row {} - Skipping parent-only row", i);
                    sNoBuf = testBuf = resultBuf = specBuf = "";
                    inRow = false;
                }
            } else {
                // Filter out footer content before appending
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
                    logger.debug("Row {} - Appended to specBuf, now: '{}'", i, specBuf);
                }
            }
        }

        if (inRow && !sNoBuf.isEmpty()) {
            boolean parentOnly = sNoBuf.matches("^\\d+$") && resultBuf.isEmpty() && specBuf.isEmpty();
            if (!parentOnly || KEEP_PARENT_HEADERS) {
                logger.info("Flushing final row after loop completion");
                flushSecondTableRow(out, sNoBuf, testBuf, resultBuf, specBuf);
            }
        }
    }

    private void mapFirstTableToParsedData(LinkedHashMap<String, String> firstTable, ParsedPdfData parsedData) {
        parsedData.setProductName(firstTable.getOrDefault("Product Name", ""));
        parsedData.setProductCode(firstTable.getOrDefault("Product Code", ""));
        parsedData.setArNo(firstTable.getOrDefault("AR No.", ""));
        parsedData.setBatchNo(firstTable.getOrDefault("B.No.", ""));
        parsedData.setBatchSize(firstTable.getOrDefault("Batch Size", ""));
        parsedData.setMfgDate(firstTable.getOrDefault("Mfg Date", ""));
        parsedData.setExpDate(firstTable.getOrDefault("Exp.Date", ""));
        parsedData.setSpecification(firstTable.getOrDefault("Specification ID", ""));
        parsedData.setStorageCondition(firstTable.getOrDefault("Storage Condition", ""));
        parsedData.setSampleQty(firstTable.getOrDefault("Sample orientation", ""));
        parsedData.setReceivedDate(firstTable.getOrDefault("Schedule Date", ""));
        parsedData.setProtocolId(firstTable.getOrDefault("Protocol ID", ""));
        parsedData.setStpNo(firstTable.getOrDefault("STP No.", ""));
        parsedData.setSchedulePeriod(firstTable.getOrDefault("Schedule period", ""));
        parsedData.setPackingType(firstTable.getOrDefault("Packing Type", ""));
        parsedData.setPackSize(firstTable.getOrDefault("Pack Size", ""));
        parsedData.setRemarks(firstTable.getOrDefault("Remarks", ""));
        parsedData.setCheckedBy(firstTable.getOrDefault("Checked by", ""));
        parsedData.setApprovedBy(firstTable.getOrDefault("Approved by", ""));
        parsedData.setCheckDate(firstTable.getOrDefault("Check Date", ""));
        parsedData.setApprovalDate(firstTable.getOrDefault("Approval Date", ""));
        parsedData.setAnalysisStartDate("");
        parsedData.setAnalysisEndDate("");
    }

    private String before(String text, String marker) {
        int idx = text.indexOf(marker);
        if (idx < 0) return text;
        return text.substring(0, idx);
    }

    private String normalize(String text) {
        if (text == null) return "";
        String t = text.trim();
        t = t.replaceAll("[:\\-]+$", "");
        t = t.replaceAll("\\s+", " ");
        t = t.replaceAll("\\(\\s+", "(").replaceAll("\\s+\\)", ")");
        return t;
    }

    private String normalizeStorageCondition(String condition) {
        if (condition == null) return "";
        String normalized = condition.trim();

        // Replace various non-standard characters that appear before C/F with degree symbol
        // This handles cases like "25□C" or special Unicode characters
        // Pattern: digit(s) followed by any non-letter character (except space) followed by C/F
        normalized = normalized.replaceAll("(\\d+)[^\\w\\s°](C|F)", "$1°$2");

        // Also handle cases where degree symbol variants exist
        normalized = normalized.replaceAll("℃", "°C");
        normalized = normalized.replaceAll("℉", "°F");

        // If no special character was found but we have digit directly followed by C/F, add degree symbol
        normalized = normalized.replaceAll("(\\d+)([CF])/", "$1°$2/");

        return normalized;
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

    private void flushSecondTableRow(LinkedHashMap<String, TestResultDto> out,
                                     String sNo, String test, String result, String spec) {
        String s = normalize(sNo);
        String t = stripFooterFragments(normalize(test));
        String r = stripFooterFragments(normalize(result));
        String p = stripFooterFragments(normalize(spec));

        logger.info("Flushing row - SNO: '{}', TEST: '{}', RESULT: '{}', SPEC (before): '{}', SPEC (after): '{}'",
                sNo, test, result, spec, p);

        TestResultDto rec = new TestResultDto();
        rec.setSNo(s);
        rec.setTest(t);
        rec.setResult(r);
        rec.setSpecification(p);

        String key = rec.getSNo() + " - " + rec.getTest();
        int counter = 1;
        String uniqueKey = key;
        while (out.containsKey(uniqueKey)) uniqueKey = key + " #" + (++counter);
        out.put(uniqueKey, rec);
    }

    private String sanitizeSno(String s) {
        if (s == null) return "";
        String[] parts = s.trim().split("\\s+");
        for (String p : parts) {
            if (p.matches("^(\\d+)(?:\\.\\d+)?$")) return p;
        }
        return "";
    }

    private String trailingFromSnoIntoTest(String s) {
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
}
