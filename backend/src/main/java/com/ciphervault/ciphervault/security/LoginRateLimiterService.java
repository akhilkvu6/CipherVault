package com.ciphervault.ciphervault.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * In-memory login rate limiter that protects against brute-force attacks
 * by limiting consecutive failed login attempts per client IP and email pair.
 */
@Service
public class LoginRateLimiterService {

    private static final Logger log =
            LoggerFactory.getLogger(LoginRateLimiterService.class);

    public static final int MAX_FAILED_ATTEMPTS = 5;

    public static final long LOCKOUT_DURATION_MS =
            TimeUnit.MINUTES.toMillis(15);

    private static class AttemptTracker {
        int failedAttempts;
        long lockoutUntilTimestamp;
        long lastAttemptTimestamp;
    }

    private final ConcurrentHashMap<String, AttemptTracker> attemptsMap =
            new ConcurrentHashMap<>();

    private String buildKey(String clientIp, String email) {
        String safeIp =
                clientIp != null && !clientIp.isBlank()
                        ? clientIp.trim()
                        : "unknown-ip";

        String safeEmail =
                email != null && !email.isBlank()
                        ? email.trim().toLowerCase(Locale.ROOT)
                        : "unknown-user";

        return safeIp + ":" + safeEmail;
    }

    private boolean isStale(AttemptTracker tracker, long now) {
        if (tracker == null) {
            return true;
        }

        if (tracker.lockoutUntilTimestamp != 0L) {
            return tracker.lockoutUntilTimestamp <= now;
        }

        return (now - tracker.lastAttemptTimestamp) >= LOCKOUT_DURATION_MS;
    }

    /**
     * Purges expired lockout entries and inactive failed attempt records.
     */
    public void cleanupStaleEntries() {
        long now = System.currentTimeMillis();
        attemptsMap.entrySet().removeIf(entry -> isStale(entry.getValue(), now));
    }

    /**
     * Returns the number of currently tracked rate-limit keys.
     */
    public int getTrackedCount() {
        return attemptsMap.size();
    }

    /**
     * Checks whether the given IP and email combination is currently locked out.
     */
    public boolean isBlocked(String clientIp, String email) {
        String key = buildKey(clientIp, email);
        AttemptTracker tracker = attemptsMap.get(key);

        if (tracker == null) {
            return false;
        }

        long now = System.currentTimeMillis();

        if (tracker.lockoutUntilTimestamp > now) {
            log.warn(
                    "Login attempt blocked by rate limiter for key: {}. "
                            + "Locked out for another {}s",
                    key,
                    (tracker.lockoutUntilTimestamp - now) / 1000
            );

            return true;
        }

        if (isStale(tracker, now)) {
            attemptsMap.remove(key, tracker);
        }

        return false;
    }

    /**
     * Records a failed login attempt and starts a lockout after the limit is reached.
     */
    public void recordFailedAttempt(String clientIp, String email) {
        long now = System.currentTimeMillis();

        if (attemptsMap.size() >= 500) {
            cleanupStaleEntries();
        }

        String key = buildKey(clientIp, email);

        attemptsMap.compute(key, (k, tracker) -> {
            if (tracker == null) {
                tracker = new AttemptTracker();
            }

            if (isStale(tracker, now)) {
                tracker.failedAttempts = 0;
                tracker.lockoutUntilTimestamp = 0L;
            }

            tracker.failedAttempts++;
            tracker.lastAttemptTimestamp = now;

            if (tracker.failedAttempts >= MAX_FAILED_ATTEMPTS) {
                tracker.lockoutUntilTimestamp =
                        now + LOCKOUT_DURATION_MS;

                log.warn(
                        "Rate limit triggered: Lockout initiated for key {} "
                                + "({} failed attempts).",
                        key,
                        tracker.failedAttempts
                );
            }

            return tracker;
        });
    }

    /**
     * Clears failed login attempts after successful authentication.
     */
    public void recordSuccessfulLogin(String clientIp, String email) {
        String key = buildKey(clientIp, email);
        attemptsMap.remove(key);
    }

    /**
     * Returns the remaining lockout duration in seconds, or zero when unlocked.
     */
    public long getRemainingLockoutSeconds(
            String clientIp,
            String email) {

        String key = buildKey(clientIp, email);
        AttemptTracker tracker = attemptsMap.get(key);

        if (tracker == null) {
            return 0L;
        }

        long remainingMs =
                tracker.lockoutUntilTimestamp
                        - System.currentTimeMillis();

        return remainingMs > 0
                ? remainingMs / 1000
                : 0L;
    }
}