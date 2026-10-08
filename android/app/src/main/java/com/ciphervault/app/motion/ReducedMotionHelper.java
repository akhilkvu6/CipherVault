package com.ciphervault.app.motion;

import android.content.Context;
import android.provider.Settings;

import androidx.annotation.Nullable;

/**
 * Reusable reduced-motion decision mechanism for navigation motion (B6).
 * Ensures safety across lifecycle, handles null Context, and supports deterministic testing.
 */
public final class ReducedMotionHelper {

    private static Boolean testOverride = null;

    private ReducedMotionHelper() {
        // Utility class
    }

    /**
     * Determines whether reduced motion should be applied based on system animation scale.
     */
    public static boolean isReducedMotionEnabled(@Nullable Context context) {
        if (testOverride != null) {
            return testOverride;
        }
        if (context == null) {
            return false;
        }
        try {
            float durationScale = Settings.Global.getFloat(
                    context.getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1.0f
            );
            return shouldReduceMotion(durationScale);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Deterministic pure decision logic: 0.0 duration scale indicates animations are disabled/reduced.
     */
    public static boolean shouldReduceMotion(float animatorDurationScale) {
        return animatorDurationScale == 0.0f;
    }

    /**
     * Sets an override value for deterministic unit testing without Android context.
     */
    public static void setTestOverride(@Nullable Boolean override) {
        testOverride = override;
    }
}
