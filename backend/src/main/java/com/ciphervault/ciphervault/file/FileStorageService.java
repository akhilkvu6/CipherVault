package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.logging.ConsoleLogger;
import com.ciphervault.ciphervault.logging.RequestContext;
import com.ciphervault.ciphervault.security.KeyManagementService;
import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.crypto.SecretKey;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HexFormat;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log =
            LoggerFactory.getLogger(FileStorageService.class);

    private static final int BUFFER_SIZE = 16 * 1024;

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final MediaPreviewService mediaPreviewService;
    private final KeyManagementService keyManagementService;
    private final MetadataExtractionService metadataExtractionService;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileCategoryService fileCategoryService;

    public FileStorageService(
            FileRepository fileRepository,
            UserRepository userRepository,
            EncryptionService encryptionService,
            MediaPreviewService mediaPreviewService,
            KeyManagementService keyManagementService,
            MetadataExtractionService metadataExtractionService,
            FileMetadataRepository fileMetadataRepository,
            FileCategoryService fileCategoryService) {

        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
        this.mediaPreviewService = mediaPreviewService;
        this.keyManagementService = keyManagementService;
        this.metadataExtractionService = metadataExtractionService;
        this.fileMetadataRepository = fileMetadataRepository;
        this.fileCategoryService = fileCategoryService;
    }

    /**
     * Store and optionally encrypt an uploaded file.
     */
    public StoredFile storeFile(
            User user,
            MultipartFile file) throws Exception {

        boolean encrypt = true;

        long uploadStart = System.currentTimeMillis();
        ConsoleLogger.UploadTimingTracker timings =
                new ConsoleLogger.UploadTimingTracker();

        String reqId = RequestContext.getRequestId();
        String failedStage = "VALIDATION";

        Path tempSourceFile = null;
        Path storagePath = null;
        Path previewPath = null;

        long fileSize = 0L;
        boolean quotaIncremented = false;

        long quotaBefore = user.getUsedStorage() != null
                ? user.getUsedStorage()
                : 0L;

        String originalFilename = null;

        try {
            long validationStart = System.currentTimeMillis();

            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File cannot be empty");
            }

            originalFilename = file.getOriginalFilename();

            if (originalFilename == null || originalFilename.isBlank()) {
                originalFilename = "file";
            }

            String extension = getExtension(originalFilename);

            // Stream the multipart upload to a temporary file before processing.
            tempSourceFile = Files.createTempFile(
                    "cv_upload_src_",
                    extension
            );

            try (InputStream input = file.getInputStream();
                 OutputStream output = Files.newOutputStream(tempSourceFile)) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }

                output.flush();
            }

            fileSize = Files.size(tempSourceFile);

            long usedStorage = user.getUsedStorage() != null
                    ? user.getUsedStorage()
                    : 0L;

            if (usedStorage + fileSize > user.getStorageLimit()) {
                log.warn(
                        "Upload rejected: storage quota exceeded for user: {}",
                        user.getEmail()
                );

                throw new StorageQuotaExceededException(
                        "Storage quota exceeded"
                );
            }

            String storedFilename =
                    UUID.randomUUID() + extension;

            Path storageDirectory = encrypt
                    ? FileStorageConfig.ENCRYPTED_STORAGE
                    : FileStorageConfig.NORMAL_STORAGE;

            Path storageRoot =
                    FileStorageConfig.STORAGE_ROOT
                            .toAbsolutePath()
                            .normalize();

            Path absoluteStorageDirectory =
                    storageDirectory
                            .toAbsolutePath()
                            .normalize();

            storagePath = absoluteStorageDirectory
                    .resolve(storedFilename)
                    .normalize();

            if (!storagePath.startsWith(storageRoot)) {
                log.error(
                        "Invalid storage path detected: {}",
                        storagePath
                );

                throw new SecurityException(
                        "Invalid storage path"
                );
            }

            timings.tValidation =
                    Math.max(
                            1,
                            System.currentTimeMillis() - validationStart
                    );

            ConsoleLogger.stage(
                    reqId,
                    ConsoleLogger.TAG_FILE,
                    "Upload validation passed ("
                            + timings.tValidation
                            + " ms)"
            );

            // Stage 2: Calculate SHA-256 before encryption.
            failedStage = "SHA-256";

            long shaStart = System.currentTimeMillis();

            String sha256Hash =
                    calculateSha256(tempSourceFile);

            timings.tSha256 =
                    Math.max(
                            1,
                            System.currentTimeMillis() - shaStart
                    );

            ConsoleLogger.stage(
                    reqId,
                    ConsoleLogger.TAG_HASH,
                    "SHA-256 calculated: "
                            + ConsoleLogger.truncateSha(sha256Hash)
                            + " ("
                            + timings.tSha256
                            + " ms)"
            );

            // Stage 3: Reject duplicate files belonging to the same user.
            failedStage = "DUPLICATE CHECK";

            long duplicateStart =
                    System.currentTimeMillis();

            Optional<StoredFile> existingDuplicate =
                    fileRepository.findByUserAndSha256Hash(
                            user,
                            sha256Hash
                    );

            timings.tDuplicateCheck =
                    Math.max(
                            1,
                            System.currentTimeMillis()
                                    - duplicateStart
                    );

            if (existingDuplicate.isPresent()) {
                ConsoleLogger.logDuplicateDetectedTrace(
                        reqId,
                        originalFilename,
                        fileSize,
                        sha256Hash,
                        user.getId(),
                        existingDuplicate.get().getId(),
                        existingDuplicate.get().getOriginalFilename()
                );

                throw new DuplicateFileException(
                        "Duplicate file already exists",
                        sha256Hash
                );
            }

            ConsoleLogger.stage(
                    reqId,
                    ConsoleLogger.TAG_DB,
                    "Duplicate check complete: UNIQUE ("
                            + timings.tDuplicateCheck
                            + " ms)"
            );

            // Stage 4: Generate preview before encryption.
            failedStage = "PREVIEW";

            long previewStart =
                    System.currentTimeMillis();

            byte[] previewData = null;

            try {
                if (mediaPreviewService != null) {
                    previewData =
                            mediaPreviewService.generatePreview(
                                    tempSourceFile,
                                    originalFilename,
                                    file.getContentType()
                            );
                }
            } catch (Exception e) {
                log.warn(
                        "Preview generation error (continuing upload): {}",
                        e.getMessage()
                );
            }

            String previewFilename = null;

            if (previewData != null && previewData.length > 0) {
                previewFilename =
                        "preview-" + UUID.randomUUID() + ".jpg";

                storageRoot =
                        FileStorageConfig.STORAGE_ROOT
                                .toAbsolutePath()
                                .normalize();

                Path candidatePreviewPath =
                        FileStorageConfig.PREVIEW_STORAGE
                                .toAbsolutePath()
                                .normalize()
                                .resolve(previewFilename)
                                .normalize();

                if (candidatePreviewPath.startsWith(storageRoot)) {
                    Files.createDirectories(
                            FileStorageConfig.PREVIEW_STORAGE
                    );

                    Files.write(
                            candidatePreviewPath,
                            previewData
                    );

                    previewPath = candidatePreviewPath;
                } else {
                    previewFilename = null;
                }
            }

            timings.tPreview =
                    Math.max(
                            1,
                            System.currentTimeMillis()
                                    - previewStart
                    );

            ConsoleLogger.stage(
                    reqId,
                    ConsoleLogger.TAG_PREVIEW,
                    "Preview generated: "
                            + (previewPath != null ? "YES" : "NO")
                            + " ("
                            + timings.tPreview
                            + " ms)"
            );

            // Stage 5: Extract metadata before encryption.
            failedStage = "METADATA";

            long metadataStart =
                    System.currentTimeMillis();

            FileMetadata fileMetadata = null;

            if (metadataExtractionService != null) {
                try {
                    fileMetadata =
                            metadataExtractionService.extractMetadata(
                                    tempSourceFile,
                                    originalFilename,
                                    file.getContentType()
                            );
                } catch (Exception e) {
                    log.warn(
                            "Metadata extraction non-fatal error: {}",
                            e.getMessage()
                    );
                }
            }

            timings.tMetadata =
                    Math.max(
                            1,
                            System.currentTimeMillis()
                                    - metadataStart
                    );

            ConsoleLogger.stage(
                    reqId,
                    ConsoleLogger.TAG_META,
                    "ExifTool metadata extracted: "
                            + (fileMetadata != null ? "YES" : "NO")
                            + " ("
                            + timings.tMetadata
                            + " ms)"
            );

            // Stage 6: Encrypt the source file or copy it unchanged.
            failedStage = "ENCRYPTION";

            long encryptionStart =
                    System.currentTimeMillis();

            Files.createDirectories(
                    absoluteStorageDirectory
            );

            if (encrypt) {
                SecretKey userKey =
                        keyManagementService.getOrGenerateUserKey(user);

                try (InputStream input =
                             Files.newInputStream(tempSourceFile);
                     OutputStream output =
                             Files.newOutputStream(storagePath)) {

                    encryptionService.encryptStream(
                            input,
                            output,
                            userKey
                    );
                }
            } else {
                Files.copy(
                        tempSourceFile,
                        storagePath,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            timings.tEncryption =
                    Math.max(
                            1,
                            System.currentTimeMillis()
                                    - encryptionStart
                    );

            ConsoleLogger.stage(
                    reqId,
                    ConsoleLogger.TAG_CRYPTO,
                    encrypt
                            ? "AES-256-GCM streaming encryption complete ("
                            + timings.tEncryption
                            + " ms)"
                            : "Encryption disabled / bypassed ("
                            + timings.tEncryption
                            + " ms)"
            );

            // Stage 7: Confirm the storage operation completed.
            failedStage = "STORAGE";

            long storageStart =
                    System.currentTimeMillis();

            if (!Files.exists(storagePath)) {
                throw new IllegalStateException(
                        "Stored file was not created"
                );
            }

            timings.tStorage =
                    Math.max(
                            1,
                            System.currentTimeMillis()
                                    - storageStart
                    );

            ConsoleLogger.stage(
                    reqId,
                    ConsoleLogger.TAG_STORAGE,
                    encrypt
                            ? "Encrypted file persisted ("
                            + timings.tStorage
                            + " ms)"
                            : "Unencrypted file persisted ("
                            + timings.tStorage
                            + " ms)"
            );

            // Stage 8: Atomically reserve the user's storage quota.
            failedStage = "QUOTA";

            long quotaStart =
                    System.currentTimeMillis();

            int rowsUpdated =
                    userRepository.incrementStorageUsedAtomic(
                            user.getId(),
                            fileSize
                    );

            if (rowsUpdated == 0) {
                Files.deleteIfExists(storagePath);

                if (previewPath != null) {
                    Files.deleteIfExists(previewPath);
                }

                throw new StorageQuotaExceededException(
                        "Storage quota exceeded"
                );
            }

            quotaIncremented = true;

            user.setUsedStorage(
                    (user.getUsedStorage() != null
                            ? user.getUsedStorage()
                            : 0L)
                            + fileSize
            );

            timings.tQuota =
                    Math.max(
                            1,
                            System.currentTimeMillis()
                                    - quotaStart
                    );

            ConsoleLogger.stage(
                    reqId,
                    ConsoleLogger.TAG_QUOTA,
                    "Quota atomic update complete ("
                            + timings.tQuota
                            + " ms)"
            );

            // Stage 9: Persist the StoredFile and extracted metadata.
            failedStage = "DATABASE";

            long databaseStart =
                    System.currentTimeMillis();

            StoredFile storedFile =
                    new StoredFile();

            storedFile.setOriginalFilename(
                    originalFilename
            );

            storedFile.setStoredFilename(
                    storedFilename
            );

            storedFile.setFileSize(fileSize);

            storedFile.setContentType(
                    file.getContentType() != null
                            ? file.getContentType()
                            : MediaType.APPLICATION_OCTET_STREAM_VALUE
            );

            storedFile.setSha256Hash(
                    sha256Hash
            );

            String relativeStoragePath =
                    (encrypt
                            ? "encrypted/"
                            : "uploads/")
                            + storedFilename;

            storedFile.setStoragePath(
                    relativeStoragePath
            );

            storedFile.setEncrypted(
                    encrypt
            );

            storedFile.setUser(
                    user
            );

            if (previewPath != null
                    && previewFilename != null) {

                storedFile.setHasPreview(true);

                storedFile.setPreviewPath(
                        "previews/" + previewFilename
                );

                storedFile.setPreviewMimeType(
                        MediaType.IMAGE_JPEG_VALUE
                );

            } else {
                storedFile.setHasPreview(false);
            }

            StoredFile savedFile =
                    fileRepository.save(storedFile);

            if (fileMetadata != null
                    && fileMetadataRepository != null) {

                try {
                    fileMetadata.setFile(savedFile);

                    fileMetadataRepository.save(
                            fileMetadata
                    );

                    savedFile.setMetadata(
                            fileMetadata
                    );

                } catch (Exception e) {
                    log.warn(
                            "Failed saving metadata for file ID {}: {}",
                            savedFile.getId(),
                            e.getMessage()
                    );
                }
            }

            timings.tDatabase =
                    Math.max(
                            1,
                            System.currentTimeMillis()
                                    - databaseStart
                    );

            timings.tTotal =
                    Math.max(
                            1,
                            System.currentTimeMillis()
                                    - uploadStart
                    );

            ConsoleLogger.stage(
                    reqId,
                    ConsoleLogger.TAG_DB,
                    "StoredFile persisted with ID="
                            + savedFile.getId()
                            + " ("
                            + timings.tDatabase
                            + " ms)"
            );

            ConsoleLogger.logFileUploadTrace(
                    reqId,
                    user.getId(),
                    user.getUsername(),
                    originalFilename,
                    fileSize,
                    file.getContentType(),
                    fileCategoryService != null
                            ? fileCategoryService.determineCategory(
                                    originalFilename,
                                    file.getContentType()
                            )
                            : "Other",
                    sha256Hash,
                    previewPath != null,
                    previewPath != null
                            ? "IMAGE"
                            : "NONE",
                    fileMetadata != null,
                    fileMetadata != null
                            ? fileMetadata.getCameraMake()
                            : null,
                    fileMetadata != null
                            ? fileMetadata.getResolution()
                            : null,
                    encrypt,
                    quotaBefore,
                    user.getStorageLimit(),
                    timings
            );

            return savedFile;

        } catch (DuplicateFileException
                 | StorageQuotaExceededException e) {

            cleanupFailure(
                    storagePath,
                    previewPath,
                    quotaIncremented,
                    user,
                    fileSize
            );

            throw e;

        } catch (Exception e) {
            log.error(
                    "File upload failed: {}",
                    e.getMessage(),
                    e
            );

            boolean tempCleaned =
                    cleanupFailure(
                            storagePath,
                            previewPath,
                            quotaIncremented,
                            user,
                            fileSize
                    );

            ConsoleLogger.logErrorTrace(
                    reqId,
                    "File upload",
                    originalFilename != null
                            ? originalFilename
                            : file != null
                            ? file.getOriginalFilename()
                            : "unknown",
                    user != null
                            ? user.getId()
                            : null,
                    failedStage,
                    e,
                    tempCleaned,
                    quotaIncremented
            );

            throw e;

        } finally {
            if (tempSourceFile != null) {
                try {
                    Files.deleteIfExists(
                            tempSourceFile
                    );
                    log.debug(
                            "Temporary source file cleaned."
                    );
                } catch (Exception ignored) {
                    // Cleanup failure must not hide the original upload result.
                }
            }
        }
    }

    private boolean cleanupFailure(
            Path storagePath,
            Path previewPath,
            boolean quotaIncremented,
            User user,
            long fileSize) {

        boolean cleaned = false;

        if (storagePath != null) {
            try {
                Files.deleteIfExists(storagePath);
                cleaned = true;
            } catch (Exception e) {
                log.warn(
                        "Failed to clean up stored file: {}",
                        e.getMessage()
                );
            }
        }

        if (previewPath != null) {
            try {
                Files.deleteIfExists(previewPath);
            } catch (Exception e) {
                log.warn(
                        "Failed to clean up preview file: {}",
                        e.getMessage()
                );
            }
        }

        if (quotaIncremented && user != null) {
            try {
                userRepository.decrementStorageUsedAtomic(
                        user.getId(),
                        fileSize
                );
            } catch (Exception e) {
                log.error(
                        "Failed to roll back quota for user {}: {}",
                        user.getId(),
                        e.getMessage()
                );
            }
        }

        return cleaned;
    }

    /**
     * Resolve download parameters and streaming response body for a stored file.
     */
    public DownloadPayload prepareDownload(
            User user,
            Long fileId,
            boolean decrypt) throws Exception {

        String reqId =
                RequestContext.getRequestId();

        long downloadStart =
                System.currentTimeMillis();

        StoredFile storedFile =
                fileRepository
                        .findByIdAndUser(fileId, user)
                        .orElse(null);

        if (storedFile == null) {
            log.warn(
                    "File not found or user does not own file ID: {}",
                    fileId
            );

            return null;
        }

        Path storageRoot =
                FileStorageConfig.STORAGE_ROOT
                        .toAbsolutePath()
                        .normalize();

        Path filePath =
                FileStorageConfig.resolvePath(
                        storedFile.getStoragePath()
                );

        if (filePath == null
                || !filePath.startsWith(storageRoot)) {

            log.error(
                    "Invalid file storage path: {}",
                    storedFile.getStoragePath()
            );

            throw new SecurityException(
                    "Invalid storage path"
            );
        }

        if (!Files.exists(filePath)) {
            log.error(
                    "Physical file does not exist on disk: {}",
                    filePath
            );

            throw new FileNotFoundException(
                    "Physical file not found on disk"
            );
        }

        String downloadFilename =
                storedFile.getOriginalFilename();

        boolean shouldDecrypt =
                storedFile.isEncrypted()
                        && decrypt;

        SecretKey userKey =
                shouldDecrypt
                        ? keyManagementService
                        .getOrGenerateUserKey(user)
                        : null;

        ConsoleLogger.stage(
                reqId,
                ConsoleLogger.TAG_FILE,
                "Download initialization: "
                        + downloadFilename
                        + " (ID="
                        + fileId
                        + ")"
        );

        if (shouldDecrypt) {
            ConsoleLogger.stage(
                    reqId,
                    ConsoleLogger.TAG_CRYPTO,
                    "AES-256-GCM streaming decryption ready"
            );

        } else if (storedFile.isEncrypted()) {
            ConsoleLogger.stage(
                    reqId,
                    ConsoleLogger.TAG_CRYPTO,
                    "Raw ciphertext requested (decryption bypassed)"
            );

        } else {
            ConsoleLogger.stage(
                    reqId,
                    ConsoleLogger.TAG_CRYPTO,
                    "Unencrypted file download (decryption not required)"
            );
        }

        long contentLength =
                shouldDecrypt
                        ? storedFile.getFileSize() != null
                        ? storedFile.getFileSize()
                        : 0L
                        : Files.size(filePath);

        StreamingResponseBody body =
                outputStream -> {

                    try (InputStream input =
                                 Files.newInputStream(filePath)) {

                        if (shouldDecrypt) {
                            encryptionService.decryptStream(
                                    input,
                                    outputStream,
                                    userKey
                            );
                        } else {
                            byte[] buffer =
                                    new byte[BUFFER_SIZE];

                            int bytesRead;

                            while ((bytesRead =
                                    input.read(buffer)) != -1) {

                                outputStream.write(
                                        buffer,
                                        0,
                                        bytesRead
                                );
                            }

                            outputStream.flush();
                        }

                        long duration =
                                Math.max(
                                        1,
                                        System.currentTimeMillis()
                                                - downloadStart
                                );

                        ConsoleLogger.logFileDownloadTrace(
                                reqId,
                                fileId,
                                downloadFilename,
                                user.getId(),
                                decrypt,
                                storedFile.isEncrypted(),
                                shouldDecrypt,
                                storedFile.getFileSize() != null
                                        ? storedFile.getFileSize()
                                        : 0L,
                                storedFile.getSha256Hash(),
                                storedFile.getContentType(),
                                duration
                        );

                    } catch (Exception e) {
                        log.error(
                                "Streaming error during file download for ID {}: {}",
                                fileId,
                                e.getMessage(),
                                e
                        );

                        throw new RuntimeException(e);
                    }
                };

        return new DownloadPayload(
                body,
                contentLength,
                storedFile.getContentType(),
                downloadFilename,
                storedFile.getSha256Hash(),
                storedFile.isEncrypted()
        );
    }

    /**
     * Delete stored file, reclaim storage quota atomically,
     * and remove physical files.
     */
    @Transactional
    public boolean deleteFile(
            User user,
            Long fileId) {

        String reqId =
                RequestContext.getRequestId();

        StoredFile storedFile =
                fileRepository
                        .findByIdAndUser(fileId, user)
                        .orElse(null);

        if (storedFile == null) {
            log.warn(
                    "File not found or user does not own file ID: {}",
                    fileId
            );

            return false;
        }

        try {
            long fileSize =
                    storedFile.getFileSize() != null
                            ? storedFile.getFileSize()
                            : 0L;

            String storagePath =
                    storedFile.getStoragePath();

            String previewPath =
                    storedFile.getPreviewPath();

            String originalFilename =
                    storedFile.getOriginalFilename();

            // 1. Remove the encrypted or unencrypted physical file first.
            try {
                Path filePath = FileStorageConfig.resolvePath(storagePath);
                if (filePath != null) {
                    Files.deleteIfExists(filePath);
                }
            } catch (Exception e) {
                log.warn(
                        "Physical file deletion failed for ID {}: {}",
                        fileId,
                        e.getMessage()
                );
                // Throw exception to trigger 500 error and rollback if I/O fails
                throw new RuntimeException("Failed to delete physical file", e);
            }

            // 2. Remove the preview file, but don't fail the whole process if this fails.
            boolean previewDeleted = false;
            if (previewPath != null) {
                try {
                    Path previewFile =
                            FileStorageConfig.resolvePath(
                                    previewPath
                            );

                    if (previewFile != null
                            && Files.exists(previewFile)) {

                        Files.deleteIfExists(
                                previewFile
                        );
                        previewDeleted = true;
                    }
                } catch (Exception e) {
                    log.warn(
                            "Preview file deletion warning for ID {}: {}",
                            fileId,
                            e.getMessage()
                    );
                }
            }

            // 3. Release the user's storage quota now that physical deletion succeeded.
            userRepository.decrementStorageUsedAtomic(
                    user.getId(),
                    fileSize
            );

            long currentUsedStorage =
                    user.getUsedStorage() != null
                            ? user.getUsedStorage()
                            : 0L;

            user.setUsedStorage(
                    Math.max(
                            0L,
                            currentUsedStorage - fileSize
                    )
            );

            ConsoleLogger.stage(
                    reqId,
                    ConsoleLogger.TAG_QUOTA,
                    "Quota decremented by "
                            + ConsoleLogger.formatSize(fileSize)
            );

            // 4. Delete the database record and associated metadata.
            fileRepository.delete(storedFile);
            fileRepository.flush();

            ConsoleLogger.stage(
                    reqId,
                    ConsoleLogger.TAG_DB,
                    "StoredFile & FileMetadata deleted (ID="
                            + fileId
                            + ")"
            );

            ConsoleLogger.logFileDeleteTrace(
                    reqId,
                    fileId,
                    originalFilename,
                    user.getId(),
                    fileSize,
                    previewDeleted
                            || previewPath != null
            );

            return true;

        } catch (Exception e) {
            log.error(
                    "Failed to delete file ID: {} - {}",
                    fileId,
                    e.getMessage(),
                    e
            );

            throw new RuntimeException(
                    "Failed to delete file",
                    e
            );
        }
    }

    /**
     * Retrieve preview bytes from disk.
     */
    public byte[] getPreviewBytes(StoredFile storedFile) throws Exception {

        String reqId =
                RequestContext.getRequestId();

        long previewStart =
                System.currentTimeMillis();

        if (storedFile == null
                || !storedFile.isHasPreview()
                || storedFile.getPreviewPath() == null) {

            return null;
        }

        Path storageRoot =
                FileStorageConfig.STORAGE_ROOT
                        .toAbsolutePath()
                        .normalize();

        Path previewPath =
                FileStorageConfig.resolvePath(
                        storedFile.getPreviewPath()
                );

        if (previewPath == null
                || !previewPath.startsWith(storageRoot)
                || !Files.exists(previewPath)) {

            log.warn(
                    "Preview file missing or invalid path: {}",
                    storedFile.getPreviewPath()
            );

            return null;
        }

        byte[] previewData =
                Files.readAllBytes(previewPath);

        long duration =
                Math.max(
                        1,
                        System.currentTimeMillis()
                                - previewStart
                );

        ConsoleLogger.logPreviewTrace(
                reqId,
                storedFile.getId(),
                storedFile.getPreviewMimeType(),
                "DiskCache",
                true,
                duration
        );

        return previewData;
    }

    /**
     * Regenerate preview from the original/decrypted storage file.
     */
    public boolean regeneratePreview(
            User user,
            Long fileId) throws Exception {

        StoredFile storedFile =
                fileRepository
                        .findByIdAndUser(fileId, user)
                        .orElse(null);

        if (storedFile == null) {
            return false;
        }

        Path filePath =
                FileStorageConfig.resolvePath(
                        storedFile.getStoragePath()
                );

        if (filePath == null
                || !Files.exists(filePath)) {

            return false;
        }

        Path sourceToRead = null;
        Path tempDecrypted = null;

        try {
            if (storedFile.isEncrypted()) {
                SecretKey userKey =
                        keyManagementService
                                .getOrGenerateUserKey(user);

                tempDecrypted =
                        Files.createTempFile(
                                "cv_regen_prev_",
                                ".tmp"
                        );

                try (InputStream input =
                             Files.newInputStream(filePath);
                     OutputStream output =
                             Files.newOutputStream(tempDecrypted)) {

                    encryptionService.decryptStream(
                            input,
                            output,
                            userKey
                    );
                }

                sourceToRead = tempDecrypted;

            } else {
                sourceToRead = filePath;
            }

            byte[] previewData =
                    mediaPreviewService != null
                            ? mediaPreviewService.generatePreview(
                                    sourceToRead,
                                    storedFile.getOriginalFilename(),
                                    storedFile.getContentType()
                            )
                            : null;

            if (previewData == null
                    || previewData.length == 0) {

                return false;
            }

            if (storedFile.getPreviewPath() != null) {
                try {
                    Path oldPreview =
                            FileStorageConfig.resolvePath(
                                    storedFile.getPreviewPath()
                            );

                    if (oldPreview != null) {
                        Files.deleteIfExists(oldPreview);
                    }

                } catch (Exception e) {
                    log.warn(
                            "Failed to remove old preview for file ID {}: {}",
                            fileId,
                            e.getMessage()
                    );
                }
            }

            String previewFilename =
                    "preview-" + UUID.randomUUID() + ".jpg";

            Path storageRoot =
                    FileStorageConfig.STORAGE_ROOT
                            .toAbsolutePath()
                            .normalize();

            Path candidatePreviewPath =
                    FileStorageConfig.PREVIEW_STORAGE
                            .toAbsolutePath()
                            .normalize()
                            .resolve(previewFilename)
                            .normalize();

            if (!candidatePreviewPath.startsWith(storageRoot)) {
                throw new SecurityException(
                        "Invalid preview storage path"
                );
            }

            Files.createDirectories(
                    FileStorageConfig.PREVIEW_STORAGE
            );

            Files.write(
                    candidatePreviewPath,
                    previewData
            );

            storedFile.setHasPreview(true);

            storedFile.setPreviewPath(
                    "previews/" + previewFilename
            );

            storedFile.setPreviewMimeType(
                    MediaType.IMAGE_JPEG_VALUE
            );

            fileRepository.save(storedFile);

            return true;

        } finally {
            if (tempDecrypted != null) {
                try {
                    Files.deleteIfExists(
                            tempDecrypted
                    );
                } catch (Exception e) {
                    log.warn(
                            "Failed to clean temporary decrypted preview source: {}",
                            e.getMessage()
                    );
                }
            }
        }
    }

    private String calculateSha256(Path file) {
        try (InputStream input =
                     Files.newInputStream(file)) {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] buffer =
                    new byte[BUFFER_SIZE];

            int read;

            while ((read = input.read(buffer)) != -1) {
                digest.update(
                        buffer,
                        0,
                        read
                );
            }

            return HexFormat
                    .of()
                    .formatHex(
                            digest.digest()
                    );

        } catch (Exception e) {
            throw new RuntimeException(
                    "SHA-256 calculation failed.",
                    e
            );
        }
    }

    private String getExtension(String filename) {
        int lastDot =
                filename.lastIndexOf('.');

        return lastDot == -1
                ? ""
                : filename.substring(lastDot);
    }

    public static class DownloadPayload {

        private final StreamingResponseBody body;
        private final long contentLength;
        private final String contentType;
        private final String filename;
        private final String sha256;
        private final boolean encrypted;

        public DownloadPayload(
                StreamingResponseBody body,
                long contentLength,
                String contentType,
                String filename,
                String sha256,
                boolean encrypted) {

            this.body = body;
            this.contentLength = contentLength;
            this.contentType = contentType;
            this.filename = filename;
            this.sha256 = sha256;
            this.encrypted = encrypted;
        }

        public StreamingResponseBody getBody() {
            return body;
        }

        public long getContentLength() {
            return contentLength;
        }

        public String getContentType() {
            return contentType;
        }

        public String getFilename() {
            return filename;
        }

        public String getSha256() {
            return sha256;
        }

        public boolean isEncrypted() {
            return encrypted;
        }
    }
}