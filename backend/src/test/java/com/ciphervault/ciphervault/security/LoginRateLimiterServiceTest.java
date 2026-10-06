package com.ciphervault.ciphervault.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LoginRateLimiterServiceTest {

    private LoginRateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new LoginRateLimiterService();
    }

    @Test
    void testLockoutAfterMaxFailedAttempts() {
        String ip = "192.168.1.100";
        String email = "test@ciphervault.local";

        assertFalse(rateLimiterService.isBlocked(ip, email));

        for (int i = 0; i < LoginRateLimiterService.MAX_FAILED_ATTEMPTS - 1; i++) {
            rateLimiterService.recordFailedAttempt(ip, email);
            assertFalse(rateLimiterService.isBlocked(ip, email));
        }

        // Final failure triggering lockout
        rateLimiterService.recordFailedAttempt(ip, email);
        assertTrue(rateLimiterService.isBlocked(ip, email));

        long remaining = rateLimiterService.getRemainingLockoutSeconds(ip, email);
        assertTrue(remaining > 0 && remaining <= 900); // 15 minutes is 900 seconds
    }

    @Test
    void testFailedAttemptsBelowThresholdDoNotBlock() {
        String ip = "192.168.1.101";
        String email = "attempts@ciphervault.local";

        assertFalse(rateLimiterService.isBlocked(ip, email));
        rateLimiterService.recordFailedAttempt(ip, email);
        assertFalse(rateLimiterService.isBlocked(ip, email));
        rateLimiterService.recordFailedAttempt(ip, email);
        assertFalse(rateLimiterService.isBlocked(ip, email));
        assertEquals(1, rateLimiterService.getTrackedCount());
    }

    @Test
    void testSuccessfulLoginClearsFailedAttempts() {
        String ip = "10.0.0.1";
        String email = "user@ciphervault.local";

        rateLimiterService.recordFailedAttempt(ip, email);
        rateLimiterService.recordFailedAttempt(ip, email);

        rateLimiterService.recordSuccessfulLogin(ip, email);

        // Max attempts will not trigger lockout because it was reset
        for (int i = 0; i < LoginRateLimiterService.MAX_FAILED_ATTEMPTS - 1; i++) {
            rateLimiterService.recordFailedAttempt(ip, email);
        }

        assertFalse(rateLimiterService.isBlocked(ip, email));
    }

    @Test
    void testStaleEntryCleanup() {
        String activeIp = "192.168.1.1";
        String activeEmail = "active@ciphervault.local";

        String staleAttemptIp = "192.168.1.2";
        String staleAttemptEmail = "stale_attempt@ciphervault.local";

        String expiredLockoutIp = "192.168.1.3";
        String expiredLockoutEmail = "expired_lockout@ciphervault.local";

        // 1. Active lockout (should remain)
        for (int i = 0; i < LoginRateLimiterService.MAX_FAILED_ATTEMPTS; i++) {
            rateLimiterService.recordFailedAttempt(activeIp, activeEmail);
        }

        // 2. Inactive failed attempt (make stale via timestamp in the past)
        rateLimiterService.recordFailedAttempt(staleAttemptIp, staleAttemptEmail);

        // 3. Expired lockout (make expired via timestamp in the past)
        for (int i = 0; i < LoginRateLimiterService.MAX_FAILED_ATTEMPTS; i++) {
            rateLimiterService.recordFailedAttempt(expiredLockoutIp, expiredLockoutEmail);
        }

        @SuppressWarnings("unchecked")
        java.util.concurrent.ConcurrentHashMap<String, Object> map =
                (java.util.concurrent.ConcurrentHashMap<String, Object>)
                        org.springframework.test.util.ReflectionTestUtils.getField(rateLimiterService, "attemptsMap");
        assertNotNull(map);

        long now = System.currentTimeMillis();
        long twentyMinutesAgo = now - java.util.concurrent.TimeUnit.MINUTES.toMillis(20);

        Object staleAttemptTracker = map.get(staleAttemptIp + ":" + staleAttemptEmail);
        assertNotNull(staleAttemptTracker);
        org.springframework.test.util.ReflectionTestUtils.setField(staleAttemptTracker, "lastAttemptTimestamp", twentyMinutesAgo);

        Object expiredLockoutTracker = map.get(expiredLockoutIp + ":" + expiredLockoutEmail);
        assertNotNull(expiredLockoutTracker);
        org.springframework.test.util.ReflectionTestUtils.setField(expiredLockoutTracker, "lockoutUntilTimestamp", now - 1000);

        assertEquals(3, rateLimiterService.getTrackedCount());

        // Perform cleanup
        rateLimiterService.cleanupStaleEntries();

        // Expired lockout and stale inactive attempt should be removed; active lockout retained
        assertEquals(1, rateLimiterService.getTrackedCount());
        assertTrue(rateLimiterService.isBlocked(activeIp, activeEmail));
        assertFalse(rateLimiterService.isBlocked(staleAttemptIp, staleAttemptEmail));
        assertFalse(rateLimiterService.isBlocked(expiredLockoutIp, expiredLockoutEmail));
    }
}
