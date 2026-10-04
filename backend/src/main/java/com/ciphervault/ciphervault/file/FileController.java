package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.logging.ConsoleLogger;
import com.ciphervault.ciphervault.logging.RequestContext;
import com.ciphervault.ciphervault.security.KeyManagementService;
import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final FileStorageService fileStorageService;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FileCategoryService fileCategoryService;
    private final FileSearchService fileSearchService;

    @Autowired
    public FileController(FileStorageService fileStorageService,
                          FileRepository fileRepository,
                          UserRepository userRepository,
                          FileCategoryService fileCategoryService,
                          FileSearchService fileSearchService) {
        this.fileStorageService = fileStorageService;
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.fileCategoryService = fileCategoryService;
        this.fileSearchService = fileSearchService;
    }

    /**
     * Backward-compatible constructor for testing and programmatic instantiation.
     */
    public FileController(FileRepository fileRepository,
                          UserRepository userRepository,
                          EncryptionService encryptionService,
                          MediaPreviewService mediaPreviewService,
                          KeyManagementService keyManagementService,
                          MetadataExtractionService metadataExtractionService,
                          FileMetadataRepository fileMetadataRepository,
                          FileCategoryService fileCategoryService,
                          FileSearchService fileSearchService) {
        this(
                new FileStorageService(
                        fileRepository,
                        userRepository,
                        encryptionService,
                        mediaPreviewService,
                        keyManagementService,
                        metadataExtractionService,
                        fileMetadataRepository,
                        fileCategoryService
                ),
                fileRepository,
                userRepository,
                fileCategoryService,
                fileSearchService
        );
    }

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file,
                                                         @RequestParam(value = "encrypt", defaultValue = "true") boolean encrypt,
                                                         Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new FileUploadResponse(false, "Authentication required", null, null, null, null, null));
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new FileUploadResponse(false, "User not found", null, null, null, null, null));
        }
        RequestContext.setUser(user.getEmail(), user.getId());

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new FileUploadResponse(false, "File cannot be empty", null, null, null, null, null));
        }

        try {
            StoredFile savedFile = fileStorageService.storeFile(user, file, encrypt);
            return ResponseEntity.ok(new FileUploadResponse(
                    true,
                    "File uploaded successfully",
                    savedFile.getId(),
                    savedFile.getOriginalFilename(),
                    savedFile.getFileSize(),
                    savedFile.isEncrypted(),
                    savedFile.getSha256Hash()
            ));
        } catch (DuplicateFileException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new FileUploadResponse(false, "Duplicate file already exists", null, null, null, null, e.getSha256Hash()));
        } catch (StorageQuotaExceededException e) {
            return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE)
                    .body(new FileUploadResponse(false, "Storage quota exceeded", null, null, null, null, null));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Database duplicate constraint violated: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new FileUploadResponse(false, "Duplicate file already exists", null, null, null, null, null));
        } catch (Exception e) {
            log.error("File upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new FileUploadResponse(false, "File upload failed: " + e.getMessage(), null, null, null, null, null));
        }
    }

    @GetMapping
    public ResponseEntity<List<FileResponse>> listFiles(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        RequestContext.setUser(user.getEmail(), user.getId());

        HttpHeaders headers = new HttpHeaders();
        List<FileResponse> files;

        if (page != null || size != null) {
            int pageNum = page != null ? Math.max(0, page) : 0;
            int pageSize = size != null ? Math.min(100, Math.max(1, size)) : 50;
            Page<StoredFile> pageResult = fileRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(pageNum, pageSize));
            files = pageResult.getContent().stream().map(this::mapToFileResponse).toList();
            headers.set("X-Total-Count", String.valueOf(pageResult.getTotalElements()));
            headers.set("X-Total-Pages", String.valueOf(pageResult.getTotalPages()));
            headers.set("X-Current-Page", String.valueOf(pageResult.getNumber()));
            headers.set("X-Page-Size", String.valueOf(pageResult.getSize()));
        } else {
            List<StoredFile> allFiles = fileRepository.findByUserOrderByCreatedAtDesc(user);
            files = allFiles.stream().map(this::mapToFileResponse).toList();
            headers.set("X-Total-Count", String.valueOf(files.size()));
            headers.set("X-Total-Pages", "1");
            headers.set("X-Current-Page", "0");
            headers.set("X-Page-Size", String.valueOf(files.size()));
        }

        return ResponseEntity.ok().headers(headers).body(files);
    }

    @GetMapping("/check-duplicate")
    public ResponseEntity<Map<String, Object>> checkDuplicate(
            @RequestParam("hash") String hash,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        Optional<StoredFile> existing = fileRepository.findByUserAndSha256Hash(user, hash.trim().toLowerCase());
        Map<String, Object> result = new LinkedHashMap<>();
        if (existing.isPresent()) {
            result.put("isDuplicate", true);
            result.put("existingFileName", existing.get().getOriginalFilename());
            result.put("fileId", existing.get().getId());
        } else {
            result.put("isDuplicate", false);
            result.put("existingFileName", null);
            result.put("fileId", null);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    public ResponseEntity<List<FileResponse>> searchFiles(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "cameraMake", required = false) String cameraMake,
            @RequestParam(value = "cameraModel", required = false) String cameraModel,
            @RequestParam(value = "resolution", required = false) String resolution,
            @RequestParam(value = "codec", required = false) String codec,
            @RequestParam(value = "artist", required = false) String artist,
            @RequestParam(value = "author", required = false) String author,
            @RequestParam(value = "genre", required = false) String genre,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String reqId = RequestContext.getRequestId();
        long sSearch = System.currentTimeMillis();

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        RequestContext.setUser(user.getEmail(), user.getId());

        HttpHeaders headers = new HttpHeaders();
        List<FileResponse> response;

        if (page != null || size != null) {
            int pageNum = page != null ? Math.max(0, page) : 0;
            int pageSize = size != null ? Math.min(100, Math.max(1, size)) : 50;
            Page<StoredFile> pageResult = fileSearchService.search(
                    user, query, category, cameraMake, cameraModel, resolution, codec, artist, author, genre,
                    PageRequest.of(pageNum, pageSize)
            );
            response = pageResult.getContent().stream().map(this::mapToFileResponse).toList();
            headers.set("X-Total-Count", String.valueOf(pageResult.getTotalElements()));
            headers.set("X-Total-Pages", String.valueOf(pageResult.getTotalPages()));
            headers.set("X-Current-Page", String.valueOf(pageResult.getNumber()));
            headers.set("X-Page-Size", String.valueOf(pageResult.getSize()));
        } else {
            List<StoredFile> files = fileSearchService.search(
                    user, query, category, cameraMake, cameraModel, resolution, codec, artist, author, genre
            );
            response = files.stream().map(this::mapToFileResponse).toList();
            headers.set("X-Total-Count", String.valueOf(response.size()));
            headers.set("X-Total-Pages", "1");
            headers.set("X-Current-Page", "0");
            headers.set("X-Page-Size", String.valueOf(response.size()));
        }

        long duration = Math.max(1, System.currentTimeMillis() - sSearch);
        StringBuilder filters = new StringBuilder();
        if (cameraMake != null && !cameraMake.isBlank()) filters.append("make=").append(cameraMake).append(" ");
        if (cameraModel != null && !cameraModel.isBlank()) filters.append("model=").append(cameraModel).append(" ");
        if (resolution != null && !resolution.isBlank()) filters.append("res=").append(resolution).append(" ");
        if (codec != null && !codec.isBlank()) filters.append("codec=").append(codec).append(" ");
        if (artist != null && !artist.isBlank()) filters.append("artist=").append(artist).append(" ");
        if (author != null && !author.isBlank()) filters.append("author=").append(author).append(" ");
        if (genre != null && !genre.isBlank()) filters.append("genre=").append(genre).append(" ");

        ConsoleLogger.logSearchTrace(
                reqId,
                user.getId(),
                query,
                category,
                filters.length() > 0 ? filters.toString().trim() : null,
                response.size(),
                duration
        );

        return ResponseEntity.ok().headers(headers).body(response);
    }

    public ResponseEntity<List<FileResponse>> searchFiles(
            String query, String category, String cameraMake, String cameraModel,
            String resolution, String codec, String artist, String author, String genre,
            Authentication authentication) {
        return searchFiles(query, category, cameraMake, cameraModel, resolution, codec, artist, author, genre, null, null, authentication);
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions(
            @RequestParam(value = "prefix", required = false) String prefix,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        List<String> suggestions = fileSearchService.getSuggestions(user, prefix);
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping(value = {"/{id}/download", "/download/{id}"})
    public ResponseEntity<StreamingResponseBody> downloadFile(@PathVariable Long id,
                                                              @RequestParam(value = "decrypt", defaultValue = "true") boolean decrypt,
                                                              Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        RequestContext.setUser(user.getEmail(), user.getId());

        try {
            FileStorageService.DownloadPayload payload = fileStorageService.prepareDownload(user, id, decrypt);
            if (payload == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(payload.getContentType()));
            headers.setContentDisposition(ContentDisposition.attachment().filename(payload.getFilename(), StandardCharsets.UTF_8).build());
            headers.setContentLength(payload.getContentLength());
            headers.set("X-File-SHA256", payload.getSha256());
            headers.set("X-Encrypted", String.valueOf(payload.isEncrypted()));

            return ResponseEntity.ok().headers(headers).body(payload.getBody());

        } catch (FileNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("File download initialization failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable Long id,
                                                          Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        RequestContext.setUser(user.getEmail(), user.getId());

        boolean deleted = fileStorageService.deleteFile(user, id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "File deleted successfully");
        response.put("fileId", id);

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = {"/{id}/preview", "/preview/{id}"})
    public ResponseEntity<byte[]> getFilePreview(@PathVariable Long id,
                                                 Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        RequestContext.setUser(user.getEmail(), user.getId());

        try {
            byte[] previewData = fileStorageService.getPreviewBytes(user, id);
            if (previewData == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            StoredFile storedFile = fileRepository.findByIdAndUser(id, user).orElse(null);
            String mimeType = storedFile != null && storedFile.getPreviewMimeType() != null
                    ? storedFile.getPreviewMimeType()
                    : MediaType.IMAGE_JPEG_VALUE;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(mimeType));
            headers.setCacheControl(CacheControl.maxAge(1, java.util.concurrent.TimeUnit.DAYS).cachePrivate().getHeaderValue());
            headers.setContentLength(previewData.length);

            return new ResponseEntity<>(previewData, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to read preview for file ID: {} - {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/regenerate-preview")
    public ResponseEntity<Map<String, Object>> regeneratePreview(@PathVariable Long id,
                                                                 Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            boolean success = fileStorageService.regeneratePreview(user, id);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Preview regenerated successfully", "hasPreview", true));
            } else {
                return ResponseEntity.ok(Map.of("success", false, "message", "Preview generation not supported for this file format", "hasPreview", false));
            }
        } catch (Exception e) {
            log.error("Failed to regenerate preview for file ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Preview regeneration failed: " + e.getMessage()));
        }
    }

    private FileResponse mapToFileResponse(StoredFile file) {
        String category = fileCategoryService.determineCategory(file.getOriginalFilename(), file.getContentType());
        FileMetadataDTO metaDTO = null;
        if (file.getMetadata() != null) {
            FileMetadata m = file.getMetadata();
            metaDTO = new FileMetadataDTO(
                    m.getCameraMake(), m.getCameraModel(), m.getLens(), m.getFocalLength(),
                    m.getIso(), m.getExposureTime(), m.getFNumber(), m.getDateTaken(),
                    m.getWidth(), m.getHeight(), m.getResolution(), m.getDuration(),
                    m.getVideoCodec(), m.getAudioCodec(), m.getFrameRate(), m.getBitrate(),
                    m.getTitle(), m.getArtist(), m.getAlbum(), m.getGenre(),
                    m.getReleaseYear(), m.getAuthor(), m.getCreator(), m.getSubject(),
                    m.getKeywords(), m.getDocCreatedDate(), m.getDocModifiedDate()
            );
        }
        return new FileResponse(
                file.getId(),
                file.getOriginalFilename(),
                file.getFileSize(),
                file.getContentType(),
                file.isEncrypted(),
                file.getSha256Hash(),
                file.getCreatedAt(),
                file.isHasPreview(),
                category,
                metaDTO
        );
    }

    // =========================
    // DTOs
    // =========================

    public static class FileMetadataDTO {
        private String cameraMake;
        private String cameraModel;
        private String lens;
        private String focalLength;
        private String iso;
        private String exposureTime;
        private String fNumber;
        private String dateTaken;
        private Integer width;
        private Integer height;
        private String resolution;
        private String duration;
        private String videoCodec;
        private String audioCodec;
        private String frameRate;
        private String bitrate;
        private String title;
        private String artist;
        private String album;
        private String genre;
        private String releaseYear;
        private String author;
        private String creator;
        private String subject;
        private String keywords;
        private String docCreatedDate;
        private String docModifiedDate;

        public FileMetadataDTO() {}

        public FileMetadataDTO(String cameraMake, String cameraModel, String lens, String focalLength,
                               String iso, String exposureTime, String fNumber, String dateTaken,
                               Integer width, Integer height, String resolution, String duration,
                               String videoCodec, String audioCodec, String frameRate, String bitrate,
                               String title, String artist, String album, String genre,
                               String releaseYear, String author, String creator, String subject,
                               String keywords, String docCreatedDate, String docModifiedDate) {
            this.cameraMake = cameraMake;
            this.cameraModel = cameraModel;
            this.lens = lens;
            this.focalLength = focalLength;
            this.iso = iso;
            this.exposureTime = exposureTime;
            this.fNumber = fNumber;
            this.dateTaken = dateTaken;
            this.width = width;
            this.height = height;
            this.resolution = resolution;
            this.duration = duration;
            this.videoCodec = videoCodec;
            this.audioCodec = audioCodec;
            this.frameRate = frameRate;
            this.bitrate = bitrate;
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.genre = genre;
            this.releaseYear = releaseYear;
            this.author = author;
            this.creator = creator;
            this.subject = subject;
            this.keywords = keywords;
            this.docCreatedDate = docCreatedDate;
            this.docModifiedDate = docModifiedDate;
        }

        public String getCameraMake() { return cameraMake; }
        public String getCameraModel() { return cameraModel; }
        public String getLens() { return lens; }
        public String getFocalLength() { return focalLength; }
        public String getIso() { return iso; }
        public String getExposureTime() { return exposureTime; }
        public String getFNumber() { return fNumber; }
        public String getDateTaken() { return dateTaken; }
        public Integer getWidth() { return width; }
        public Integer getHeight() { return height; }
        public String getResolution() { return resolution; }
        public String getDuration() { return duration; }
        public String getVideoCodec() { return videoCodec; }
        public String getAudioCodec() { return audioCodec; }
        public String getFrameRate() { return frameRate; }
        public String getBitrate() { return bitrate; }
        public String getTitle() { return title; }
        public String getArtist() { return artist; }
        public String getAlbum() { return album; }
        public String getGenre() { return genre; }
        public String getReleaseYear() { return releaseYear; }
        public String getAuthor() { return author; }
        public String getCreator() { return creator; }
        public String getSubject() { return subject; }
        public String getKeywords() { return keywords; }
        public String getDocCreatedDate() { return docCreatedDate; }
        public String getDocModifiedDate() { return docModifiedDate; }
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
        private String category;
        private FileMetadataDTO metadata;

        public FileResponse(Long id, String filename, Long fileSize, String contentType,
                            boolean encrypted, String sha256Hash, java.time.LocalDateTime createdAt,
                            boolean hasPreview, String category, FileMetadataDTO metadata) {
            this.id = id;
            this.filename = filename;
            this.fileSize = fileSize;
            this.contentType = contentType;
            this.encrypted = encrypted;
            this.sha256Hash = sha256Hash;
            this.createdAt = createdAt;
            this.hasPreview = hasPreview;
            this.category = category;
            this.metadata = metadata;
        }

        public Long getId() { return id; }
        public String getFilename() { return filename; }
        public Long getFileSize() { return fileSize; }
        public String getContentType() { return contentType; }
        public boolean isEncrypted() { return encrypted; }
        public String getSha256Hash() { return sha256Hash; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public boolean isHasPreview() { return hasPreview; }
        public String getCategory() { return category; }
        public FileMetadataDTO getMetadata() { return metadata; }
    }
}