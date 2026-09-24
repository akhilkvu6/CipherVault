package com.ciphervault.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class SessionManager {

    private static final String PREF_NAME = "CipherVaultSession";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";
    public static final long DEFAULT_LIMIT = 1073741824L; // 1 GB in bytes (1024 * 1024 * 1024)

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    public void saveLogin(String token, String username) {
        preferences.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USERNAME, username)
                .apply();
    }

    public String getToken() {
        return preferences.getString(KEY_TOKEN, null);
    }

    public String getUsername() {
        return preferences.getString(KEY_USERNAME, null);
    }

    public long getStorageLimit() {
        return DEFAULT_LIMIT;
    }

    public void saveThemeMode(int mode) {
        preferences.edit().putInt(KEY_THEME_MODE, mode).apply();
    }

    public int getThemeMode() {
        return preferences.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public void setOnboardingCompleted(boolean completed) {
        preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply();
    }

    public boolean isOnboardingCompleted() {
        return preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    public boolean isLoggedIn() {
        return getToken() != null && !getToken().isEmpty();
    }

    public void logout() {
        preferences.edit().clear().apply();
    }
}