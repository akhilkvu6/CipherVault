package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.logging.ConsoleLogger;
import com.ciphervault.ciphervault.logging.RequestContext;
import com.ciphervault.ciphervault.security.KeyManagementService;
import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.security.MessageDigest;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final MediaPreviewService mediaPreviewService;
    private final KeyManagementService keyManagementService;
    private final MetadataExtractionService metadataExtractionService;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileCategoryService fileCategoryService;

    @Autowired
    public FileStorageService(FileRepository fileRepository,
                              UserRepository userRepository,
                              EncryptionService encryptionService,
                              MediaPreviewService mediaPreviewService,
                              KeyManagementService keyManagementService,
                              @Autowired(required = false) MetadataExtractionService metadataExtractionService,
                              @Autowired(required = false) FileMetadataRepository fileMetadataRepository,
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
    public StoredFile storeFile(User user, MultipartFile file, boolean encrypt) throws Exception {
        long uploadStart = System.currentTimeMillis();
        ConsoleLogger.UploadTimingTracker timings = new ConsoleLogger.UploadTimingTracker();
        String reqId = RequestContext.getRequestId();
        String failedStage = "VALIDATION";

        Path tempSourceFile = null;
        Path storagePath = null;
        Path previewPath = null;
        long fileSize = 0L;
        boolean quotaIncremented = false;
        long quotaBefore = user.getUsedStorage() != null ? user.getUsedStorage() : 0L;
        String originalFilename = null;

        try {
            long sVal = System.currentTimeMillis();
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File cannot be empty");
            }

            originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                originalFilename = "file";
            }

            String extension = getExtension(originalFilename);

            // Stream MultipartFile directly into a temporary source file on disk
            tempSourceFile = Files.createTempFile("cv_upload_src_", extension);
            try (InputStream is = file.getInputStream();
                 OutputStream os = Files.newOutputStream(tempSourceFile)) {
                byte[] buffer = new byte[16384];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }

            fileSize = Files.size(tempSourceFile);

            if (user.getUsedStorage() + fileSize > user.getStorageLimit()) {
                log.warn("Upload rejected: storage quota exceeded for user: {}", user.getEmail());
                throw new StorageQuotaExceededException("Storage quota exceeded");
            }

            String storedFilename = UUID.randomUUID() + extension;
            Path storageDirectory = encrypt
                    ? FileStorageConfig.ENCRYPTED_STORAGE
                    : FileStorageConfig.NORMAL_STORAGE;

            Path storageRoot = FileStorageConfig.STORAGE_ROOT.toAbsolutePath().normalize();
            Path absoluteStorageDirectory = storageDirectory.toAbsolutePath().normalize();
            storagePath = absoluteStorageDirectory.resolve(storedFilename).normalize();

            if (!storagePath.startsWith(storageRoot)) {
                log.error("Invalid storage path detected: {}", storagePath);
                throw new SecurityException("Invalid storage path");
            }
            timings.tValidation = Math.max(1, System.currentTimeMillis() - sVal);
            ConsoleLogger.stage(reqId, ConsoleLogger.TAG_FILE, "Upload validation passed (" + timings.tValidation + " ms)");

            // Stage 2: SHA-256
            failedStage = "SHA-256";
            long sSha = System.currentTimeMillis();
            String sha256Hash = calculateSha256(tempSourceFile);
            timings.tSha256 = Math.max(1, System.currentTimeMillis() - sSha);
            ConsoleLogger.stage(reqId, ConsoleLogger.TAG_HASH, "SHA-256 calculated: " + ConsoleLogger.truncateSha(sha256Hash) + " (" + timings.tSha256 + " ms)");

            // Stage 3: Duplicate Check
            failedStage = "DUPLICATE CHECK";
            long sDup = System.currentTimeMillis();
            Optional<StoredFile> existingDup = fileRepository.findByUserAndSha256Hash(user, sha256Hash);
            timings.tDuplicateCheck = Math.max(1, System.currentTimeMillis() - sDup);
            if (existingDup.isPresent()) {
                ConsoleLogger.logDuplicateDetectedTrace(
                        reqId, originalFilename, fileSize, sha256Hash, user.getId(),
                        existingDup.get().getId(), existingDup.get().getOriginalFilename()
                );
                throw new DuplicateFileException("Duplicate file already exists", sha256Hash);
            }
            ConsoleLogger.stage(reqId, ConsoleLogger.TAG_DB, "Duplicate check complete: UNIQUE (" + timings.tDuplicateCheck + " ms)");

            // Stage 4: Preview Generation (before encryption)
            failedStage = "PREVIEW";
            long sPrev = System.currentTimeMillis();
            byte[] previewData = null;
            try {
                if (mediaPreviewService != null) {
                    previewData = mediaPreviewService.generatePreview(tempSourceFile, originalFilename, file.getContentType());
                }
            } catch (Exception e) {
                log.warn("Preview generation error (continuing upload): {}", e.getMessage());
            }

            String previewFilename = null;
            if (previewData != null && previewData.length > 0) {
                previewFilename = "preview-" + UUID.randomUUID() + ".jpg";
                Path candidatePreviewPath = FileStorageConfig.PREVIEW_STORAGE.toAbsolutePath().normalize().resolve(previewFilename).normalize();
                if (candidatePreviewPath.startsWith(storageRoot)) {
                    Files.createDirectories(FileStorageConfig.PREVIEW_STORAGE);
                    Files.write(candidatePreviewPath, previewData);
                    previewPath = candidatePreviewPath;
                } else {
                    previewFilename = null;
                }
            }
            timings.tPreview = Math.max(1, System.currentTimeMillis() - sPrev);
            ConsoleLogger.stage(reqId, ConsoleLogger.TAG_PREVIEW, "Preview generated: " + (previewPath != null ? "YES" : "NO") + " (" + timings.tPreview + " ms)");

            // Stage 5: Metadata Extraction (before encryption)
            failedStage = "METADATA";
            long sMeta = System.currentTimeMillis();
            FileMetadata fileMetadata = null;
            if (metadataExtractionService != null) {
                try {
                    fileMetadata = metadataExtractionService.extractMetadata(tempSourceFile, originalFilename, file.getContentType());
                } catch (Exception e) {
                    log.warn("Metadata extraction non-fatal error: {}", e.getMessage());
                }
            }
            timings.tMetadata = Math.max(1, System.currentTimeMillis() - sMeta);
            ConsoleLogger.stage(reqId, ConsoleLogger.TAG_META, "ExifTool metadata extracted: " + (fileMetadata != null ? "YES" : "NO") + " (" + timings.tMetadata + " ms)");

            // Stage 6: Encryption
            failedStage = "ENCRYPTION";
            long sEnc = System.currentTimeMillis();
            Files.createDirectories(absoluteStorageDirectory);

            if (encrypt) {
                SecretKey userKey = null;
                if (keyManagementService != null) {
                    userKey = keyManagementService.getOrGenerateUserKey(user);
                }
                try (InputStream fis = Files.newInputStream(tempSourceFile);
                     OutputStream fos = Files.newOutputStream(storagePath)) {
                    encryptionService.encryptStream(fis, fos, userKey);
                }
            } else {
                Files.copy(tempSourceFile, storagePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            timings.tEncryption = Math.max(1, System.currentTimeMillis() - sEnc);
            if (encrypt) {
                ConsoleLogger.stage(reqId, ConsoleLogger.TAG_CRYPTO, "AES-256-GCM streaming encryption complete (" + timings.tEncryption + " ms)");
            } else {
                ConsoleLogger.stage(reqId, ConsoleLogger.TAG_CRYPTO, "Encryption disabled / bypassed (" + timings.tEncryption + " ms)");
            }

            // Stage 7: Storage Write Verification
            failedStage = "STORAGE";
            long sStore = System.currentTimeMillis();
            timings.tStorage = Math.max(1, System.currentTimeMillis() - sStore);
            if (encrypt) {
                ConsoleLogger.stage(reqId, ConsoleLogger.TAG_STORAGE, "Encrypted file persisted (" + timings.tStorage + " ms)");
            } else {
                ConsoleLogger.stage(reqId, ConsoleLogger.TAG_STORAGE, "Unencrypted file persisted (" + timings.tStorage + " ms)");
            }

            // Stage 8: Quota Update
            failedStage = "QUOTA";
            long sQuota = System.currentTimeMillis();
            int rowsUpdated = userRepository.incrementStorageUsedAtomic(user.getId(), fileSize);
            if (rowsUpdated == 0) {
                if (storagePath != null) Files.deleteIfExists(storagePath);
                if (previewPath != null) Files.deleteIfExists(previewPath);
                throw new StorageQuotaExceededException("Storage quota exceeded");
            }
            quotaIncremented = true;
            user.setUsedStorage(user.getUsedStorage() + fileSize);
            timings.tQuota = Math.max(1, System.currentTimeMillis() - sQuota);
            ConsoleLogger.stage(reqId, ConsoleLogger.TAG_QUOTA, "Quota atomic update complete (" + timings.tQuota + " ms)");

            // Stage 9: Database Persistence
            failedStage = "DATABASE";
            long sDb = System.currentTimeMillis();
            StoredFile storedFile = new StoredFile();
            storedFile.setOriginalFilename(originalFilename);
            storedFile.setStoredFilename(storedFilename);
            storedFile.setFileSize(fileSize);
            storedFile.setContentType(file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE);
            storedFile.setSha256Hash(sha256Hash);

            String relativeStoragePath = (encrypt ? "encrypted/" : "uploads/") + storedFilename;
            storedFile.setStoragePath(relativeStoragePath);
            storedFile.setEncrypted(encrypt);
            storedFile.setUser(user);
            if (previewPath != null && previewFilename != null) {
                storedFile.setHasPreview(true);
                storedFile.setPreviewPath("previews/" + previewFilename);
                storedFile.setPreviewMimeType("image/jpeg");
            } else {
                storedFile.setHasPreview(false);
            }

            StoredFile savedFile = fileRepository.save(storedFile);

            if (fileMetadata != null && fileMetadataRepository != null) {
                try {
                    fileMetadata.setFile(savedFile);
                    fileMetadataRepository.save(fileMetadata);
                    savedFile.setMetadata(fileMetadata);
                } catch (Exception e) {
                    log.warn("Failed saving metadata for file ID {}: {}", savedFile.getId(), e.getMessage());
                }
            }
            timings.tDatabase = Math.max(1, System.currentTimeMillis() - sDb);
            timings.tTotal = Math.max(1, System.currentTimeMillis() - uploadStart);
            ConsoleLogger.stage(reqId, ConsoleLogger.TAG_DB, "StoredFile persisted with ID=" + savedFile.getId() + " (" + timings.tDatabase + " ms)");

            ConsoleLogger.logFileUploadTrace(
                    reqId,
                    user.getId(),
                    user.getUsername(),
                    originalFilename,
                    fileSize,
                    file.getContentType(),
                    fileCategoryService != null ? fileCategoryService.determineCategory(originalFilename, file.getContentType()) : "Other",
                    sha256Hash,
                    previewPath != null,
                    previewPath != null ? "IMAGE" : "NONE",
                    fileMetadata != null,
                    fileMetadata != null ? fileMetadata.getCameraMake() : null,
                    fileMetadata != null ? fileMetadata.getResolution() : null,
                    encrypt,
                    quotaBefore,
                    user.getStorageLimit(),
                    timings
            );

            return savedFile;

        } catch (DuplicateFileException | StorageQuotaExceededException e) {
            cleanupFailure(storagePath, previewPath, quotaIncremented, user, fileSize);
            throw e;
        } catch (Exception e) {
            log.error("File upload failed: {}", e.getMessage(), e);
            boolean tempCleaned = cleanupFailure(storagePath, previewPath, quotaIncremented, user, fileSize);
            ConsoleLogger.logErrorTrace(
                    reqId,
                    "File upload",
                    originalFilename != null ? originalFilename : (file != null ? file.getOriginalFilename() : "unknown"),
                    user != null ? user.getId() : null,
                    failedStage,
                    e,
                    tempCleaned,
                    quotaIncremented
            );
            throw e;
        } finally {
            if (tempSourceFile != null) {
                try {
                    Files.deleteIfExists(tempSourceFile);
                    log.debug("Temporary source file cleaned.");
                } catch (Exception ignored) {}
            }
        }
    }

    private boolean cleanupFailure(Path storagePath, Path previewPath, boolean quotaIncremented, User user, long fileSize) {
        boolean cleaned = false;
        if (storagePath != null) {
            try { Files.deleteIfExists(storagePath); cleaned = true; } catch (Exception ignored) {}
        }
        if (previewPath != null) {
            try { Files.deleteIfExists(previewPath); } catch (Exception ignored) {}
        }
        if (quotaIncremented && user != null) {
            try {
                userRepository.decrementStorageUsedAtomic(user.getId(), fileSize);
            } catch (Exception ex) {
                log.error("Failed to roll back quota for user {}: {}", user.getId(), ex.getMessage());
            }
        }
        return cleaned;
    }

    /**
     * Resolve download parameters and streaming response body for a stored file.
     */
    public DownloadPayload prepareDownload(User user, Long fileId, boolean decrypt) throws Exception {
        String reqId = RequestContext.getRequestId();
        long sDownload = System.currentTimeMillis();

        StoredFile storedFile = fileRepository.findByIdAndUser(fileId, user).orElse(null);
        if (storedFile == null) {
            log.warn("File not found or user does not own file ID: {}", fileId);
            return null;
        }

        Path storageRoot = FileStorageConfig.STORAGE_ROOT.toAbsolutePath().normalize();
        Path filePath = FileStorageConfig.resolvePath(storedFile.getStoragePath());

        if (filePath == null || !filePath.startsWith(storageRoot)) {
            log.error("Invalid file storage path: {}", storedFile.getStoragePath());
            throw new SecurityException("Invalid storage path");
        }

        if (!Files.exists(filePath)) {
            log.error("Physical file does not exist on disk: {}", filePath);
            throw new FileNotFoundException("Physical file not found on disk");
        }

        String downloadFilename = storedFile.getOriginalFilename();
        boolean shouldDecrypt = storedFile.isEncrypted() && decrypt;
        SecretKey userKey = shouldDecrypt && keyManagementService != null
                ? keyManagementService.getOrGenerateUserKey(user)
                : null;

        ConsoleLogger.stage(reqId, ConsoleLogger.TAG_FILE, "Download initialization: " + downloadFilename + " (ID=" + fileId + ")");
        if (shouldDecrypt) {
            ConsoleLogger.stage(reqId, ConsoleLogger.TAG_CRYPTO, "AES-256-GCM streaming decryption ready");
        } else if (storedFile.isEncrypted()) {
            ConsoleLogger.stage(reqId, ConsoleLogger.TAG_CRYPTO, "Raw ciphertext requested (decryption bypassed)");
        } else {
            ConsoleLogger.stage(reqId, ConsoleLogger.TAG_CRYPTO, "Unencrypted file download (decryption not required)");
        }

        long contentLength = shouldDecrypt ? (storedFile.getFileSize() != null ? storedFile.getFileSize() : 0L) : Files.size(filePath);

        StreamingResponseBody body = outputStream -> {
            try (InputStream fis = Files.newInputStream(filePath)) {
                if (shouldDecrypt) {
                    encryptionService.decryptStream(fis, outputStream, userKey);
                } else {
                    byte[] buffer = new byte[16384];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                }
                long duration = Math.max(1, System.currentTimeMillis() - sDownload);
                ConsoleLogger.logFileDownloadTrace(
                        reqId,
                        fileId,
                        downloadFilename,
                        user.getId(),
                        decrypt,
                        storedFile.isEncrypted(),
                        shouldDecrypt,
                        storedFile.getFileSize() != null ? storedFile.getFileSize() : 0L,
                        storedFile.getSha256Hash(),
                        storedFile.getContentType(),
                        duration
                );
            } catch (Exception e) {
                log.error("Streaming error during file download for ID {}: {}", fileId, e.getMessage());
                throw new RuntimeException(e);
            }
        };

        return new DownloadPayload(storedFile, body, contentLength, storedFile.getContentType(), downloadFilename, storedFile.getSha256Hash(), storedFile.isEncrypted());
    }

    /**
     * Delete stored file, reclaim storage quota atomically, and remove physical files.
     */
    @Transactional
    public boolean deleteFile(User user, Long fileId) {
        String reqId = RequestContext.getRequestId();

        StoredFile storedFile = fileRepository.findByIdAndUser(fileId, user).orElse(null);
        if (storedFile == null) {
            log.warn("File not found or user does not own file ID: {}", fileId);
            return false;
        }

        try {
            long fileSize = storedFile.getFileSize() != null ? storedFile.getFileSize() : 0L;
            String storagePathStr = storedFile.getStoragePath();
            String previewPathStr = storedFile.getPreviewPath();
            String originalFilename = storedFile.getOriginalFilename();

            // 1. Decrement quota atomically in the database
            userRepository.decrementStorageUsedAtomic(user.getId(), fileSize);
            user.setUsedStorage(Math.max(0L, user.getUsedStorage() - fileSize));
            ConsoleLogger.stage(reqId, ConsoleLogger.TAG_QUOTA, "Quota decremented by " + ConsoleLogger.formatSize(fileSize));

            // 2. Delete database records (StoredFile and cascaded FileMetadata)
            fileRepository.delete(storedFile);
            fileRepository.flush();
            ConsoleLogger.stage(reqId, ConsoleLogger.TAG_DB, "StoredFile & FileMetadata deleted (ID=" + fileId + ")");

            // 3. Physical file cleanup on disk
            boolean fileDeleted = false;
            try {
                Path filePath = FileStorageConfig.resolvePath(storagePathStr);
                if (filePath != null && Files.exists(filePath)) {
                    Files.deleteIfExists(filePath);
                    fileDeleted = true;
                }
            } catch (Exception e) {
                log.warn("Physical file deletion warning for ID {}: {}", fileId, e.getMessage());
            }

            boolean previewDeleted = false;
            if (previewPathStr != null) {
                try {
                    Path previewPath = FileStorageConfig.resolvePath(previewPathStr);
                    if (previewPath != null && Files.exists(previewPath)) {
                        Files.deleteIfExists(previewPath);
                        previewDeleted = true;
                    }
                } catch (Exception e) {
                    log.warn("Preview file deletion warning for ID {}: {}", fileId, e.getMessage());
                }
            }

            ConsoleLogger.logFileDeleteTrace(
                    reqId,
                    fileId,
                    originalFilename,
                    user.getId(),
                    fileSize,
                    previewDeleted || previewPathStr != null
            );

            return true;
        } catch (Exception e) {
            log.error("Failed to delete file ID: {} - {}", fileId, e.getMessage());
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    /**
     * Retrieve preview bytes from disk.
     */
    public byte[] getPreviewBytes(User user, Long fileId) throws Exception {
        String reqId = RequestContext.getRequestId();
        long sPrev = System.currentTimeMillis();

        StoredFile storedFile = fileRepository.findByIdAndUser(fileId, user).orElse(null);
        if (storedFile == null || !storedFile.isHasPreview() || storedFile.getPreviewPath() == null) {
            return null;
        }

        Path storageRoot = FileStorageConfig.STORAGE_ROOT.toAbsolutePath().normalize();
        Path previewPath = FileStorageConfig.resolvePath(storedFile.getPreviewPath());

        if (previewPath == null || !previewPath.startsWith(storageRoot) || !Files.exists(previewPath)) {
            log.warn("Preview file missing or invalid path: {}", storedFile.getPreviewPath());
            return null;
        }

        byte[] previewData = Files.readAllBytes(previewPath);
        long duration = Math.max(1, System.currentTimeMillis() - sPrev);
        ConsoleLogger.logPreviewTrace(reqId, fileId, storedFile.getPreviewMimeType(), "DiskCache", true, duration);
        return previewData;
    }

    /**
     * Regenerate preview from original/decrypted storage file.
     */
    public boolean regeneratePreview(User user, Long fileId) throws Exception {
        StoredFile storedFile = fileRepository.findByIdAndUser(fileId, user).orElse(null);
        if (storedFile == null) {
            return false;
        }

        Path filePath = FileStorageConfig.resolvePath(storedFile.getStoragePath());
        if (filePath == null || !Files.exists(filePath)) {
            return false;
        }

        Path sourceToRead = null;
        Path tempDecrypted = null;
        try {
            if (storedFile.isEncrypted()) {
                SecretKey userKey = keyManagementService != null ? keyManagementService.getOrGenerateUserKey(user) : null;
                tempDecrypted = Files.createTempFile("cv_regen_prev_", ".tmp");
                try (InputStream fis = Files.newInputStream(filePath);
                     OutputStream fos = Files.newOutputStream(tempDecrypted)) {
                    encryptionService.decryptStream(fis, fos, userKey);
                }
                sourceToRead = tempDecrypted;
            } else {
                sourceToRead = filePath;
            }

            byte[] previewData = mediaPreviewService != null
                    ? mediaPreviewService.generatePreview(sourceToRead, storedFile.getOriginalFilename(), storedFile.getContentType())
                    : null;

            if (previewData != null && previewData.length > 0) {
                if (storedFile.getPreviewPath() != null) {
                    try {
                        Path oldPrev = FileStorageConfig.resolvePath(storedFile.getPreviewPath());
                        if (oldPrev != null) Files.deleteIfExists(oldPrev);
                    } catch (Exception ignored) {}
                }

                String previewFilename = "preview-" + UUID.randomUUID() + ".jpg";
                Path candidatePreviewPath = FileStorageConfig.PREVIEW_STORAGE.toAbsolutePath().normalize().resolve(previewFilename).normalize();
                Files.createDirectories(FileStorageConfig.PREVIEW_STORAGE);
                Files.write(candidatePreviewPath, previewData);

                storedFile.setHasPreview(true);
                storedFile.setPreviewPath("previews/" + previewFilename);
                storedFile.setPreviewMimeType("image/jpeg");
                fileRepository.save(storedFile);
                return true;
            }
            return false;
        } finally {
            if (tempDecrypted != null) {
                try { Files.deleteIfExists(tempDecrypted); } catch (Exception ignored) {}
            }
        }
    }

    private String calculateSha256(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[16384];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hash = digest.digest();
            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 calculation failed.", e);
        }
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot == -1 ? "" : filename.substring(lastDot);
    }

    public static class DownloadPayload {
        private final StoredFile storedFile;
        private final StreamingResponseBody body;
        private final long contentLength;
        private final String contentType;
        private final String filename;
        private final String sha256;
        private final boolean encrypted;

        public DownloadPayload(StoredFile storedFile, StreamingResponseBody body, long contentLength,
                               String contentType, String filename, String sha256, boolean encrypted) {
            this.storedFile = storedFile;
            this.body = body;
            this.contentLength = contentLength;
            this.contentType = contentType;
            this.filename = filename;
            this.sha256 = sha256;
            this.encrypted = encrypted;
        }

        public StoredFile getStoredFile() { return storedFile; }
        public StreamingResponseBody getBody() { return body; }
        public long contentLength() { return contentLength; }
        public long getContentLength() { return contentLength; }
        public String getContentType() { return contentType; }
        public String getFilename() { return filename; }
        public String getSha256() { return sha256; }
        public boolean isEncrypted() { return encrypted; }
    }
}
