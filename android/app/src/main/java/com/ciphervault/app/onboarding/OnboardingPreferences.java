package com.ciphervault.app.onboarding;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Handles persistent storage for onboarding state using SharedPreferences.
 * Does not store sensitive data such as passwords, JWTs, or encryption keys.
 */
public final class OnboardingPreferences {

    private static final String PREF_NAME = "ciphervault_prefs";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";

    private final SharedPreferences preferences;

    public OnboardingPreferences(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isOnboardingCompleted() {
        return preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    public void setOnboardingCompleted(boolean completed) {
        preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply();
    }

    public void clear() {
        preferences.edit().clear().apply();
    }
}
