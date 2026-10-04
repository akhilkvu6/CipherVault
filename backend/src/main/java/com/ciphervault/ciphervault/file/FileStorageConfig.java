package com.ciphervault.ciphervault.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileStorageConfig {

    private static final Logger log = LoggerFactory.getLogger(FileStorageConfig.class);

    public static final Path STORAGE_ROOT = Paths.get("storage");
    public static final Path ENCRYPTED_STORAGE = STORAGE_ROOT.resolve("encrypted");
    public static final Path NORMAL_STORAGE = STORAGE_ROOT.resolve("uploads");
    public static final Path PREVIEW_STORAGE = STORAGE_ROOT.resolve("previews");

    public FileStorageConfig() {
        log.debug("Initializing file storage configuration...");

        try {
            Files.createDirectories(ENCRYPTED_STORAGE);
            Files.createDirectories(NORMAL_STORAGE);
            Files.createDirectories(PREVIEW_STORAGE);
            log.debug("File storage directories initialized successfully.");
        } catch (Exception e) {
            log.error("Failed to initialize file storage directories: {}", e.getMessage(), e);
            throw new RuntimeException("Could not initialize file storage directories.", e);
        }
    }

    /**
     * Resolves a stored file or preview path safely, supporting:
     * 1. Relative paths relative to STORAGE_ROOT (e.g. "encrypted/abc.mp4", "previews/preview-abc.jpg")
     * 2. Legacy absolute paths stored in the database on Windows/Linux
     * 3. Cross-platform path separators (\ and /)
     *
     * Enforces path containment to prevent directory traversal outside STORAGE_ROOT.
     */
    public static Path resolvePath(String pathStr) {
        if (pathStr == null || pathStr.isBlank()) {
            return null;
        }

        Path root = STORAGE_ROOT.toAbsolutePath().normalize();
        String normalizedInput = pathStr.replace('\\', '/').trim();

        // Check if path contains "storage/" prefix (common in relative or absolute paths)
        int storageIdx = normalizedInput.indexOf("storage/");
        if (storageIdx != -1) {
            String relativeToRoot = normalizedInput.substring(storageIdx + "storage/".length());
            Path resolved = root.resolve(relativeToRoot).normalize();
            if (resolved.startsWith(root) && Files.exists(resolved)) {
                return resolved;
            }
        }

        // Direct candidate (e.g. legacy absolute path that exists on disk)
        Path candidate = Paths.get(pathStr);
        if (candidate.isAbsolute()) {
            Path norm = candidate.normalize();
            if (Files.exists(norm)) {
                return norm;
            }
        }

        // If path begins with "storage/", strip it to prevent duplicate nesting
        if (normalizedInput.startsWith("storage/")) {
            normalizedInput = normalizedInput.substring("storage/".length());
        }

        Path resolved = root.resolve(normalizedInput).normalize();
        if (resolved.startsWith(root)) {
            return resolved;
        }

        // Fallback for absolute path that is inside root
        if (candidate.isAbsolute() && candidate.normalize().startsWith(root)) {
            return candidate.normalize();
        }

        throw new SecurityException("Potential path traversal attempt: " + pathStr);
    }
}