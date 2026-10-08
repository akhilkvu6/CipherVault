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

        long totalBytes = user.getStorageLimit() != null ? user.getStorageLimit() : (10L * 1024L * 1024L * 1024L);
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

        List<StoredFile> userFiles = fileRepository.findByUser(user);
        Map<String, List<StoredFile>> groupedByHash = userFiles.stream()
                .filter(f -> f.getSha256Hash() != null && !f.getSha256Hash().isBlank())
                .collect(Collectors.groupingBy(f -> f.getSha256Hash().toLowerCase(java.util.Locale.ROOT)));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<StoredFile>> entry : groupedByHash.entrySet()) {
            List<StoredFile> duplicates = entry.getValue();
            if (duplicates.size() > 1) {
                long groupSize = duplicates.get(0).getFileSize() != null ? duplicates.get(0).getFileSize() : 0L;
                long count = duplicates.size();
                long totalOccupied = groupSize * count;
                long potentialSaving = groupSize * (count - 1);

                Map<String, Object> group = new HashMap<>();
                group.put("hash", entry.getKey());
                group.put("fileSize", groupSize);
                group.put("fileCount", count);
                group.put("totalOccupied", totalOccupied);
                group.put("potentialSaving", potentialSaving);

                List<Map<String, Object>> fileList = duplicates.stream().map(f -> {
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

                group.put("files", fileList);
                result.add(group);
            }
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/large-files")
    public ResponseEntity<List<Map<String, Object>>> getLargeFiles(
            @org.springframework.web.bind.annotation.RequestParam(value = "minSize", required = false) Long minSize,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) throw new com.ciphervault.ciphervault.exception.ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");

        long threshold = (minSize != null && minSize > 0) ? minSize : 50L * 1024L * 1024L; // 50 MB threshold
        List<StoredFile> largeFiles = fileRepository.findByUserAndFileSizeGreaterThanEqualOrderByFileSizeDesc(user, threshold, PageRequest.of(0, 100)).getContent();
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

    public ResponseEntity<List<Map<String, Object>>> getLargeFiles(Authentication authentication) {
        return getLargeFiles(null, authentication);
    }
}
