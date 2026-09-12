package com.ciphervault.app;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "CipherVaultSession";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USERNAME = "username";
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

    public boolean isLoggedIn() {
        return getToken() != null && !getToken().isEmpty();
    }

    public void logout() {
        preferences.edit().clear().apply();
    }
}