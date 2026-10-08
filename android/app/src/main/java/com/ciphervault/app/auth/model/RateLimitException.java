package com.ciphervault.app.auth.model;

/**
 * Exception thrown when server returns HTTP 429 Too Many Requests (Section 10).
 */
public class RateLimitException extends Exception {

    private final int retryAfterSeconds;

    public RateLimitException(int retryAfterSeconds) {
        super("Too many attempts. Please try again in " + retryAfterSeconds + " seconds.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
