package com.ciphervault.ciphervault.file;

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

    private static final Logger log = LoggerFactory.getLogger(MetadataBackfillService.class);

    private final FileRepository fileRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final MetadataExtractionService metadataExtractionService;
    private final EncryptionService encryptionService;
    private final KeyManagementService keyManagementService;
    private final TransactionTemplate transactionTemplate;

    public MetadataBackfillService(FileRepository fileRepository,
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
        // 1. Normalize legacy absolute storage and preview paths in the database
        normalizeLegacyPaths();

        log.debug("Checking for files requiring metadata backfill...");
        List<Long> pendingFileIds = transactionTemplate.execute(status -> {
            try {
                return fileRepository.findByMetadataIsNull().stream()
                        .map(StoredFile::getId)
                        .toList();
            } catch (Exception e) {
                log.warn("Could not query files for metadata backfill: {}", e.getMessage());
                return Collections.emptyList();
            }
        });

        long totalFiles = 0;
        try {
            totalFiles = fileRepository.count();
        } catch (Exception ignored) {}

        if (pendingFileIds == null || pendingFileIds.isEmpty()) {
            log.debug("Metadata backfill check complete: all files have metadata.");
            com.ciphervault.ciphervault.logging.ConsoleLogger.logMetadataBackfillSummary((int) totalFiles, 0, 0);
            return 0;
        }

        log.debug("Starting metadata backfill for {} file(s)...", pendingFileIds.size());
        int backfilledCount = 0;

        for (Long fileId : pendingFileIds) {
            boolean success = backfillSingleFile(fileId);
            if (success) {
                backfilledCount++;
            }
        }

        log.debug("Metadata backfill completed. Successfully processed {} file(s).", backfilledCount);
        com.ciphervault.ciphervault.logging.ConsoleLogger.logMetadataBackfillSummary((int) totalFiles, pendingFileIds.size(), backfilledCount);
        return backfilledCount;
    }

    private void normalizeLegacyPaths() {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                List<StoredFile> allFiles = fileRepository.findAll();
                boolean changed = false;
                for (StoredFile file : allFiles) {
                    boolean fileChanged = false;
                    String sp = file.getStoragePath();
                    if (sp != null) {
                        String norm = sp.replace('\\', '/');
                        int encIdx = norm.indexOf("encrypted/");
                        int uplIdx = norm.indexOf("uploads/");
                        if (encIdx != -1 && !sp.equals(norm.substring(encIdx))) {
                            file.setStoragePath(norm.substring(encIdx));
                            fileChanged = true;
                        } else if (uplIdx != -1 && !sp.equals(norm.substring(uplIdx))) {
                            file.setStoragePath(norm.substring(uplIdx));
                            fileChanged = true;
                        }
                    }

                    String pp = file.getPreviewPath();
                    if (pp != null) {
                        String normP = pp.replace('\\', '/');
                        int prevIdx = normP.indexOf("previews/");
                        if (prevIdx != -1 && !pp.equals(normP.substring(prevIdx))) {
                            file.setPreviewPath(normP.substring(prevIdx));
                            fileChanged = true;
                        }
                    }

                    if (fileChanged) {
                        fileRepository.save(file);
                        changed = true;
                    }
                }
                if (changed) {
                    log.debug("Normalized legacy file storage and preview paths to relative paths in database.");
                }
            });
        } catch (Exception e) {
            log.warn("Legacy path normalization warning: {}", e.getMessage());
        }
    }

    private boolean backfillSingleFile(Long fileId) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            StoredFile file = fileRepository.findById(fileId).orElse(null);
            if (file == null) {
                return false;
            }

            Path tempDecrypted = null;
            try {
                Path storagePath = FileStorageConfig.resolvePath(file.getStoragePath());
                if (storagePath == null || !Files.exists(storagePath)) {
                    log.warn("Storage file missing for ID: {} ({}). Marking as processed.", file.getId(), file.getOriginalFilename());
                    FileMetadata emptyMeta = new FileMetadata();
                    emptyMeta.setFile(file);
                    fileMetadataRepository.save(emptyMeta);
                    return true;
                }

                FileMetadata metadata;
                if (file.isEncrypted()) {
                    SecretKey userKey = keyManagementService.getOrGenerateUserKey(file.getUser());
                    tempDecrypted = Files.createTempFile("cv_backfill_", ".tmp");
                    try (InputStream is = Files.newInputStream(storagePath);
                         OutputStream os = Files.newOutputStream(tempDecrypted)) {
                        encryptionService.decryptStream(is, os, userKey);
                    }
                    metadata = metadataExtractionService.extractMetadata(tempDecrypted, file.getOriginalFilename(), file.getContentType());
                } else {
                    metadata = metadataExtractionService.extractMetadata(storagePath, file.getOriginalFilename(), file.getContentType());
                }

                if (metadata == null) {
                    metadata = new FileMetadata();
                }

                metadata.setFile(file);
                fileMetadataRepository.save(metadata);
                log.debug("Backfilled metadata for file ID: {} ({})", file.getId(), file.getOriginalFilename());
                return true;

            } catch (Exception e) {
                log.warn("Failed backfilling metadata for file ID: {} - {}", file.getId(), e.getMessage());
                status.setRollbackOnly();
                return false;
            } finally {
                if (tempDecrypted != null) {
                    try {
                        Files.deleteIfExists(tempDecrypted);
                    } catch (Exception ignored) {}
                }
            }
        }));
    }
}
