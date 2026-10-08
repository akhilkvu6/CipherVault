package com.ciphervault.ciphervault.user;

import com.ciphervault.ciphervault.activity.ActivityEventRepository;
import com.ciphervault.ciphervault.activity.ActivityService;
import com.ciphervault.ciphervault.activity.EventType;
import com.ciphervault.ciphervault.file.FileCategoryService;
import com.ciphervault.ciphervault.file.FileRepository;
import com.ciphervault.ciphervault.file.FileStorageConfig;
import com.ciphervault.ciphervault.file.FileStorageService;
import com.ciphervault.ciphervault.file.StoredFile;
import com.ciphervault.ciphervault.transfer.TransferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FileCategoryService fileCategoryService;
    private final FileStorageService fileStorageService;
    private final TransferRepository transferRepository;
    private final ActivityEventRepository activityEventRepository;
    private final ActivityService activityService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public void setPasswordEncoder(org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    public UserController(
            UserRepository userRepository,
            FileRepository fileRepository,
            FileCategoryService fileCategoryService,
            @Autowired(required = false) FileStorageService fileStorageService,
            @Autowired(required = false) TransferRepository transferRepository,
            @Autowired(required = false) ActivityEventRepository activityEventRepository,
            @Autowired(required = false) ActivityService activityService) {

        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
        this.fileCategoryService = fileCategoryService;
        this.fileStorageService = fileStorageService;
        this.transferRepository = transferRepository;
        this.activityEventRepository = activityEventRepository;
        this.activityService = activityService;
    }

    public UserController(
            UserRepository userRepository,
            FileRepository fileRepository,
            FileCategoryService fileCategoryService) {
        this(userRepository, fileRepository, fileCategoryService, null, null, null, null);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        User user = userRepository
                .findByEmail(authentication.getName())
                .orElse(null);

        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .build();
        }

        List<Object[]> userFiles = fileRepository.findFileStatsByUser(user);

        long fileCount = userFiles.size();
        long encryptedCount = 0;

        Map<String, Long> categoryBytes = new LinkedHashMap<>();
        Map<String, Long> categoryCounts = new LinkedHashMap<>();

        // Initialize all standard categories so empty categories are included.
        for (String category : fileCategoryService.getStandardCategories()) {
            categoryBytes.put(category, 0L);
            categoryCounts.put(category, 0L);
        }

        for (Object[] row : userFiles) {
            String originalFilename = (String) row[0];
            String contentType = (String) row[1];
            Long fileSize = (Long) row[2];
            Boolean encrypted = (Boolean) row[3];
            
            if (Boolean.TRUE.equals(encrypted)) {
                encryptedCount++;
            }

            String category = fileCategoryService.determineCategory(
                    originalFilename,
                    contentType
            );

            long size = fileSize != null ? fileSize : 0L;

            categoryBytes.put(
                    category,
                    categoryBytes.getOrDefault(category, 0L) + size
            );

            categoryCounts.put(
                    category,
                    categoryCounts.getOrDefault(category, 0L) + 1L
            );
        }

        boolean photoAvailable = false;
        if (user.getProfilePhotoPath() != null) {
            Path photoPath = FileStorageConfig.resolvePath(user.getProfilePhotoPath());
            photoAvailable = photoPath != null && Files.exists(photoPath);
        }

        UserProfileResponse response = new UserProfileResponse(
                user.getId(),
                user.getName() != null ? user.getName() : user.getUsername(),
                user.getUsername(),
                user.getEmail(),
                user.getStorageLimit(),
                user.getUsedStorage(),
                fileCount,
                encryptedCount,
                user.getCreatedAt(),
                categoryBytes,
                categoryCounts,
                photoAvailable
        );

        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/username", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> updateUsername(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Authentication required"));
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "User not found"));
        }

        String newUsername = request != null ? request.get("username") : null;
        if (newUsername == null || newUsername.trim().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Username is required"));
        }

        newUsername = newUsername.trim();
        if (newUsername.length() < 3) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Username must be at least 3 characters"));
        }

        if (!newUsername.equalsIgnoreCase(user.getUsername()) && userRepository.existsByUsername(newUsername)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Username already exists"));
        }

        user.setUsername(newUsername);
        userRepository.save(user);

        if (activityService != null) {
            try {
                activityService.logEvent(
                        user,
                        EventType.USERNAME_CHANGED,
                        "Updated username to " + newUsername,
                        1,
                        0L,
                        null,
                        "SUCCESS",
                        null);
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "Username updated successfully",
                        "username", newUsername));
    }

    @RequestMapping(value = "/name", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> updateName(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Authentication required"));
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "User not found"));
        }

        String newName = request != null ? request.get("name") : null;
        if (newName == null || newName.trim().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Name is required"));
        }

        newName = newName.trim();
        if (newName.length() > 255) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Name must not exceed 255 characters"));
        }

        user.setName(newName);
        userRepository.save(user);

        if (activityService != null) {
            try {
                activityService.logEvent(
                        user,
                        EventType.USERNAME_CHANGED,
                        "Updated display name to " + newName,
                        1,
                        0L,
                        null,
                        "SUCCESS",
                        null);
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "Name updated successfully",
                        "name", newName));
    }

    @RequestMapping(value = "/password", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> updatePassword(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Authentication required"));
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "User not found"));
        }

        if (request == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Request body is required"));
        }

        String currentPassword = request.get("currentPassword");
        if (currentPassword == null || currentPassword.isBlank()) {
            currentPassword = request.get("oldPassword");
        }
        if (currentPassword == null || currentPassword.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Current password is required"));
        }

        if (passwordEncoder != null && !passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Incorrect current password"));
        }

        String newPassword = request.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "New password is required"));
        }

        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Password must be at least 8 characters"));
        }
        if (!newPassword.matches(".*[A-Z].*")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Password must contain at least one uppercase character"));
        }
        if (!newPassword.matches(".*[a-z].*")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Password must contain at least one lowercase character"));
        }
        if (!newPassword.matches(".*\\d.*")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Password must contain at least one number"));
        }
        if (!newPassword.matches(".*[^a-zA-Z0-9].*")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Password must contain at least one special symbol"));
        }

        if (newPassword.equals(currentPassword)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "New password cannot be the same as current password"));
        }

        String confirmPassword = request.get("confirmPassword");
        if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "New password and confirmation do not match"));
        }

        if (passwordEncoder != null) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        int tokenVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 1;
        user.setTokenVersion(tokenVersion + 1);
        userRepository.save(user);

        if (activityService != null) {
            try {
                activityService.logEvent(
                        user,
                        EventType.PASSWORD_CHANGED,
                        "Password changed successfully",
                        1,
                        0L,
                        null,
                        "SUCCESS",
                        null);
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "Password changed successfully"));
    }

    public ResponseEntity<Map<String, Object>> deleteAllData(Authentication authentication) {
        return deleteAllData(null, authentication);
    }

    @RequestMapping(value = {"/data", "/delete-all-data"}, method = {RequestMethod.DELETE, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> deleteAllData(
            @RequestBody(required = false) Map<String, String> request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Authentication required"));
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "User not found"));
        }

        if (request != null && request.containsKey("password") && request.get("password") != null && !request.get("password").isBlank()) {
            String rawPassword = request.get("password");
            if (passwordEncoder != null && !passwordEncoder.matches(rawPassword, user.getPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Incorrect password"));
            }
        }

        Object lockKey = user.getId() != null ? ("cv_user_data_" + user.getId()).intern() : new Object();
        synchronized (lockKey) {
            if (fileRepository != null) {
                List<StoredFile> files = fileRepository.findByUser(user);
                for (StoredFile f : files) {
                    try {
                        if (f.getStoragePath() != null) {
                            Path p = FileStorageConfig.resolvePath(f.getStoragePath());
                            if (p != null) {
                                Files.deleteIfExists(p);
                            }
                        }
                        if (f.getPreviewPath() != null) {
                            Path prev = FileStorageConfig.resolvePath(f.getPreviewPath());
                            if (prev != null) {
                                Files.deleteIfExists(prev);
                            }
                        }
                    } catch (Exception ignored) {}
                    try {
                        fileRepository.delete(f);
                    } catch (Exception ignored) {}
                }
                try {
                    fileRepository.flush();
                } catch (Exception ignored) {}
            }

            if (transferRepository != null) {
                try {
                    transferRepository.deleteByUser(user);
                    transferRepository.flush();
                } catch (Exception ignored) {}
            }

            if (activityEventRepository != null) {
                try {
                    activityEventRepository.deleteByUser(user);
                    activityEventRepository.flush();
                } catch (Exception ignored) {}
            }

            user.setUsedStorage(0L);
            userRepository.save(user);
            try {
                userRepository.flush();
            } catch (Exception ignored) {}

            if (activityService != null) {
                try {
                    activityService.logEvent(
                            user,
                            EventType.DATA_DELETED,
                            "Deleted all vault data",
                            1,
                            0L,
                            null,
                            "SUCCESS",
                            null);
                } catch (Exception ignored) {}
            }
        }

        return ResponseEntity.ok(
                Map.of("success", true, "message", "All data deleted successfully"));
    }

    public ResponseEntity<Map<String, Object>> deleteAccount(Authentication authentication) {
        return deleteAccount(null, authentication);
    }

    @RequestMapping(value = {"/account", "/me", "/delete-account"}, method = {RequestMethod.DELETE, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> deleteAccount(
            @RequestBody(required = false) Map<String, String> request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Authentication required"));
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "User not found"));
        }

        if (request != null && request.containsKey("password") && request.get("password") != null && !request.get("password").isBlank()) {
            String rawPassword = request.get("password");
            if (passwordEncoder != null && !passwordEncoder.matches(rawPassword, user.getPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Incorrect password"));
            }
        }

        Object lockKey = user.getId() != null ? ("cv_user_delete_" + user.getId()).intern() : new Object();
        synchronized (lockKey) {
            if (fileRepository != null) {
                List<StoredFile> files = fileRepository.findByUser(user);
                for (StoredFile f : files) {
                    try {
                        if (f.getStoragePath() != null) {
                            Path p = FileStorageConfig.resolvePath(f.getStoragePath());
                            if (p != null) {
                                Files.deleteIfExists(p);
                            }
                        }
                        if (f.getPreviewPath() != null) {
                            Path prev = FileStorageConfig.resolvePath(f.getPreviewPath());
                            if (prev != null) {
                                Files.deleteIfExists(prev);
                            }
                        }
                    } catch (Exception ignored) {}
                    try {
                        fileRepository.delete(f);
                    } catch (Exception ignored) {}
                }
                try {
                    fileRepository.flush();
                } catch (Exception ignored) {}
            }

            if (transferRepository != null) {
                try {
                    transferRepository.deleteByUser(user);
                    transferRepository.flush();
                } catch (Exception ignored) {}
            }

            if (activityEventRepository != null) {
                try {
                    activityEventRepository.deleteByUser(user);
                    activityEventRepository.flush();
                } catch (Exception ignored) {}
            }

            if (user.getProfilePhotoPath() != null) {
                try {
                    Path photoPath = FileStorageConfig.resolvePath(user.getProfilePhotoPath());
                    if (photoPath != null) {
                        Files.deleteIfExists(photoPath);
                    }
                } catch (Exception ignored) {}
            }

            try {
                userRepository.delete(user);
                userRepository.flush();
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(
                Map.of("success", true, "message", "Account deleted successfully"));
    }

    @PostMapping("/profile-photo")
    public ResponseEntity<Map<String, Object>> uploadProfilePhoto(
            @RequestParam(value = "photo", required = false) MultipartFile photoParam,
            @RequestParam(value = "file", required = false) MultipartFile fileParam,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Authentication required"));
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "User not found"));
        }

        MultipartFile file = photoParam != null ? photoParam : fileParam;
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Profile photo file is required"));
        }

        long maxPhotoSize = 5L * 1024L * 1024L; // 5 MB
        if (file.getSize() > maxPhotoSize) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Profile photo exceeds maximum allowable limit of 5 MB"));
        }

        String extension = null;
        String contentType = file.getContentType();
        if (contentType != null) {
            String ct = contentType.trim().toLowerCase(Locale.ROOT);
            if (ct.equals("image/jpeg") || ct.equals("image/jpg")) {
                extension = ".jpg";
            } else if (ct.equals("image/png")) {
                extension = ".png";
            } else if (ct.equals("image/webp")) {
                extension = ".webp";
            }
        }

        if (extension == null && file.getOriginalFilename() != null) {
            String orig = file.getOriginalFilename().trim().toLowerCase(Locale.ROOT);
            if (orig.endsWith(".jpg") || orig.endsWith(".jpeg")) {
                extension = ".jpg";
            } else if (orig.endsWith(".png")) {
                extension = ".png";
            } else if (orig.endsWith(".webp")) {
                extension = ".webp";
            }
        }

        if (extension == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Unsupported image format. Allowed formats: JPG, PNG, WebP"));
        }

        try {
            // Remove previous physical photo if present
            if (user.getProfilePhotoPath() != null) {
                Path oldPath = FileStorageConfig.resolvePath(user.getProfilePhotoPath());
                if (oldPath != null) {
                    Files.deleteIfExists(oldPath);
                }
            }

            Files.createDirectories(FileStorageConfig.PROFILE_PHOTO_STORAGE);
            String storedFilename = "profile_" + UUID.randomUUID() + extension;
            Path targetPath = FileStorageConfig.PROFILE_PHOTO_STORAGE.resolve(storedFilename).normalize();

            try (InputStream in = file.getInputStream();
                 OutputStream out = Files.newOutputStream(targetPath)) {
                byte[] buf = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buf)) != -1) {
                    out.write(buf, 0, bytesRead);
                }
                out.flush();
            }

            user.setProfilePhotoPath("profile_photos/" + storedFilename);
            userRepository.save(user);

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message", "Profile photo uploaded successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to store profile photo: " + e.getMessage()));
        }
    }

    @GetMapping("/profile-photo")
    public ResponseEntity<?> getProfilePhoto(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null || user.getProfilePhotoPath() == null) {
            return ResponseEntity.notFound().build();
        }

        Path photoPath = FileStorageConfig.resolvePath(user.getProfilePhotoPath());
        if (photoPath == null || !Files.exists(photoPath)) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] bytes = Files.readAllBytes(photoPath);
            MediaType mediaType = MediaType.IMAGE_JPEG;
            String lower = photoPath.toString().toLowerCase(Locale.ROOT);
            if (lower.endsWith(".png")) {
                mediaType = MediaType.IMAGE_PNG;
            } else if (lower.endsWith(".webp")) {
                mediaType = MediaType.parseMediaType("image/webp");
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/profile-photo")
    public ResponseEntity<Map<String, Object>> deleteProfilePhoto(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Authentication required"));
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "User not found"));
        }

        if (user.getProfilePhotoPath() != null) {
            try {
                Path photoPath = FileStorageConfig.resolvePath(user.getProfilePhotoPath());
                if (photoPath != null) {
                    Files.deleteIfExists(photoPath);
                }
            } catch (Exception ignored) {}

            user.setProfilePhotoPath(null);
            userRepository.save(user);
        }

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "Profile photo deleted successfully"));
    }

    public static class UserProfileResponse {

        private Long id;
        private String name;
        private String username;
        private String email;
        private Long storageLimit;
        private Long usedStorage;
        private Long fileCount;
        private Long encryptedCount;
        private LocalDateTime createdAt;
        private Map<String, Long> categoryBytes;
        private Map<String, Long> categoryCounts;
        private boolean profilePhotoAvailable;

        public UserProfileResponse() {
        }

        public UserProfileResponse(
                Long id,
                String name,
                String username,
                String email,
                Long storageLimit,
                Long usedStorage,
                Long fileCount,
                Long encryptedCount,
                LocalDateTime createdAt,
                Map<String, Long> categoryBytes,
                Map<String, Long> categoryCounts,
                boolean profilePhotoAvailable) {

            this.id = id;
            this.name = name;
            this.username = username;
            this.email = email;
            this.storageLimit = storageLimit;
            this.usedStorage = usedStorage;
            this.fileCount = fileCount;
            this.encryptedCount = encryptedCount;
            this.createdAt = createdAt;
            this.categoryBytes = categoryBytes;
            this.categoryCounts = categoryCounts;
            this.profilePhotoAvailable = profilePhotoAvailable;
        }

        public UserProfileResponse(
                Long id,
                String username,
                String email,
                Long storageLimit,
                Long usedStorage,
                Long fileCount,
                Long encryptedCount,
                LocalDateTime createdAt,
                Map<String, Long> categoryBytes,
                Map<String, Long> categoryCounts) {
            this(id, username, username, email, storageLimit, usedStorage, fileCount, encryptedCount, createdAt, categoryBytes, categoryCounts, false);
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }

        public Long getStorageLimit() {
            return storageLimit;
        }

        public Long getUsedStorage() {
            return usedStorage;
        }

        public Long getFileCount() {
            return fileCount;
        }

        public Long getEncryptedCount() {
            return encryptedCount;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public Map<String, Long> getCategoryBytes() {
            return categoryBytes;
        }

        public Map<String, Long> getCategoryCounts() {
            return categoryCounts;
        }

        public boolean isProfilePhotoAvailable() {
            return profilePhotoAvailable;
        }

        public boolean getProfilePhotoAvailable() {
            return profilePhotoAvailable;
        }

        public void setProfilePhotoAvailable(boolean profilePhotoAvailable) {
            this.profilePhotoAvailable = profilePhotoAvailable;
        }
    }
}