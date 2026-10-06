package com.ciphervault.ciphervault.analytics;

import com.ciphervault.ciphervault.activity.ActivityEventRepository;
import com.ciphervault.ciphervault.activity.EventType;
import com.ciphervault.ciphervault.activity.ActivityEvent;
import com.ciphervault.ciphervault.file.FileRepository;
import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import com.ciphervault.ciphervault.exception.ApiException;
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

    public AnalyticsController(ActivityEventRepository activityRepository, FileRepository fileRepository, UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
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
        
        switch (period.toLowerCase()) {
            case "today": since = LocalDateTime.now().toLocalDate().atStartOfDay(); hourly = true; break;
            case "week": since = LocalDateTime.now().minusDays(7).toLocalDate().atStartOfDay(); break;
            case "month": since = LocalDateTime.now().minusDays(30).toLocalDate().atStartOfDay(); break;
            default: throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", "Supported periods: today, week, month");
        }

        long uploadCount = activityRepository.countEventsSince(user, EventType.UPLOAD, since);
        long downloadCount = activityRepository.countEventsSince(user, EventType.DOWNLOAD, since);
        
        Long uploadBytes = activityRepository.sumBytesSince(user, EventType.UPLOAD, since);
        Long downloadBytes = activityRepository.sumBytesSince(user, EventType.DOWNLOAD, since);
        
        Long filesAdded = activityRepository.sumFilesSince(user, EventType.UPLOAD, since);
        Long filesDeleted = activityRepository.sumFilesSince(user, EventType.DELETE, since);
        
        Long storageAdded = uploadBytes != null ? uploadBytes : 0L;
        Long storageRemoved = activityRepository.sumBytesSince(user, EventType.DELETE, since);
        if (storageRemoved == null) storageRemoved = 0L;
        
        long storageGrowth = storageAdded - storageRemoved;

        List<Object[]> categoryStats = fileRepository.findFileStatsByUser(user);
        Map<String, Object> distribution = new HashMap<>();
        for (Object[] row : categoryStats) {
            distribution.put((String) row[0], row[2]); // category name -> count
        }

        Map<String, Object> timeSeries = buildTimeSeries(user, since, hourly);

                Map<String, Object> result = new HashMap<>();
        result.put("uploads", uploadCount);
        result.put("downloads", downloadCount);
        result.put("uploadedBytes", uploadBytes != null ? uploadBytes : 0L);
        result.put("downloadedBytes", downloadBytes != null ? downloadBytes : 0L);
        result.put("filesAdded", filesAdded != null ? filesAdded : 0L);
        result.put("filesDeleted", filesDeleted != null ? filesDeleted : 0L);
        result.put("storageAdded", storageAdded);
        result.put("storageRemoved", storageRemoved);
        result.put("storageGrowth", storageGrowth);
        result.put("fileTypeDistribution", distribution);
        result.put("timeSeries", timeSeries);

        return ResponseEntity.ok(result);
    }

    private Map<String, Object> buildTimeSeries(User user, LocalDateTime since, boolean hourly) {
        List<EventType> types = Arrays.asList(EventType.UPLOAD, EventType.DOWNLOAD, EventType.DELETE);
        List<ActivityEvent> events = activityRepository.findEventsSince(user, types, since);

        Map<String, Map<String, Long>> buckets = new TreeMap<>(); // Label -> {metric -> value}

        DateTimeFormatter formatter = hourly ? DateTimeFormatter.ofPattern("HH:00") : DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Initialize buckets
        LocalDateTime current = since;
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

