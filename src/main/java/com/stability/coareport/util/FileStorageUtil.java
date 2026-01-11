package com.stability.coareport.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileStorageUtil {

    private static final String UPLOAD_BASE_DIR = "uploads";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public static String generateFilePath(String productName, String batchNo,
                                          String storageCondition, String originalFilename) {
        String sanitizedProduct = sanitizeFolderName(productName);
        String sanitizedBatch = sanitizeFolderName(batchNo);
        String sanitizedStorage = sanitizeFolderName(storageCondition);
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

        String fileName = timestamp + "_" + sanitizeFileName(originalFilename);

        return Paths.get(UPLOAD_BASE_DIR, sanitizedProduct, sanitizedBatch,
                        sanitizedStorage, fileName).toString();
    }

    public static void ensureDirectoryExists(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Path directory = path.getParent();
        if (directory != null && !Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }

    private static String sanitizeFolderName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "unknown";
        }
        return name.replaceAll("[^a-zA-Z0-9._-]", "_")
                   .replaceAll("_+", "_")
                   .trim();
    }

    private static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "file.pdf";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_")
                       .replaceAll("_+", "_")
                       .trim();
    }

    public static String getRelativePath(String fullPath) {
        if (fullPath.startsWith(UPLOAD_BASE_DIR + "/") || fullPath.startsWith(UPLOAD_BASE_DIR + "\\")) {
            return fullPath.substring(UPLOAD_BASE_DIR.length() + 1);
        }
        return fullPath;
    }
}
