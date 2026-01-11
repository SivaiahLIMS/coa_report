package example.pdf;


import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class COAExtractorAllPage119 {

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

    public static class PDFRecordCOAReport {
        public final String sNo;
        public final String test;
        public final String result;
        public final String specification;

        public PDFRecordCOAReport(String sNo, String test, String result, String specification) {
            this.sNo = sNo;
            this.test = test;
            this.result = result;
            this.specification = specification;
        }

        @Override
        public String toString() {
            return "PDFRecordCOAReport{" +
                    "sNo='" + sNo + '\'' +
                    ", test='" + test + '\'' +
                    ", result='" + result + '\'' +
                    ", specification='" + specification + '\'' +
                    '}';
        }
    }

    public static class Result {
        public final LinkedHashMap<String, String> firstTable = new LinkedHashMap<>();
        public final LinkedHashMap<String, PDFRecordCOAReport> secondTable = new LinkedHashMap<>();
    }

    public static Result extract(File pdfFile) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdfFile)) {
            PositionStripper stripper = new PositionStripper();
            stripper.getText(doc);

            Result result = new Result();
            for (PageText page : stripper.getPages()) {
                parsePage(page, result.firstTable, result.secondTable);
            }
            return result;
        }
    }

    private static void parsePage(PageText page,
                                  LinkedHashMap<String, String> firstTableOut,
                                  LinkedHashMap<String, PDFRecordCOAReport> secondTableOut) {

        int headerIdx = findSecondTableHeader(page.rows);

        if (firstTableOut.isEmpty()) {
            int endIdx = (headerIdx >= 0) ? headerIdx : page.rows.size();
            List<String> rawLines = page.rows.subList(0, endIdx).stream()
                    .map(r -> r.cells.stream().map(c -> c.text).collect(Collectors.joining(" ")).trim())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            parseFirstTableDeterministic(rawLines, firstTableOut);
        }

        parseSecondTableRows(page.rows, secondTableOut);
    }

    private static int findSecondTableHeader(List<Row> rows) {
        for (int i = 0; i < rows.size(); i++) {
            String line = rows.get(i).cells.stream().map(c -> c.text).collect(Collectors.joining(" "));
            if (line.contains("S.No.") && line.contains("Test")
                    && line.contains("Result") && line.contains("Specification")) {
                return i;
            }
        }
        return -1;
    }

    private static void parseFirstTableDeterministic(List<String> lines, LinkedHashMap<String,String> out) {
        for (String line : lines) {
            if (line.startsWith("Product ") && line.endsWith(" Name")) {
                out.put("Product Name", line.substring("Product ".length(), line.length() - " Name".length()).trim());
            }
            if (line.startsWith("Product ") && line.contains("B.No.") && line.contains("AR No.")) {
                String afterProduct = line.substring("Product ".length()).trim();
                String code = before(afterProduct, "B.No.").trim();
                out.put("Product Code", code);
                String afterBnoLabel = afterProduct.substring(afterProduct.indexOf("B.No.") + "B.No.".length()).trim();
                String bno = before(afterBnoLabel, "AR No.").trim();
                out.put("B.No.", bno);
                String arNo = afterBnoLabel.substring(afterBnoLabel.indexOf("AR No.") + "AR No.".length()).trim();
                out.put("AR No.", arNo);
            }
            if (line.startsWith("Specification ") && line.contains("Batch Size") && line.contains("Protocol ID")) {
                String afterSpec = line.substring("Specification ".length()).trim();
                String specId = before(afterSpec, "Batch Size").trim();
                out.put("Specification ID", specId);
                String afterBatchLabel = afterSpec.substring(afterSpec.indexOf("Batch Size") + "Batch Size".length()).trim();
                String batchSize = before(afterBatchLabel, "Protocol ID").trim();
                out.put("Batch Size", batchSize);
                String protocolId = afterBatchLabel.substring(afterBatchLabel.indexOf("Protocol ID") + "Protocol ID".length()).trim();
                out.put("Protocol ID", protocolId);
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
            if (line.startsWith("Storage ")) {
                String afterStorage = line.substring("Storage ".length()).trim();
                String cond = before(afterStorage, "Schedule").trim();
                out.put("Storage Condition", cond);
                String afterFirstScheduleLabel = afterStorage.substring(afterStorage.indexOf("Schedule") + "Schedule".length()).trim();
                String period = before(afterFirstScheduleLabel, "Schedule").trim();
                out.put("Schedule period", period);
                String scheduleDate = afterFirstScheduleLabel.substring(afterFirstScheduleLabel.indexOf("Schedule") + "Schedule".length()).trim();
                out.put("Schedule Date", scheduleDate);
            }
            if (line.startsWith("Sample ")) {
                String afterSample = line.substring("Sample ".length()).trim();
                String orientation = before(afterSample, "Packing").trim();
                out.put("Sample orientation", orientation);

                String afterPackingLabel = afterSample.substring(afterSample.indexOf("Packing") + "Packing".length()).trim();
                String packType = before(afterPackingLabel, "Pack Size").trim();
                out.put("Packing Type", packType);

                String packSize = afterPackingLabel.substring(afterPackingLabel.indexOf("Pack Size") + "Pack Size".length()).trim();
                out.put("Pack Size", packSize);
            }
        }
    }

    // -----------------------------
    // Utilities
    // -----------------------------
    private static String before(String text, String marker) {
        int idx = text.indexOf(marker);
        if (idx < 0) return text;
        return text.substring(0, idx);
    }

    private static String normalize(String text) {
        if (text == null) return "";
        String t = text.trim();
        t = t.replaceAll("[:\\-]+$", "");
        t = t.replaceAll("\\s+", " ");
        t = t.replaceAll("\\(\\s+", "(").replaceAll("\\s+\\)", ")");
        return t;
    }

    private static String stripFooterFragments(String text) {
        if (text == null) return "";
        String t = text;

        t = t.replaceAll("\\s*Remarks:.*$", "");
        t = t.replaceAll("\\s*Checked by.*$", "");
        t = t.replaceAll("\\s*Approved by.*$", "");
        t = t.replaceAll("\\s*Date:.*$", "");

        t = t.replaceAll("(?i)\\s*[A-Za-z\\s]+(Pharma|Pharmaceuticals?)\\s+(Limited|Ltd\\.?|Pvt\\.?).*$", "");
        t = t.replaceAll("(?i)\\s*[A-Za-z\\s]+,\\s*Plot\\s+(no\\.?|Nos?\\.?).*$", "");
        t = t.replaceAll("(?i)\\s*[A-Za-z\\s]+\\s*-\\s*\\d{5,6}\\s*$", "");
        t = t.replaceAll("(?i)\\s*[A-Za-z\\s]+,\\s*[A-Za-z\\s]+\\s*-\\s*\\d{5,6}.*$", "");

        return t.trim();
    }

    private static boolean isFooterLine(String line) {
        String l = line.trim().toLowerCase();

        if (l.isEmpty()) return false;

        if (l.startsWith("remarks") || l.contains("checked by") ||
                l.contains("approved by") || l.matches(".*\\bdate:.*")) {
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

    private static String append(String base, String add) {
        String a = (add == null) ? "" : add.trim();
        if (a.isEmpty()) return base == null ? "" : base;
        if (base == null || base.isEmpty()) return a;
        return base + " " + a;
    }

    private static void flushSecondTableRow(LinkedHashMap<String, PDFRecordCOAReport> out,
                                            String sNo, String test, String result, String spec) {
        String s = normalize(sNo);
        String t = stripFooterFragments(normalize(test));
        String r = stripFooterFragments(normalize(result));
        String p = stripFooterFragments(normalize(spec));

        PDFRecordCOAReport rec = new PDFRecordCOAReport(s, t, r, p);

        String key = rec.sNo + " - " + rec.test;
        int counter = 1;
        String uniqueKey = key;
        while (out.containsKey(uniqueKey)) uniqueKey = key + " #" + (++counter);
        out.put(uniqueKey, rec);
    }

    // -----------------------------
    // Second table parsing
    // -----------------------------
    private static void parseSecondTableRows(List<Row> rows, LinkedHashMap<String, PDFRecordCOAReport> out) {
        float xSno = 0f, xTest = 120f, xResult = 320f, xSpec = 450f;

        for (Row r : rows) {
            String joined = r.cells.stream().map(c -> c.text).collect(Collectors.joining(" "));
            if (joined.contains("S.No.") && joined.contains("Test") && joined.contains("Result") && joined.contains("Specification")) {
                List<Cell> hc = new ArrayList<>(r.cells);
                hc.sort(Comparator.comparing(c -> c.x));
                for (Cell c : hc) {
                    String t = c.text.toLowerCase();
                    String normalized = t.replaceAll("[\\s.]+", "");
                    if (normalized.contains("sno") || t.matches(".*s\\.?\\s*no\\.?.*")) xSno = c.x;
                    else if (t.contains("test")) xTest = c.x;
                    else if (t.contains("result")) xResult = c.x;
                    else if (t.contains("spec")) xSpec = c.x;
                }
                break;
            }
        }
        float cut1 = (xSno + xTest) / 2f;
        float cut2 = (xTest + xResult) / 2f;
        float cut3 = (xResult + xSpec) / 2f;

        String sNoBuf = "", testBuf = "", resultBuf = "", specBuf = "";
        boolean inRow = false;

        final java.util.function.Function<String, String> sanitizeSno = (s) -> {
            if (s == null) return "";
            String[] parts = s.trim().split("\\s+");
            for (String p : parts) {
                if (p.matches("^(\\d+)(?:\\.\\d+)?$")) return p;
            }
            return "";
        };
        final java.util.function.Function<String, String> trailingFromSnoIntoTest = (s) -> {
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
        };

        for (Row r : rows) {
            String joinedLine = r.cells.stream().map(c -> c.text.trim()).collect(Collectors.joining(" "));
            if (joinedLine.isEmpty() || isFooterLine(joinedLine)) continue;
            if (joinedLine.contains("S.No.") && joinedLine.contains("Test") &&
                    joinedLine.contains("Result") && joinedLine.contains("Specification")) {
                continue; // skip header anywhere
            }

            List<String> snoTokens = new ArrayList<>(), testTokens = new ArrayList<>(),
                    resultTokens = new ArrayList<>(), specTokens = new ArrayList<>();
            for (Cell c : r.cells) {
                String t = c.text.trim();
                if (t.isEmpty()) continue;
                if (c.x <= cut1) snoTokens.add(t);
                else if (c.x <= cut2) testTokens.add(t);
                else if (c.x <= cut3) resultTokens.add(t);
                else specTokens.add(t);
            }

            String rawSnoStr = String.join(" ", snoTokens).trim();
            String snoStr = sanitizeSno.apply(rawSnoStr);
            String testStr = String.join(" ", testTokens).trim();
            String resultStr = String.join(" ", resultTokens).trim();
            String specStr = String.join(" ", specTokens).trim();

            String trailingFromSno = trailingFromSnoIntoTest.apply(rawSnoStr);
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

                if (isParent && !hasValues && !KEEP_PARENT_HEADERS) {
                    sNoBuf = testBuf = resultBuf = specBuf = "";
                    inRow = false;
                }
            } else {
                if (!testStr.isEmpty()) testBuf = append(testBuf, testStr);
                if (!resultStr.isEmpty()) resultBuf = append(resultBuf, resultStr);
                if (!specStr.isEmpty()) specBuf = append(specBuf, specStr);
            }
        }

        if (inRow && !sNoBuf.isEmpty()) {
            boolean parentOnly = sNoBuf.matches("^\\d+$") && resultBuf.isEmpty() && specBuf.isEmpty();
            if (!parentOnly || KEEP_PARENT_HEADERS) {
                flushSecondTableRow(out, sNoBuf, testBuf, resultBuf, specBuf);
            }
        }
    }

    // -----------------------------
    // Main
    // -----------------------------
    public static void main(String[] args) {
        try {
            File pdf = new File("D:\\reoosinv\\acetominophen - 3 month.pdf");
            Result res = extract(pdf);

            System.out.println("---- First Table ----");
            res.firstTable.forEach((k, v) -> System.out.println(k + " = " + v));

            System.out.println("\n---- Second Table (all pages combined) ----");
            res.secondTable.forEach((k, v) -> System.out.println(k + " => " + v));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
