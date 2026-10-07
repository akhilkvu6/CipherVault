package com.ciphervault.ciphervault.analytics;

import com.ciphervault.ciphervault.activity.ActivityEventRepository;
import com.ciphervault.ciphervault.activity.EventType;
import com.ciphervault.ciphervault.file.FileCategoryService;
import com.ciphervault.ciphervault.file.FileRepository;
import com.ciphervault.ciphervault.file.StoredFile;
import com.ciphervault.ciphervault.user.User;
import com.ciphervault.ciphervault.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class AnalyticsControllerTest {

    private ActivityEventRepository activityRepository;
    private FileRepository fileRepository;
    private UserRepository userRepository;
    private AnalyticsController analyticsController;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        activityRepository = Mockito.mock(ActivityEventRepository.class);
        fileRepository = Mockito.mock(FileRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        analyticsController = new AnalyticsController(
                activityRepository,
                fileRepository,
                userRepository,
                new FileCategoryService()
        );

        authentication = Mockito.mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("bob@example.com");
    }

    @Test
    void getAnalyticsTodayShouldReturnAccurateMetrics() {
        User user = new User();
        user.setId(2L);
        user.setEmail("bob@example.com");
        user.setUsedStorage(50_000_000L);
        user.setStorageLimit(1_000_000_000L);

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));
        when(activityRepository.countEventsSince(eq(user), eq(EventType.UPLOAD), any())).thenReturn(5L);
        when(activityRepository.countEventsSince(eq(user), eq(EventType.DOWNLOAD), any())).thenReturn(2L);
        when(activityRepository.countEventsSince(eq(user), eq(EventType.DELETE), any())).thenReturn(1L);
        when(activityRepository.sumBytesSince(eq(user), eq(EventType.UPLOAD), any())).thenReturn(20_000_000L);
        when(activityRepository.sumBytesSince(eq(user), eq(EventType.DELETE), any())).thenReturn(5_000_000L);
        when(fileRepository.countByUser(user)).thenReturn(10L);

        List<Object[]> stats = new ArrayList<>();
        stats.add(new Object[]{"photo.jpg", "image/jpeg", 10_000_000L, true});
        stats.add(new Object[]{"contract.pdf", "application/pdf", 2_000_000L, true});
        when(fileRepository.findFileStatsByUser(user)).thenReturn(stats);

        ResponseEntity<Map<String, Object>> response = analyticsController.getAnalytics("today", authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5L, response.getBody().get("uploads"));
        assertEquals(2L, response.getBody().get("downloads"));
        assertEquals(1L, response.getBody().get("deletions"));
        assertEquals(15_000_000L, response.getBody().get("storageGrowth"));
        assertEquals(10L, response.getBody().get("totalFiles"));
        assertEquals(50_000_000L, response.getBody().get("totalStorage"));

        @SuppressWarnings("unchecked")
        Map<String, Long> dist = (Map<String, Long>) response.getBody().get("fileTypeDistribution");
        assertNotNull(dist);
        assertEquals(1L, dist.get("Images"));
        assertEquals(1L, dist.get("Documents"));
    }

    @Test
    void getAnalyticsAllTimeShouldReturnDuplicatesAndLargeFiles() {
        User user = new User();
        user.setId(2L);
        user.setEmail("bob@example.com");
        user.setUsedStorage(120_000_000L);
        user.setStorageLimit(1_000_000_000L);

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));
        when(fileRepository.countByUser(user)).thenReturn(3L);

        StoredFile f1 = new StoredFile();
        f1.setId(10L);
        f1.setSha256Hash("hash-abc");
        f1.setFileSize(60_000_000L); // >= 50MB

        StoredFile f2 = new StoredFile();
        f2.setId(11L);
        f2.setSha256Hash("hash-abc"); // duplicate!
        f2.setFileSize(60_000_000L);

        StoredFile f3 = new StoredFile();
        f3.setId(12L);
        f3.setSha256Hash("hash-unique");
        f3.setFileSize(1_000_000L);

        when(fileRepository.findByUser(user)).thenReturn(List.of(f1, f2, f3));

        ResponseEntity<Map<String, Object>> response = analyticsController.getAnalytics("all", authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().get("duplicateCount"));
        assertEquals(60_000_000L, response.getBody().get("duplicateSavings"));
        assertEquals(2L, response.getBody().get("largeFileCount"));
    }
}
