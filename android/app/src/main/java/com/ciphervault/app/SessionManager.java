package com.ciphervault.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class SessionManager {

    private static final String TAG = "SessionManager";
    private static final String PREF_NAME = "CipherVaultSession";
    private static final String ENCRYPTED_PREF_NAME = "CipherVaultEncryptedSession";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    public static final long DEFAULT_LIMIT = 1073741824L; // 1 GB in bytes (1024 * 1024 * 1024)

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        SharedPreferences securePref = null;
        if (context != null) {
            Context appContext = context.getApplicationContext();
            try {
                MasterKey masterKey = new MasterKey.Builder(appContext)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();

                securePref = EncryptedSharedPreferences.create(
                        appContext,
                        ENCRYPTED_PREF_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
            } catch (java.security.GeneralSecurityException | java.io.IOException | SecurityException e) {
                Log.w(TAG, "EncryptedSharedPreferences unavailable, falling back to standard SharedPreferences: " + e.getMessage());
            }

            if (securePref == null) {
                securePref = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            } else {
                try {
                    SharedPreferences legacyPref = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                    if (legacyPref.contains(KEY_TOKEN) && !securePref.contains(KEY_TOKEN)) {
                        String token = safelyGetString(legacyPref, KEY_TOKEN, null);
                        String username = safelyGetString(legacyPref, KEY_USERNAME, null);
                        String email = safelyGetString(legacyPref, KEY_EMAIL, null);

                        SharedPreferences.Editor editor = securePref.edit();
                        if (token != null) editor.putString(KEY_TOKEN, token);
                        if (username != null) editor.putString(KEY_USERNAME, username);
                        if (email != null) editor.putString(KEY_EMAIL, email);
                        editor.apply();

                        // Safely remove only migrated keys, preserving any other keys
                        legacyPref.edit()
                                .remove(KEY_TOKEN)
                                .remove(KEY_USERNAME)
                                .remove(KEY_EMAIL)
                                .apply();
                        Log.i(TAG, "Migrated legacy session into EncryptedSharedPreferences.");
                    }
                } catch (ClassCastException | SecurityException e) {
                    Log.w(TAG, "Legacy migration skipped due to format: " + e.getMessage());
                }
            }
        }
        this.preferences = securePref != null ? securePref : (context != null ? context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) : null);
    }

    private static String safelyGetString(SharedPreferences prefs, String key, String defaultValue) {
        if (prefs == null) return defaultValue;
        try {
            Object val = prefs.getAll().get(key);
            if (val == null) return defaultValue;
            if (val instanceof String) return (String) val;
            return String.valueOf(val);
        } catch (ClassCastException | NullPointerException e) {
            return defaultValue;
        }
    }

    private static int safelyGetInt(SharedPreferences prefs, String key, int defaultValue) {
        if (prefs == null) return defaultValue;
        try {
            Object val = prefs.getAll().get(key);
            if (val == null) return defaultValue;
            if (val instanceof Integer) return (Integer) val;
            if (val instanceof Number) return ((Number) val).intValue();
            if (val instanceof String) return Integer.parseInt((String) val);
            return defaultValue;
        } catch (NumberFormatException | ClassCastException | NullPointerException e) {
            return defaultValue;
        }
    }

    public void saveLogin(String token, String username) {
        saveLogin(token, username, null);
    }

    public void saveLogin(String token, String username, String email) {
        if (preferences == null) return;
        SharedPreferences.Editor editor = preferences.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USERNAME, username);
        if (email != null && !email.trim().isEmpty()) {
            editor.putString(KEY_EMAIL, email.trim());
        }
        editor.apply();
    }

    public void saveEmail(String email) {
        if (preferences == null) return;
        if (email != null && !email.trim().isEmpty()) {
            preferences.edit().putString(KEY_EMAIL, email.trim()).apply();
        }
    }

    public void saveAuthToken(String token) {
        if (preferences == null || token == null) return;
        preferences.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return safelyGetString(preferences, KEY_TOKEN, null);
    }

    public String getUsername() {
        return safelyGetString(preferences, KEY_USERNAME, null);
    }

    public String getEmail() {
        if (preferences == null) return "user@ciphervault.local";
        String saved = safelyGetString(preferences, KEY_EMAIL, null);

        if (saved != null && !saved.trim().isEmpty()) {
            return saved;
        }
        String token = getToken();
        if (token != null && token.contains(".")) {
            try {
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    byte[] decodedBytes = Base64.decode(parts[1], Base64.URL_SAFE);
                    String payloadJson = new String(decodedBytes, StandardCharsets.UTF_8);
                    JSONObject obj = new JSONObject(payloadJson);
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

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.isEmpty();
    }

    public void logout() {
        if (preferences == null) return;
        try {
            preferences.edit().clear().apply();
        } catch (Throwable ignored) {}
    }
}