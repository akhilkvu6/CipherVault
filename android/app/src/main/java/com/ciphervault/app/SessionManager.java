package com.ciphervault.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

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
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_BIOMETRIC = "biometric_enabled";
    public static final long DEFAULT_LIMIT = 10737418240L; // 10 GB in bytes (10 * 1024 * 1024 * 1024)

    private static volatile SharedPreferences sSecurePreferences;
    private static final Object INIT_LOCK = new Object();
    private static volatile String sCachedToken = null;
    private static volatile SessionManager sInstance;

    private final SharedPreferences preferences;

    public static SessionManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (INIT_LOCK) {
                if (sInstance == null) {
                    sInstance = new SessionManager(context != null ? context.getApplicationContext() : null);
                }
            }
        }
        return sInstance;
    }

    public SessionManager(Context context) {
        this.preferences = getOrCreatePreferences(context);
        if (sCachedToken == null && this.preferences != null) {
            sCachedToken = safelyGetString(this.preferences, KEY_TOKEN, null);
        }
    }

    private static SharedPreferences getOrCreatePreferences(Context context) {
        if (sSecurePreferences != null) {
            return sSecurePreferences;
        }
        if (context == null) {
            return null;
        }
        synchronized (INIT_LOCK) {
            if (sSecurePreferences != null) {
                return sSecurePreferences;
            }

            Context appContext = context.getApplicationContext();
            SharedPreferences securePref = null;
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
            } catch (Throwable e) {
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

                        legacyPref.edit()
                                .remove(KEY_TOKEN)
                                .remove(KEY_USERNAME)
                                .remove(KEY_EMAIL)
                                .apply();
                        Log.i(TAG, "Migrated legacy session into EncryptedSharedPreferences.");
                    }
                } catch (Throwable e) {
                    Log.w(TAG, "Legacy migration skipped: " + e.getMessage());
                }
            }
            sSecurePreferences = securePref;
            return sSecurePreferences;
        }
    }

    private static String safelyGetString(SharedPreferences prefs, String key, String defaultValue) {
        if (prefs == null) return defaultValue;
        try {
            Object val = prefs.getAll().get(key);
            if (val == null) return defaultValue;
            if (val instanceof String) return (String) val;
            return String.valueOf(val);
        } catch (Throwable e) {
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
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    public void saveLogin(String token, String username) {
        saveLogin(token, username, null);
    }

    public void saveLogin(String token, String username, String email) {
        saveLogin(token, username, email, username);
    }

    public void saveLogin(String token, String username, String email, String name) {
        sCachedToken = token;
        if (preferences == null) return;
        SharedPreferences.Editor editor = preferences.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USERNAME, username);
        if (name != null && !name.trim().isEmpty()) {
            editor.putString(KEY_NAME, name.trim());
        }
        if (email != null && !email.trim().isEmpty()) {
            editor.putString(KEY_EMAIL, email.trim());
        }
        editor.apply();
    }

    public void saveName(String name) {
        if (preferences == null) return;
        if (name != null && !name.trim().isEmpty()) {
            preferences.edit().putString(KEY_NAME, name.trim()).apply();
        }
    }

    public void saveUsername(String username) {
        if (preferences == null) return;
        if (username != null && !username.trim().isEmpty()) {
            preferences.edit().putString(KEY_USERNAME, username.trim()).apply();
        }
    }

    public void setBiometricEnabled(boolean enabled) {
        if (preferences == null) return;
        preferences.edit().putBoolean(KEY_BIOMETRIC, enabled).apply();
    }

    public boolean isBiometricEnabled() {
        if (preferences == null) return false;
        try {
            return preferences.getBoolean(KEY_BIOMETRIC, false);
        } catch (Throwable e) {
            return false;
        }
    }

    public String getName() {
        String saved = safelyGetString(preferences, KEY_NAME, null);
        if (saved != null && !saved.trim().isEmpty()) {
            return saved;
        }
        return getUsername() != null ? getUsername() : "User";
    }

    public void saveEmail(String email) {
        if (preferences == null) return;
        if (email != null && !email.trim().isEmpty()) {
            preferences.edit().putString(KEY_EMAIL, email.trim()).apply();
        }
    }

    public void saveAuthToken(String token) {
        sCachedToken = token;
        if (preferences == null || token == null) return;
        preferences.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        if (sCachedToken != null && !sCachedToken.isEmpty()) {
            return sCachedToken;
        }
        sCachedToken = safelyGetString(preferences, KEY_TOKEN, null);
        return sCachedToken;
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
            } catch (Throwable ignored) {}
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
        sCachedToken = null;
        if (preferences == null) return;
        try {
            preferences.edit().clear().apply();
        } catch (Throwable ignored) {}
    }

    public void clearSession() {
        logout();
    }
}