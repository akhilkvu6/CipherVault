package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import com.ciphervault.ciphervault.util.ConsoleLogger;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final MediaPreviewService mediaPreviewService;

    public FileController(FileRepository fileRepository, UserRepository userRepository,
                          EncryptionService encryptionService, MediaPreviewService mediaPreviewService) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
        this.mediaPreviewService = mediaPreviewService;
        ConsoleLogger.success("FileController initialized successfully.");
    }

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file,
                                                         @RequestParam(value = "encrypt", defaultValue = "true") boolean encrypt,
                                                         Authentication authentication) {
        ConsoleLogger.info("File upload request received.");

        Path tempSourceFile = null;
        Path storagePath = null;
        Path previewPath = null;

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new FileUploadResponse(false, "Authentication required", null, null, null, null, null));
            }

            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new FileUploadResponse(false, "User not found", null, null, null, null, null));
            }

            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new FileUploadResponse(false, "File cannot be empty", null, null, null, null, null));
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) originalFilename = "file";

            String extension = getExtension(originalFilename);

            // Stream MultipartFile directly into a temporary source file on disk
            tempSourceFile = Files.createTempFile("cv_upload_src_", extension);
            try (InputStream is = file.getInputStream();
                 OutputStream os = Files.newOutputStream(tempSourceFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }

            long fileSize = Files.size(tempSourceFile);
            String sha256Hash = calculateSha256(tempSourceFile);

            if (fileRepository.existsByUserAndSha256Hash(user, sha256Hash)) {
                ConsoleLogger.warn("Duplicate file upload rejected for user: " + authentication.getName());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new FileUploadResponse(false, "Duplicate file already exists", null, null, null, null, sha256Hash));
            }

            if (user.getUsedStorage() + fileSize > user.getStorageLimit()) {
                ConsoleLogger.warn("Upload rejected: storage quota exceeded.");
                return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE)
                        .body(new FileUploadResponse(false, "Storage quota exceeded", null, null, null, null, sha256Hash));
            }

            String storedFilename = UUID.randomUUID() + extension;

            Path storageDirectory = encrypt
                    ? FileStorageConfig.ENCRYPTED_STORAGE
                    : FileStorageConfig.NORMAL_STORAGE;

            Path storageRoot = FileStorageConfig.STORAGE_ROOT.toAbsolutePath().normalize();
            Path absoluteStorageDirectory = storageDirectory.toAbsolutePath().normalize();
            storagePath = absoluteStorageDirectory.resolve(storedFilename).normalize();

            if (!storagePath.startsWith(storageRoot)) {
                ConsoleLogger.error("Invalid storage path detected.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new FileUploadResponse(false, "Invalid storage path", null, null, null, null, null));
            }

            // 1. Generate preview directly from temporary source file before encryption
            byte[] previewData = null;
            try {
                previewData = mediaPreviewService.generatePreview(tempSourceFile, originalFilename, file.getContentType());
            } catch (Exception e) {
                ConsoleLogger.warn("Preview generation error (continuing upload): " + e.getMessage());
            }

            if (previewData != null && previewData.length > 0) {
                String previewFilename = "preview-" + UUID.randomUUID() + ".jpg";
                Path candidatePreviewPath = FileStorageConfig.PREVIEW_STORAGE.toAbsolutePath().normalize().resolve(previewFilename).normalize();
                if (candidatePreviewPath.startsWith(storageRoot)) {
                    Files.createDirectories(FileStorageConfig.PREVIEW_STORAGE);
                    Files.write(candidatePreviewPath, previewData);
                    previewPath = candidatePreviewPath;
                    ConsoleLogger.success("Preview generated and stored successfully.");
                }
            }

            Files.createDirectories(absoluteStorageDirectory);

            if (encrypt) {
                ConsoleLogger.info("Encryption enabled. Encrypting uploaded file...");
                byte[] rawBytes = Files.readAllBytes(tempSourceFile);
                byte[] dataToStore = encryptionService.encrypt(rawBytes);
                Files.write(storagePath, dataToStore);
                ConsoleLogger.success("File encrypted successfully.");
            } else {
                ConsoleLogger.info("Encryption disabled. Storing original file...");
                Files.copy(tempSourceFile, storagePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            ConsoleLogger.success("File stored successfully on server.");

            StoredFile storedFile = new StoredFile();
            storedFile.setOriginalFilename(originalFilename);
            storedFile.setStoredFilename(storedFilename);
            storedFile.setFileSize(fileSize);
            storedFile.setContentType(file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE);
            storedFile.setSha256Hash(sha256Hash);
            storedFile.setStoragePath(storagePath.toString());
            storedFile.setEncrypted(encrypt);
            storedFile.setUser(user);
            if (previewPath != null) {
                storedFile.setHasPreview(true);
                storedFile.setPreviewPath(previewPath.toString());
                storedFile.setPreviewMimeType("image/jpeg");
            } else {
                storedFile.setHasPreview(false);
            }

            StoredFile savedFile = fileRepository.save(storedFile);

            user.setUsedStorage(user.getUsedStorage() + fileSize);
            userRepository.save(user);

            ConsoleLogger.success("File upload completed successfully.");

            return ResponseEntity.ok(new FileUploadResponse(true, "File uploaded successfully",
                    savedFile.getId(), originalFilename, fileSize, encrypt, sha256Hash));

        } catch (Exception e) {
            ConsoleLogger.error("File upload failed: " + e.getClass().getSimpleName());
            if (storagePath != null) {
                try { Files.deleteIfExists(storagePath); } catch (Exception ignored) {}
            }
            if (previewPath != null) {
                try { Files.deleteIfExists(previewPath); } catch (Exception ignored) {}
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new FileUploadResponse(false, "File upload failed", null, null, null, null, null));
        } finally {
            if (tempSourceFile != null) {
                try {
                    Files.deleteIfExists(tempSourceFile);
                    ConsoleLogger.info("Temporary source file cleaned successfully.");
                } catch (Exception ignored) {}
            }
        }
    }

    @GetMapping
    public ResponseEntity<List<FileResponse>> listFiles(Authentication authentication) {
        ConsoleLogger.info("File list request received.");

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        List<FileResponse> files = fileRepository.findByUser(user).stream()
                .map(file -> new FileResponse(file.getId(), file.getOriginalFilename(), file.getFileSize(),
                        file.getContentType(), file.isEncrypted(), file.getSha256Hash(), file.getCreatedAt(),
                        file.isHasPreview()))
                .toList();

        ConsoleLogger.success("File list retrieved successfully.");
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id,
                                               @RequestParam(value = "decrypt", defaultValue = "true") boolean decrypt,
                                               Authentication authentication) {
        ConsoleLogger.info("File download request received.");

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        StoredFile storedFile = fileRepository.findByIdAndUser(id, user).orElse(null);
        if (storedFile == null) {
            ConsoleLogger.warn("File not found or user does not own file ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            Path storageRoot = FileStorageConfig.STORAGE_ROOT.toAbsolutePath().normalize();
            Path filePath = Path.of(storedFile.getStoragePath()).toAbsolutePath().normalize();

            if (!filePath.startsWith(storageRoot)) {
                ConsoleLogger.error("Invalid file storage path detected.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            if (!Files.exists(filePath)) {
                ConsoleLogger.error("Physical file does not exist on server.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            ConsoleLogger.info("Reading file from secure storage...");
            byte[] storedData = Files.readAllBytes(filePath);
            byte[] responseData;
            String downloadFilename = storedFile.getOriginalFilename();

            if (storedFile.isEncrypted() && decrypt) {
                ConsoleLogger.info("Encrypted file detected. Decrypting...");
                responseData = encryptionService.decrypt(storedData);

                ConsoleLogger.info("Verifying downloaded file integrity...");
                String downloadedHash = calculateSha256(responseData);

                if (!downloadedHash.equalsIgnoreCase(storedFile.getSha256Hash())) {
                    ConsoleLogger.error("File integrity check failed: SHA-256 hash mismatch.");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }

                ConsoleLogger.success("File integrity verified: SHA-256 hash matches.");
            } else if (storedFile.isEncrypted()) {
                ConsoleLogger.info("Encrypted download requested. Returning raw encrypted file.");
                responseData = storedData;
                downloadFilename += ".encrypted";
            } else {
                ConsoleLogger.info("Unencrypted file detected.");
                responseData = storedData;

                ConsoleLogger.info("Verifying downloaded file integrity...");
                String downloadedHash = calculateSha256(responseData);

                if (!downloadedHash.equalsIgnoreCase(storedFile.getSha256Hash())) {
                    ConsoleLogger.error("File integrity check failed: SHA-256 hash mismatch.");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }

                ConsoleLogger.success("File integrity verified: SHA-256 hash matches.");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(storedFile.getContentType()));
            headers.setContentDisposition(ContentDisposition.attachment().filename(downloadFilename).build());
            headers.setContentLength(responseData.length);

            ConsoleLogger.success("File ready for download: " + downloadFilename);
            return new ResponseEntity<>(responseData, headers, HttpStatus.OK);

        } catch (Exception e) {
            ConsoleLogger.error("File download failed: " + e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable Long id,
                                                          Authentication authentication) {
        ConsoleLogger.info("File delete request received for ID: " + id);

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        StoredFile storedFile = fileRepository.findByIdAndUser(id, user).orElse(null);
        if (storedFile == null) {
            ConsoleLogger.warn("File not found or user does not own file ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            Path storageRoot = FileStorageConfig.STORAGE_ROOT.toAbsolutePath().normalize();
            Path filePath = Path.of(storedFile.getStoragePath()).toAbsolutePath().normalize();

            if (filePath.startsWith(storageRoot) && Files.exists(filePath)) {
                Files.delete(filePath);
                ConsoleLogger.info("Physical file deleted from disk: " + filePath);
            }

            if (storedFile.getPreviewPath() != null) {
                Path previewPath = Path.of(storedFile.getPreviewPath()).toAbsolutePath().normalize();
                if (previewPath.startsWith(storageRoot) && Files.exists(previewPath)) {
                    Files.delete(previewPath);
                    ConsoleLogger.info("Preview file deleted from disk: " + previewPath);
                }
            }

            long fileSize = storedFile.getFileSize() != null ? storedFile.getFileSize() : 0L;
            long newUsedStorage = Math.max(0L, user.getUsedStorage() - fileSize);
            user.setUsedStorage(newUsedStorage);
            userRepository.save(user);

            fileRepository.delete(storedFile);
            ConsoleLogger.success("File deleted successfully from database and user quota updated. User: " + user.getEmail());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "File deleted successfully");
            response.put("fileId", id);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            ConsoleLogger.error("Failed to delete file ID: " + id + " - " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<byte[]> getFilePreview(@PathVariable Long id,
                                                 Authentication authentication) {
        ConsoleLogger.info("File preview request received for ID: " + id);

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        StoredFile storedFile = fileRepository.findByIdAndUser(id, user).orElse(null);
        if (storedFile == null) {
            ConsoleLogger.warn("File not found or user does not own file ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (!storedFile.isHasPreview() || storedFile.getPreviewPath() == null) {
            ConsoleLogger.info("No preview available for file ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            Path storageRoot = FileStorageConfig.STORAGE_ROOT.toAbsolutePath().normalize();
            Path previewPath = Path.of(storedFile.getPreviewPath()).toAbsolutePath().normalize();

            if (!previewPath.startsWith(storageRoot) || !Files.exists(previewPath)) {
                ConsoleLogger.warn("Invalid or missing preview file path: " + previewPath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            byte[] previewData = Files.readAllBytes(previewPath);
            HttpHeaders headers = new HttpHeaders();
            String mimeType = storedFile.getPreviewMimeType() != null ? storedFile.getPreviewMimeType() : MediaType.IMAGE_JPEG_VALUE;
            headers.setContentType(MediaType.parseMediaType(mimeType));
            headers.setCacheControl(CacheControl.maxAge(1, java.util.concurrent.TimeUnit.DAYS).cachePrivate().getHeaderValue());
            headers.setContentLength(previewData.length);

            return new ResponseEntity<>(previewData, headers, HttpStatus.OK);
        } catch (Exception e) {
            ConsoleLogger.error("Failed to read preview for file ID: " + id + " - " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String calculateSha256(byte[] data) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder result = new StringBuilder();
            for (byte b : hash) result.append(String.format("%02x", b));
            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 calculation failed.", e);
        }
    }

    private String calculateSha256(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hash = digest.digest();
            StringBuilder result = new StringBuilder();
            for (byte b : hash) result.append(String.format("%02x", b));
            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 calculation failed.", e);
        }
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot == -1 ? "" : filename.substring(lastDot);
    }

    public static class FileUploadResponse {
        private boolean success;
        private String message;
        private Long fileId;
        private String filename;
        private Long fileSize;
        private Boolean encrypted;
        private String sha256Hash;

        public FileUploadResponse(boolean success, String message, Long fileId, String filename,
                                   Long fileSize, Boolean encrypted, String sha256Hash) {
            this.success = success;
            this.message = message;
            this.fileId = fileId;
            this.filename = filename;
            this.fileSize = fileSize;
            this.encrypted = encrypted;
            this.sha256Hash = sha256Hash;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Long getFileId() { return fileId; }
        public String getFilename() { return filename; }
        public Long getFileSize() { return fileSize; }
        public Boolean getEncrypted() { return encrypted; }
        public String getSha256Hash() { return sha256Hash; }
    }

    public static class FileResponse {
        private Long id;
        private String filename;
        private Long fileSize;
        private String contentType;
        private boolean encrypted;
        private String sha256Hash;
        private java.time.LocalDateTime createdAt;
        private boolean hasPreview;

        public FileResponse(Long id, String filename, Long fileSize, String contentType,
                            boolean encrypted, String sha256Hash, java.time.LocalDateTime createdAt,
                            boolean hasPreview) {
            this.id = id;
            this.filename = filename;
            this.fileSize = fileSize;
            this.contentType = contentType;
            this.encrypted = encrypted;
            this.sha256Hash = sha256Hash;
            this.createdAt = createdAt;
            this.hasPreview = hasPreview;
        }

        public FileResponse(Long id, String filename, Long fileSize, String contentType,
                            boolean encrypted, String sha256Hash, java.time.LocalDateTime createdAt) {
            this(id, filename, fileSize, contentType, encrypted, sha256Hash, createdAt, false);
        }

        public Long getId() { return id; }
        public String getFilename() { return filename; }
        public Long getFileSize() { return fileSize; }
        public String getContentType() { return contentType; }
        public boolean isEncrypted() { return encrypted; }
        public String getSha256Hash() { return sha256Hash; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public boolean isHasPreview() { return hasPreview; }
    }
}