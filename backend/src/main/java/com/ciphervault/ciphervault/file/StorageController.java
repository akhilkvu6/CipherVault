package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
@RequestMapping("/api/storage")
public class StorageController {

    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FileCategoryService fileCategoryService;

    public StorageController(UserRepository userRepository, FileRepository fileRepository, FileCategoryService fileCategoryService) {
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
        this.fileCategoryService = fileCategoryService;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) throw new com.ciphervault.ciphervault.exception.ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");

        long totalBytes = user.getStorageLimit() != null ? user.getStorageLimit() : 1_000_000_000L;
        long usedBytes = user.getUsedStorage() != null ? user.getUsedStorage() : 0L;
        long availableBytes = Math.max(0, totalBytes - usedBytes);
        double percentage = totalBytes > 0 ? ((double) usedBytes / totalBytes) * 100 : 0.0;
        
        long fileCount = fileRepository.countByUser(user);

        Map<String, Object> response = new HashMap<>();
        response.put("totalBytes", totalBytes);
        response.put("usedBytes", usedBytes);
        response.put("availableBytes", availableBytes);
        response.put("usagePercentage", percentage);
        response.put("percentage", percentage);
        response.put("fileCount", fileCount);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, Object>>> getCategories(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) throw new com.ciphervault.ciphervault.exception.ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");

        List<Object[]> stats = fileRepository.findFileStatsByUser(user);
        Map<String, Long> categoryBytes = new HashMap<>();
        Map<String, Long> categoryCounts = new HashMap<>();
        
        long totalBytes = 0;

        for (Object[] row : stats) {
            String originalFilename = (String) row[0];
            String contentType = (String) row[1];
            Long size = (Long) row[2];
            
            String category = fileCategoryService.determineCategory(originalFilename, contentType);
            long fileSize = size != null ? size : 0L;
            totalBytes += fileSize;

            categoryBytes.put(category, categoryBytes.getOrDefault(category, 0L) + fileSize);
            categoryCounts.put(category, categoryCounts.getOrDefault(category, 0L) + 1L);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (String category : fileCategoryService.getStandardCategories()) {
            long bytes = categoryBytes.getOrDefault(category, 0L);
            long count = categoryCounts.getOrDefault(category, 0L);
            double percentage = totalBytes > 0 ? ((double) bytes / totalBytes) * 100 : 0.0;
            
            Map<String, Object> map = new HashMap<>();
            map.put("category", category);
            map.put("fileCount", count);
            map.put("totalBytes", bytes);
            map.put("percentage", percentage);
            result.add(map);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/duplicates")
    public ResponseEntity<List<Map<String, Object>>> getDuplicates(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) throw new com.ciphervault.ciphervault.exception.ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");

        // The database schema enforces a unique constraint on (user_id, sha256_hash),
        // meaning structural duplicates are currently impossible.
        // Returning an empty list to preserve the Android API contract.
        List<Map<String, Object>> result = new ArrayList<>();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/large-files")
    public ResponseEntity<List<Map<String, Object>>> getLargeFiles(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) throw new com.ciphervault.ciphervault.exception.ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");

        long minSizeBytes = 100L * 1024L * 1024L; // 100 MB
        List<StoredFile> largeFiles = fileRepository.findByUserAndFileSizeGreaterThanEqualOrderByFileSizeDesc(user, minSizeBytes, PageRequest.of(0, 100)).getContent();
        List<Map<String, Object>> result = largeFiles.stream().map(f -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", f.getId());
            map.put("filename", f.getOriginalFilename());
            map.put("fileSize", f.getFileSize());
            map.put("size", f.getFileSize());
            map.put("contentType", f.getContentType());
            map.put("mimeType", f.getContentType());
            map.put("encrypted", f.isEncrypted());
            map.put("sha256Hash", f.getSha256Hash());
            map.put("createdAt", f.getCreatedAt() != null ? f.getCreatedAt().toString() : null);
            map.put("uploadedAt", f.getCreatedAt() != null ? f.getCreatedAt().toString() : null);
            map.put("hasPreview", f.isHasPreview());
            map.put("category", fileCategoryService.determineCategory(f.getOriginalFilename(), f.getContentType()));
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
