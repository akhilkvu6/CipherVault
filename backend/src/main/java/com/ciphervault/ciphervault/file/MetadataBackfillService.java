package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.logging.ConsoleLogger;
import com.ciphervault.ciphervault.security.KeyManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.crypto.SecretKey;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Service
public class MetadataBackfillService {

    private static final Logger log =
            LoggerFactory.getLogger(MetadataBackfillService.class);

    private final FileRepository fileRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final MetadataExtractionService metadataExtractionService;
    private final EncryptionService encryptionService;
    private final KeyManagementService keyManagementService;
    private final TransactionTemplate transactionTemplate;

    public MetadataBackfillService(
            FileRepository fileRepository,
            FileMetadataRepository fileMetadataRepository,
            MetadataExtractionService metadataExtractionService,
            EncryptionService encryptionService,
            KeyManagementService keyManagementService,
            TransactionTemplate transactionTemplate) {

        this.fileRepository = fileRepository;
        this.fileMetadataRepository = fileMetadataRepository;
        this.metadataExtractionService = metadataExtractionService;
        this.encryptionService = encryptionService;
        this.keyManagementService = keyManagementService;
        this.transactionTemplate = transactionTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        runBackfill();
    }

    public synchronized int runBackfill() {
        // Normalize legacy absolute storage and preview paths before backfill.
        normalizeLegacyPaths();

        log.debug("Checking for files requiring metadata backfill...");

        List<Long> pendingFileIds = transactionTemplate.execute(status -> {
            try {
                return fileRepository.findByMetadataIsNull()
                        .stream()
                        .map(StoredFile::getId)
                        .toList();

            } catch (Exception e) {
                log.warn(
                        "Could not query files for metadata backfill: {}",
                        e.getMessage()
                );

                return Collections.emptyList();
            }
        });

        long totalFiles = getTotalFileCount();

        if (pendingFileIds == null || pendingFileIds.isEmpty()) {
            log.debug(
                    "Metadata backfill check complete: all files have metadata."
            );

            ConsoleLogger.logMetadataBackfillSummary(
                    (int) totalFiles,
                    0,
                    0
            );

            return 0;
        }

        log.debug(
                "Starting metadata backfill for {} file(s)...",
                pendingFileIds.size()
        );

        int backfilledCount = 0;

        for (Long fileId : pendingFileIds) {
            if (backfillSingleFile(fileId)) {
                backfilledCount++;
            }
        }

        log.debug(
                "Metadata backfill completed. Successfully processed {} file(s).",
                backfilledCount
        );

        ConsoleLogger.logMetadataBackfillSummary(
                (int) totalFiles,
                pendingFileIds.size(),
                backfilledCount
        );

        return backfilledCount;
    }

    private long getTotalFileCount() {
        try {
            return fileRepository.count();

        } catch (Exception e) {
            log.warn(
                    "Could not determine total file count: {}",
                    e.getMessage()
            );

            return 0;
        }
    }

    private void normalizeLegacyPaths() {
        try {
            int batchSize = 500;
            long totalFiles = getTotalFileCount();
            long totalPages = (totalFiles + batchSize - 1) / batchSize;
            boolean anyChanged = false;

            for (int page = 0; page < totalPages; page++) {
                final int currentPage = page;
                boolean changed = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
                    org.springframework.data.domain.Page<StoredFile> filePage = 
                            fileRepository.findAll(org.springframework.data.domain.PageRequest.of(currentPage, batchSize));
                    
                    boolean pageChanged = false;
                    for (StoredFile file : filePage.getContent()) {
                        if (normalizeStoragePath(file) || normalizePreviewPath(file)) {
                            pageChanged = true;
                        }
                    }
                    return pageChanged;
                }));
                
                if (changed) {
                    anyChanged = true;
                }
            }

            if (anyChanged) {
                log.debug("Normalized legacy file storage and preview paths to relative paths in database.");
            }

        } catch (Exception e) {
            log.warn("Legacy path normalization warning: {}", e.getMessage());
        }
    }

    private boolean normalizeStoragePath(StoredFile file) {
        String storagePath = file.getStoragePath();

        if (storagePath == null || storagePath.isBlank()) {
            return false;
        }

        String normalizedPath =
                storagePath.replace('\\', '/');

        int encryptedIndex =
                normalizedPath.indexOf("encrypted/");

        int uploadsIndex =
                normalizedPath.indexOf("uploads/");

        if (encryptedIndex != -1) {
            return updateStoragePath(
                    file,
                    storagePath,
                    normalizedPath.substring(encryptedIndex)
            );
        }

        if (uploadsIndex != -1) {
            return updateStoragePath(
                    file,
                    storagePath,
                    normalizedPath.substring(uploadsIndex)
            );
        }

        return false;
    }

    private boolean updateStoragePath(
            StoredFile file,
            String currentPath,
            String normalizedPath) {

        if (currentPath.equals(normalizedPath)) {
            return false;
        }

        file.setStoragePath(normalizedPath);
        return true;
    }

    private boolean normalizePreviewPath(StoredFile file) {
        String previewPath = file.getPreviewPath();

        if (previewPath == null || previewPath.isBlank()) {
            return false;
        }

        String normalizedPath =
                previewPath.replace('\\', '/');

        int previewsIndex =
                normalizedPath.indexOf("previews/");

        if (previewsIndex == -1) {
            return false;
        }

        String relativePath =
                normalizedPath.substring(previewsIndex);

        if (previewPath.equals(relativePath)) {
            return false;
        }

        file.setPreviewPath(relativePath);
        return true;
    }

    private boolean backfillSingleFile(Long fileId) {
        return Boolean.TRUE.equals(
                transactionTemplate.execute(status -> {
                    StoredFile file =
                            fileRepository.findById(fileId).orElse(null);

                    if (file == null) {
                        return false;
                    }

                    Path tempDecrypted = null;

                    try {
                        Path storagePath =
                                FileStorageConfig.resolvePath(
                                        file.getStoragePath()
                                );

                        if (storagePath == null
                                || !Files.isRegularFile(storagePath)) {

                            log.warn(
                                    "Storage file missing for ID: {} ({}). Marking as processed.",
                                    file.getId(),
                                    file.getOriginalFilename()
                            );

                            saveEmptyMetadata(file);
                            return true;
                        }

                        FileMetadata metadata;

                        if (file.isEncrypted()) {
                            SecretKey userKey =
                                    keyManagementService.getOrGenerateUserKey(
                                            file.getUser()
                                    );

                            tempDecrypted =
                                    Files.createTempFile(
                                            "cv_backfill_",
                                            ".tmp"
                                    );

                            try (
                                    InputStream input =
                                            Files.newInputStream(storagePath);

                                    OutputStream output =
                                            Files.newOutputStream(tempDecrypted)
                            ) {
                                encryptionService.decryptStream(
                                        input,
                                        output,
                                        userKey
                                );
                            }

                            metadata =
                                    metadataExtractionService.extractMetadata(
                                            tempDecrypted,
                                            file.getOriginalFilename(),
                                            file.getContentType()
                                    );

                        } else {
                            metadata =
                                    metadataExtractionService.extractMetadata(
                                            storagePath,
                                            file.getOriginalFilename(),
                                            file.getContentType()
                                    );
                        }

                        if (metadata == null) {
                            metadata = new FileMetadata();
                        }

                        metadata.setFile(file);
                        fileMetadataRepository.save(metadata);

                        log.debug(
                                "Backfilled metadata for file ID: {} ({})",
                                file.getId(),
                                file.getOriginalFilename()
                        );

                        return true;

                    } catch (Exception e) {
                        log.warn(
                                "Failed backfilling metadata for file ID: {} - {}",
                                file.getId(),
                                e.getMessage()
                        );

                        status.setRollbackOnly();
                        return false;

                    } finally {
                        deleteTemporaryFile(tempDecrypted);
                    }
                })
        );
    }

    private void saveEmptyMetadata(StoredFile file) {
        FileMetadata metadata = new FileMetadata();
        metadata.setFile(file);
        fileMetadataRepository.save(metadata);
    }

    private void deleteTemporaryFile(Path path) {
        if (path == null) {
            return;
        }

        try {
            Files.deleteIfExists(path);

        } catch (Exception e) {
            log.debug(
                    "Could not delete temporary backfill file: {}",
                    path
            );
        }
    }
}