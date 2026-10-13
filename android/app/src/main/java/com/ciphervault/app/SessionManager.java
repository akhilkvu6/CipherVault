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
    public static final String KEY_TOKEN = "token";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_NAME = "name";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_BIOMETRIC = "biometric_enabled";
    public static final String KEY_MIGRATION_COMPLETED = "legacy_session_migrated";
    public static final long DEFAULT_LIMIT = 10737418240L; // 10 GB in bytes (10 * 1024 * 1024 * 1024)

    private static volatile SharedPreferences sSecurePreferences;
    private static final Object INIT_LOCK = new Object();
    private static volatile String sCachedToken = null;
    private static volatile SessionManager sInstance;

    private final SharedPreferences preferences;
    private final Context appContext;

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
        this(context, getOrCreatePreferences(context));
    }

    // Constructor for testing with mock or specific SharedPreferences
    SessionManager(Context context, SharedPreferences prefs) {
        this.appContext = context != null ? context.getApplicationContext() : null;
        this.preferences = prefs;
        if (this.preferences != null) {
            sCachedToken = safelyGetString(this.preferences, KEY_TOKEN, null);
        } else {
            sCachedToken = null;
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
                    boolean alreadyMigrated = securePref.getBoolean(KEY_MIGRATION_COMPLETED, false);
                    SharedPreferences legacyPref = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

                    if (!alreadyMigrated && legacyPref.contains(KEY_TOKEN) && !securePref.contains(KEY_TOKEN)) {
                        String token = safelyGetString(legacyPref, KEY_TOKEN, null);
                        if (token != null && !token.trim().isEmpty() && !isTokenExpired(token)) {
                            String username = safelyGetString(legacyPref, KEY_USERNAME, null);
                            String email = safelyGetString(legacyPref, KEY_EMAIL, null);
                            String name = safelyGetString(legacyPref, KEY_NAME, null);

                            SharedPreferences.Editor editor = securePref.edit();
                            editor.putString(KEY_TOKEN, token);
                            if (username != null) editor.putString(KEY_USERNAME, username);
                            if (email != null) editor.putString(KEY_EMAIL, email);
                            if (name != null) editor.putString(KEY_NAME, name);
                            editor.putBoolean(KEY_MIGRATION_COMPLETED, true);
                            editor.commit();
                            Log.i(TAG, "Migrated valid legacy session into EncryptedSharedPreferences.");
                        } else {
                            securePref.edit().putBoolean(KEY_MIGRATION_COMPLETED, true).commit();
                        }
                    } else if (!alreadyMigrated) {
                        securePref.edit().putBoolean(KEY_MIGRATION_COMPLETED, true).commit();
                    }

                    // Always synchronously purge legacy preferences so old sessions cannot resurrect
                    if (legacyPref.contains(KEY_TOKEN) || legacyPref.contains(KEY_USERNAME)) {
                        legacyPref.edit()
                                .remove(KEY_TOKEN)
                                .remove(KEY_USERNAME)
                                .remove(KEY_EMAIL)
                                .remove(KEY_NAME)
                                .remove(KEY_BIOMETRIC)
                                .clear()
                                .commit();
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
        editor.commit();

        if (appContext != null) {
            try {
                appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .edit().clear().commit();
            } catch (Throwable ignored) {}
        }
    }

    public void saveName(String name) {
        if (preferences == null) return;
        if (name != null && !name.trim().isEmpty()) {
            preferences.edit().putString(KEY_NAME, name.trim()).commit();
        }
    }

    public void saveUsername(String username) {
        if (preferences == null) return;
        if (username != null && !username.trim().isEmpty()) {
            preferences.edit().putString(KEY_USERNAME, username.trim()).commit();
        }
    }

    public void setBiometricEnabled(boolean enabled) {
        if (preferences == null) return;
        preferences.edit().putBoolean(KEY_BIOMETRIC, enabled).commit();
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
            preferences.edit().putString(KEY_EMAIL, email.trim()).commit();
        }
    }

    public void saveAuthToken(String token) {
        sCachedToken = token;
        if (preferences == null || token == null) return;
        preferences.edit().putString(KEY_TOKEN, token).commit();
    }

    public String getToken() {
        if (sCachedToken != null && !sCachedToken.trim().isEmpty()) {
            return sCachedToken;
        }
        String token = safelyGetString(preferences, KEY_TOKEN, null);
        if (token != null && token.trim().isEmpty()) {
            token = null;
        }
        sCachedToken = token;
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
                    byte[] decodedBytes = decodeBase64Url(parts[1]);
                    if (decodedBytes != null && decodedBytes.length > 0) {
                        String payloadJson = new String(decodedBytes, StandardCharsets.UTF_8);
                        try {
                            com.google.gson.JsonObject obj = new com.google.gson.Gson().fromJson(payloadJson, com.google.gson.JsonObject.class);
                            if (obj != null && obj.has("sub") && !obj.get("sub").isJsonNull()) {
                                return obj.get("sub").getAsString();
                            }
                        } catch (Throwable gsonEx) {
                            JSONObject obj = new JSONObject(payloadJson);
                            if (obj.has("sub")) {
                                return obj.getString("sub");
                            }
                        }
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
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        if (isTokenExpired(token)) {
            Log.i(TAG, "Stored session token is expired. Purging session.");
            logout();
            return false;
        }
        return true;
    }

    public void logout() {
        sCachedToken = null;
        if (appContext != null) {
            AuditLogger.AuditEntry logoutEntry = new AuditLogger.AuditEntry(AuditLogger.ACTION_LOGOUT, "SUCCESS", "User session ended / signed out", null);
            AuditLogger.logEvent(appContext, logoutEntry);
        }
        if (preferences != null) {
            try {
                SharedPreferences.Editor editor = preferences.edit();
                editor.remove(KEY_TOKEN)
                        .remove(KEY_USERNAME)
                        .remove(KEY_NAME)
                        .remove(KEY_EMAIL)
                        .remove(KEY_BIOMETRIC);
                try {
                    java.util.Map<String, ?> all = preferences.getAll();
                    if (all != null) {
                        for (String k : all.keySet()) {
                            if (!KEY_MIGRATION_COMPLETED.equals(k)) {
                                editor.remove(k);
                            }
                        }
                    }
                } catch (Throwable ignored) {}
                editor.clear();
                editor.putBoolean(KEY_MIGRATION_COMPLETED, true);
                editor.commit();
            } catch (Throwable ignored) {}
        }
        if (appContext != null) {
            try {
                SharedPreferences legacyPref = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                legacyPref.edit()
                        .remove(KEY_TOKEN)
                        .remove(KEY_USERNAME)
                        .remove(KEY_NAME)
                        .remove(KEY_EMAIL)
                        .remove(KEY_BIOMETRIC)
                        .clear()
                        .commit();
            } catch (Throwable ignored) {}

            try {
                SharedPreferences secPref = appContext.getSharedPreferences("ciphervault_secure_session", Context.MODE_PRIVATE);
                secPref.edit().clear().commit();
            } catch (Throwable ignored) {}
        }
        synchronized (INIT_LOCK) {
            sCachedToken = null;
            sInstance = null;
        }
    }

    public void clearSession() {
        logout();
    }

    public static boolean isTokenExpired(String token) {
        if (token == null || token.trim().isEmpty()) {
            return true;
        }
        if (!token.contains(".")) {
            // Non-JWT token (e.g. test mock) - treat as non-expired
            return false;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return true;
            }
            byte[] decodedBytes = decodeBase64Url(parts[1]);
            if (decodedBytes == null || decodedBytes.length == 0) {
                return true;
            }
            String payloadJson = new String(decodedBytes, StandardCharsets.UTF_8);
            try {
                com.google.gson.JsonObject obj = new com.google.gson.Gson().fromJson(payloadJson, com.google.gson.JsonObject.class);
                if (obj != null && obj.has("exp") && !obj.get("exp").isJsonNull()) {
                    long expSec = obj.get("exp").getAsLong();
                    long nowSec = System.currentTimeMillis() / 1000L;
                    return nowSec >= expSec;
                }
            } catch (Throwable gsonEx) {
                JSONObject obj = new JSONObject(payloadJson);
                if (obj.has("exp")) {
                    long expSec = obj.getLong("exp");
                    long nowSec = System.currentTimeMillis() / 1000L;
                    return nowSec >= expSec;
                }
            }
            return false;
        } catch (Throwable t) {
            Log.w(TAG, "Failed to parse token expiration: " + t.getMessage());
            return true;
        }
    }

    private static byte[] decodeBase64Url(String input) {
        if (input == null || input.trim().isEmpty()) return new byte[0];
        try {
            return java.util.Base64.getUrlDecoder().decode(input.trim());
        } catch (Throwable t1) {
            try {
                return Base64.decode(input.trim(), Base64.URL_SAFE);
            } catch (Throwable t2) {
                return new byte[0];
            }
        }
    }
}