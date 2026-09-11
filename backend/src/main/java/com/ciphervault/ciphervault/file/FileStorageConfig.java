package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.util.ConsoleLogger;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileStorageConfig {

    public static final Path STORAGE_ROOT =
            Paths.get("storage");

    public static final Path ENCRYPTED_STORAGE =
            STORAGE_ROOT.resolve("encrypted");

    public static final Path NORMAL_STORAGE =
            STORAGE_ROOT.resolve("uploads");

    public FileStorageConfig() {

        ConsoleLogger.info("Initializing file storage configuration...");

        try {
            Files.createDirectories(ENCRYPTED_STORAGE);
            Files.createDirectories(NORMAL_STORAGE);

            ConsoleLogger.success(
                    "File storage directories initialized successfully."
            );

        } catch (Exception e) {

            ConsoleLogger.error(
                    "Failed to initialize file storage directories: "
                            + e.getMessage()
            );

            throw new RuntimeException(
                    "Could not initialize file storage directories.",
                    e
            );
        }
    }
}