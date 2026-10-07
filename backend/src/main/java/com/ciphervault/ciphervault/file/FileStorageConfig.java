package com.ciphervault.ciphervault.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

@Configuration
public class FileStorageConfig {

    private static final Logger log =
            LoggerFactory.getLogger(FileStorageConfig.class);

    public static final Path STORAGE_ROOT = Paths.get("storage");
    public static final Path ENCRYPTED_STORAGE =
            STORAGE_ROOT.resolve("encrypted");
    public static final Path NORMAL_STORAGE =
            STORAGE_ROOT.resolve("uploads");
    public static final Path PREVIEW_STORAGE =
            STORAGE_ROOT.resolve("previews");
    public static final Path PROFILE_PHOTO_STORAGE =
            STORAGE_ROOT.resolve("profile_photos");

    public FileStorageConfig() {
        log.debug("Initializing file storage configuration...");

        try {
            Files.createDirectories(ENCRYPTED_STORAGE);
            Files.createDirectories(NORMAL_STORAGE);
            Files.createDirectories(PREVIEW_STORAGE);
            Files.createDirectories(PROFILE_PHOTO_STORAGE);

            log.debug("File storage directories initialized successfully.");
        } catch (Exception e) {
            log.error(
                    "Failed to initialize file storage directories: {}",
                    e.getMessage(),
                    e
            );
            throw new RuntimeException(
                    "Could not initialize file storage directories.",
                    e
            );
        }
    }

    /**
     * Resolves a stored file or preview path safely, supporting:
     * 1. Relative paths relative to STORAGE_ROOT
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

        String normalizedInput =
                pathStr.trim().replace('\\', '/');

        String lowerInput =
                normalizedInput.toLowerCase(Locale.ROOT);

        // Check if path contains a "storage/" directory segment.
        int storageIndex = findStorageSegment(lowerInput);

        if (storageIndex >= 0) {
            String relativeToRoot =
                    normalizedInput.substring(
                            storageIndex + "storage/".length()
                    );

            Path resolved =
                    root.resolve(relativeToRoot).normalize();

            return validatePath(resolved, root);
        }

        Path candidate =
                Paths.get(pathStr).toAbsolutePath().normalize();

        // Accept legacy absolute paths only when they remain inside STORAGE_ROOT.
        if (Paths.get(pathStr).isAbsolute()) {
            return validatePath(candidate, root);
        }

        if (normalizedInput.startsWith("storage/")) {
            normalizedInput =
                    normalizedInput.substring("storage/".length());
        }

        Path resolved =
                root.resolve(normalizedInput).normalize();

        return validatePath(resolved, root);
    }

    private static int findStorageSegment(String path) {
        if (path.startsWith("storage/")) {
            return 0;
        }

        int index = path.indexOf("/storage/");
        return index >= 0 ? index + 1 : -1;
    }

    private static Path validatePath(Path path, Path root) {
        if (!path.startsWith(root)) {
            throw new SecurityException(
                    "Potential path traversal attempt: " + path
            );
        }

        if (!Files.exists(path)) {
            return path;
        }

        try {
            Path realRoot = root.toRealPath();
            Path realPath = path.toRealPath();

            if (!realPath.startsWith(realRoot)) {
                throw new SecurityException(
                        "Resolved path is outside storage root: " + path
                );
            }

            return realPath;
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to validate storage path: " + path,
                    e
            );
        }
    }
}