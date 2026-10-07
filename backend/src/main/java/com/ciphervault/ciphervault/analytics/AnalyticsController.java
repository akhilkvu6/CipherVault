package com.ciphervault.ciphervault.analytics;

import com.ciphervault.ciphervault.activity.ActivityEvent;
import com.ciphervault.ciphervault.activity.ActivityEventRepository;
import com.ciphervault.ciphervault.activity.EventType;
import com.ciphervault.ciphervault.file.FileCategoryService;
import com.ciphervault.ciphervault.file.FileRepository;
import com.ciphervault.ciphervault.file.StoredFile;
import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import com.ciphervault.ciphervault.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final ActivityEventRepository activityRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FileCategoryService fileCategoryService;

    @Autowired
    public AnalyticsController(
            ActivityEventRepository activityRepository,
            FileRepository fileRepository,
            UserRepository userRepository,
            @Autowired(required = false) FileCategoryService fileCategoryService) {
        this.activityRepository = activityRepository;
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.fileCategoryService = fileCategoryService != null ? fileCategoryService : new FileCategoryService();
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required"));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAnalytics(
            @RequestParam(defaultValue = "today") String period,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        LocalDateTime since;
        boolean hourly = false;
        boolean isAllTime = false;

        String normPeriod = period != null ? period.trim().toLowerCase(Locale.ROOT) : "today";
        switch (normPeriod) {
            case "today":
                since = LocalDateTime.now().toLocalDate().atStartOfDay();
                hourly = true;
                break;
            case "week":
                since = LocalDateTime.now().minusDays(7).toLocalDate().atStartOfDay();
                break;
            case "month":
                since = LocalDateTime.now().minusDays(30).toLocalDate().atStartOfDay();
                break;
            case "all":
            case "alltime":
                since = LocalDateTime.of(2000, 1, 1, 0, 0);
                isAllTime = true;
                break;
            default:
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", "Supported periods: today, week, month, all");
        }

        long uploadCount = activityRepository.countEventsSince(user, EventType.UPLOAD, since);
        long downloadCount = activityRepository.countEventsSince(user, EventType.DOWNLOAD, since);
        long deleteCount = activityRepository.countEventsSince(user, EventType.DELETE, since);

        Long uploadBytes = activityRepository.sumBytesSince(user, EventType.UPLOAD, since);
        Long downloadBytes = activityRepository.sumBytesSince(user, EventType.DOWNLOAD, since);

        Long filesAdded = activityRepository.sumFilesSince(user, EventType.UPLOAD, since);
        Long filesDeleted = activityRepository.sumFilesSince(user, EventType.DELETE, since);

        Long storageAdded = uploadBytes != null ? uploadBytes : 0L;
        Long storageRemoved = activityRepository.sumBytesSince(user, EventType.DELETE, since);
        if (storageRemoved == null) storageRemoved = 0L;

        long storageGrowth = storageAdded - storageRemoved;

        // Accurate category distribution using FileCategoryService
        List<Object[]> categoryStats = fileRepository.findFileStatsByUser(user);
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (String cat : fileCategoryService.getStandardCategories()) {
            distribution.put(cat, 0L);
        }

        for (Object[] row : categoryStats) {
            String originalFilename = (String) row[0];
            String contentType = (String) row[1];
            String category = fileCategoryService.determineCategory(originalFilename, contentType);
            distribution.put(category, distribution.getOrDefault(category, 0L) + 1L);
        }

        // Vault-wide authoritative metrics
        long totalFiles = fileRepository.countByUser(user);
        long totalStorage = user.getUsedStorage() != null ? user.getUsedStorage() : 0L;
        long storageLimit = user.getStorageLimit() != null ? user.getStorageLimit() : 1_000_000_000L;

        // Duplicate calculation
        List<StoredFile> userFiles = fileRepository.findByUser(user);
        Map<String, List<StoredFile>> groupedByHash = userFiles.stream()
                .filter(f -> f.getSha256Hash() != null && !f.getSha256Hash().isBlank())
                .collect(Collectors.groupingBy(f -> f.getSha256Hash().toLowerCase(Locale.ROOT)));

        long duplicateCount = 0L;
        long duplicateSavings = 0L;
        for (List<StoredFile> group : groupedByHash.values()) {
            if (group.size() > 1) {
                long groupSize = group.get(0).getFileSize() != null ? group.get(0).getFileSize() : 0L;
                duplicateCount += (group.size() - 1);
                duplicateSavings += groupSize * (group.size() - 1);
            }
        }

        // Large files calculation (>= 50 MB)
        long largeFileThreshold = 50L * 1024L * 1024L;
        long largeFileCount = userFiles.stream()
                .filter(f -> f.getFileSize() != null && f.getFileSize() >= largeFileThreshold)
                .count();

        Map<String, Object> timeSeries = buildTimeSeries(user, since, hourly, isAllTime);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("uploads", uploadCount);
        result.put("downloads", downloadCount);
        result.put("deletions", deleteCount);
        result.put("uploadedBytes", uploadBytes != null ? uploadBytes : 0L);
        result.put("downloadedBytes", downloadBytes != null ? downloadBytes : 0L);
        result.put("filesAdded", filesAdded != null ? filesAdded : 0L);
        result.put("filesDeleted", filesDeleted != null ? filesDeleted : 0L);
        result.put("storageAdded", storageAdded);
        result.put("storageRemoved", storageRemoved);
        result.put("storageGrowth", storageGrowth);
        result.put("totalFiles", totalFiles);
        result.put("totalStorage", totalStorage);
        result.put("storageLimit", storageLimit);
        result.put("duplicateCount", duplicateCount);
        result.put("duplicateSavings", duplicateSavings);
        result.put("largeFileCount", largeFileCount);
        result.put("fileTypeDistribution", distribution);
        result.put("timeSeries", timeSeries);

        return ResponseEntity.ok(result);
    }

    private Map<String, Object> buildTimeSeries(User user, LocalDateTime since, boolean hourly, boolean isAllTime) {
        List<EventType> types = Arrays.asList(EventType.UPLOAD, EventType.DOWNLOAD, EventType.DELETE);
        LocalDateTime effectiveSince = isAllTime ? LocalDateTime.now().minusDays(30).toLocalDate().atStartOfDay() : since;
        List<ActivityEvent> events = activityRepository.findEventsSince(user, types, effectiveSince);

        Map<String, Map<String, Long>> buckets = new TreeMap<>(); // Label -> {metric -> value}

        DateTimeFormatter formatter = hourly ? DateTimeFormatter.ofPattern("HH:00") : DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Initialize buckets
        LocalDateTime current = effectiveSince;
        LocalDateTime now = LocalDateTime.now();
        while (!current.isAfter(now)) {
            String label = current.format(formatter);
            buckets.putIfAbsent(label, new HashMap<>(Map.of(
                    "uploads", 0L, "downloads", 0L, "storageAdded", 0L, "storageRemoved", 0L
            )));
            if (hourly) {
                current = current.plusHours(1);
            } else {
                current = current.plusDays(1);
            }
        }

        for (ActivityEvent e : events) {
            String label = e.getTimestamp().format(formatter);
            Map<String, Long> metrics = buckets.get(label);
            if (metrics == null) continue;

            long bytes = e.getTotalBytes() != null ? e.getTotalBytes() : 0L;
            if (e.getEventType() == EventType.UPLOAD) {
                metrics.put("uploads", metrics.get("uploads") + 1);
                metrics.put("storageAdded", metrics.get("storageAdded") + bytes);
            } else if (e.getEventType() == EventType.DOWNLOAD) {
                metrics.put("downloads", metrics.get("downloads") + 1);
            } else if (e.getEventType() == EventType.DELETE) {
                metrics.put("storageRemoved", metrics.get("storageRemoved") + bytes);
            }
        }

        // Add growth
        for (Map<String, Long> metrics : buckets.values()) {
            metrics.put("storageGrowth", metrics.get("storageAdded") - metrics.get("storageRemoved"));
        }

        return Map.of(
                "labels", buckets.keySet(),
                "data", buckets.values()
        );
    }
}
