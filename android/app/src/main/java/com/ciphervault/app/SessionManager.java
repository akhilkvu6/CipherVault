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

    private static final String KEY_EMAIL = "email";

    public void saveLogin(String token, String username) {
        saveLogin(token, username, null);
    }

    public void saveLogin(String token, String username, String email) {
        SharedPreferences.Editor editor = preferences.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USERNAME, username);
        if (email != null && !email.trim().isEmpty()) {
            editor.putString(KEY_EMAIL, email.trim());
        }
        editor.apply();
    }

    public void saveEmail(String email) {
        if (email != null && !email.trim().isEmpty()) {
            preferences.edit().putString(KEY_EMAIL, email.trim()).apply();
        }
    }

    public String getToken() {
        return preferences.getString(KEY_TOKEN, null);
    }

    public String getUsername() {
        return preferences.getString(KEY_USERNAME, null);
    }

    public String getEmail() {
        String saved = preferences.getString(KEY_EMAIL, null);
        if (saved != null && !saved.trim().isEmpty()) {
            return saved;
        }
        String token = getToken();
        if (token != null && token.contains(".")) {
            try {
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    byte[] decodedBytes = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE);
                    String payloadJson = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
                    org.json.JSONObject obj = new org.json.JSONObject(payloadJson);
                    if (obj.has("sub")) {
                        return obj.getString("sub");
                    }
                }
            } catch (Exception ignored) {}
        }
        return "user@ciphervault.local";
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