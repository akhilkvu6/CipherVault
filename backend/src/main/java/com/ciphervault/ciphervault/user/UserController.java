package com.ciphervault.ciphervault.user;

import com.ciphervault.ciphervault.activity.ActivityEventRepository;
import com.ciphervault.ciphervault.activity.ActivityService;
import com.ciphervault.ciphervault.activity.EventType;
import com.ciphervault.ciphervault.file.FileCategoryService;
import com.ciphervault.ciphervault.file.FileRepository;
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

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        UserProfileResponse response = new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getStorageLimit(),
                user.getUsedStorage(),
                fileCount,
                encryptedCount,
                user.getCreatedAt(),
                categoryBytes,
                categoryCounts
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

    @RequestMapping(value = {"/data", "/delete-all-data"}, method = {RequestMethod.DELETE, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> deleteAllData(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Authentication required"));
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "User not found"));
        }

        if (fileRepository != null) {
            List<StoredFile> files = fileRepository.findByUser(user);
            for (StoredFile f : files) {
                try {
                    if (fileStorageService != null) {
                        fileStorageService.deleteFile(user, f.getId());
                    } else {
                        fileRepository.delete(f);
                    }
                } catch (Exception e) {
                    fileRepository.delete(f);
                }
            }
        }

        if (transferRepository != null) {
            try {
                transferRepository.deleteByUser(user);
            } catch (Exception ignored) {}
        }

        if (activityEventRepository != null) {
            try {
                activityEventRepository.deleteByUser(user);
            } catch (Exception ignored) {}
        }

        user.setUsedStorage(0L);
        userRepository.save(user);

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

        return ResponseEntity.ok(
                Map.of("success", true, "message", "All data deleted successfully"));
    }

    @RequestMapping(value = {"/account", "/me", "/delete-account"}, method = {RequestMethod.DELETE, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> deleteAccount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Authentication required"));
        }

        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "User not found"));
        }

        if (fileRepository != null) {
            List<StoredFile> files = fileRepository.findByUser(user);
            for (StoredFile f : files) {
                try {
                    if (fileStorageService != null) {
                        fileStorageService.deleteFile(user, f.getId());
                    } else {
                        fileRepository.delete(f);
                    }
                } catch (Exception e) {
                    fileRepository.delete(f);
                }
            }
        }

        if (transferRepository != null) {
            try {
                transferRepository.deleteByUser(user);
            } catch (Exception ignored) {}
        }

        if (activityEventRepository != null) {
            try {
                activityEventRepository.deleteByUser(user);
            } catch (Exception ignored) {}
        }

        userRepository.delete(user);

        return ResponseEntity.ok(
                Map.of("success", true, "message", "Account deleted successfully"));
    }

    public static class UserProfileResponse {

        private Long id;
        private String username;
        private String email;
        private Long storageLimit;
        private Long usedStorage;
        private Long fileCount;
        private Long encryptedCount;
        private LocalDateTime createdAt;
        private Map<String, Long> categoryBytes;
        private Map<String, Long> categoryCounts;

        public UserProfileResponse() {
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

            this.id = id;
            this.username = username;
            this.email = email;
            this.storageLimit = storageLimit;
            this.usedStorage = usedStorage;
            this.fileCount = fileCount;
            this.encryptedCount = encryptedCount;
            this.createdAt = createdAt;
            this.categoryBytes = categoryBytes;
            this.categoryCounts = categoryCounts;
        }

        public Long getId() {
            return id;
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
    }
}