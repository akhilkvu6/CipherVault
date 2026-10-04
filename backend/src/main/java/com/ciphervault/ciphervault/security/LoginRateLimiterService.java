package com.ciphervault.ciphervault.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Production-appropriate in-memory login rate limiter.
 * Protects against brute-force attacks by limiting consecutive failed login attempts
 * per (client IP + email) pair.
 */
@Service
public class LoginRateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiterService.class);

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final long LOCKOUT_DURATION_MS = TimeUnit.MINUTES.toMillis(15); // 15 minutes

    private static class AttemptTracker {
        int failedAttempts;
        long lastAttemptTimestamp;
        long lockoutUntilTimestamp;

        AttemptTracker() {
            this.failedAttempts = 0;
            this.lastAttemptTimestamp = System.currentTimeMillis();
            this.lockoutUntilTimestamp = 0L;
        }
    }

    private final ConcurrentHashMap<String, AttemptTracker> attemptsMap = new ConcurrentHashMap<>();

    private String buildKey(String clientIp, String email) {
        String safeIp = (clientIp != null && !clientIp.isBlank()) ? clientIp.trim() : "unknown-ip";
        String safeEmail = (email != null && !email.isBlank()) ? email.trim().toLowerCase() : "unknown-user";
        return safeIp + ":" + safeEmail;
    }

    /**
     * Checks if the given IP/email combination is currently locked out.
     */
    public boolean isBlocked(String clientIp, String email) {
        String key = buildKey(clientIp, email);
        AttemptTracker tracker = attemptsMap.get(key);
        if (tracker == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (tracker.lockoutUntilTimestamp > now) {
            log.warn("Login attempt blocked by rate limiter for key: {}. Locked out for another {}s",
                    key, (tracker.lockoutUntilTimestamp - now) / 1000);
            return true;
        }

        // Lockout expired; reset tracker
        if (tracker.lockoutUntilTimestamp != 0L && tracker.lockoutUntilTimestamp <= now) {
            attemptsMap.remove(key);
        }

        return false;
    }

    /**
     * Records a failed login attempt. If failed attempts reach MAX_FAILED_ATTEMPTS,
     * triggers a 15-minute lockout.
     */
    public void recordFailedAttempt(String clientIp, String email) {
        String key = buildKey(clientIp, email);
        long now = System.currentTimeMillis();

        attemptsMap.compute(key, (k, tracker) -> {
            if (tracker == null) {
                tracker = new AttemptTracker();
            }

            // If previously expired, reset
            if (tracker.lockoutUntilTimestamp != 0L && tracker.lockoutUntilTimestamp <= now) {
                tracker.failedAttempts = 0;
                tracker.lockoutUntilTimestamp = 0L;
            }

            tracker.failedAttempts++;
            tracker.lastAttemptTimestamp = now;

            if (tracker.failedAttempts >= MAX_FAILED_ATTEMPTS) {
                tracker.lockoutUntilTimestamp = now + LOCKOUT_DURATION_MS;
                log.warn("Rate limit triggered: Lockout initiated for key {} ({} failed attempts).",
                        key, tracker.failedAttempts);
            }

            return tracker;
        });
    }

    /**
     * Clears failed attempt counters upon successful authentication.
     */
    public void recordSuccessfulLogin(String clientIp, String email) {
        String key = buildKey(clientIp, email);
        attemptsMap.remove(key);
    }

    /**
     * Returns remaining lockout seconds, or 0 if not locked out.
     */
    public long getRemainingLockoutSeconds(String clientIp, String email) {
        String key = buildKey(clientIp, email);
        AttemptTracker tracker = attemptsMap.get(key);
        if (tracker == null || tracker.lockoutUntilTimestamp <= System.currentTimeMillis()) {
            return 0L;
        }
        return (tracker.lockoutUntilTimestamp - System.currentTimeMillis()) / 1000;
    }

    /**
     * Visible for testing.
     */
    public void reset() {
        attemptsMap.clear();
    }
}
