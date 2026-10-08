package com.ciphervault.app.motion;

/**
 * Centralized motion timing tokens (B1).
 * MICRO    = 100ms
 * FAST     = 160ms
 * STANDARD = 220ms
 * EMPHASIS = 280ms
 */
public final class MotionTokens {

    public static final long MICRO = 100L;
    public static final long FAST = 160L;
    public static final long STANDARD = 220L;
    public static final long EMPHASIS = 280L;

    public static final int TRANSLATION_DP_STANDARD = 24;

    private MotionTokens() {
        // Utility constant holder
    }
}
