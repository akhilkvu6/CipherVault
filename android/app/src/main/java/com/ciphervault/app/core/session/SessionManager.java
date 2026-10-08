package com.ciphervault.app.core.session;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ciphervault.app.core.security.KeyStoreCipher;

/**
 * Secure Session Manager (Sections 12, 13, 14).
 * Stores encrypted JWT via Android Keystore AES-GCM and non-secret user metadata.
 */
public class SessionManager {

    public static final String PREF_NAME = "ciphervault_secure_session";
    private static final String KEY_ENCRYPTED_TOKEN = "key_encrypted_token";
    private static final String KEY_USERNAME = "key_username";
    private static final String KEY_NAME = "key_name";

    private final SharedPreferences preferences;
    private final KeyStoreCipher cipher;
    private OnSessionInvalidatedListener sessionInvalidatedListener;

    public interface OnSessionInvalidatedListener {
        void onSessionInvalidated();
    }

    public SessionManager(@NonNull Context context) {
        this(
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE),
                new KeyStoreCipher()
        );
    }

    public SessionManager(@NonNull SharedPreferences preferences, @NonNull KeyStoreCipher cipher) {
        this.preferences = preferences;
        this.cipher = cipher;
    }

    public synchronized void saveSession(
            @NonNull String token,
            @Nullable String username,
            @Nullable String name
    ) {
        try {
            String encryptedToken = cipher.encrypt(token);
            preferences.edit()
                    .putString(KEY_ENCRYPTED_TOKEN, encryptedToken)
                    .putString(KEY_USERNAME, username != null ? username : "")
                    .putString(KEY_NAME, name != null ? name : "")
                    .apply();
        } catch (Exception e) {
            // Never log sensitive tokens
            clearSession();
        }
    }

    @Nullable
    public synchronized String getToken() {
        String encryptedToken = preferences.getString(KEY_ENCRYPTED_TOKEN, null);
        if (encryptedToken == null || encryptedToken.trim().isEmpty()) {
            return null;
        }
        try {
            return cipher.decrypt(encryptedToken);
        } catch (Exception e) {
            // Decryption failure invalidates corrupted session
            clearSession();
            return null;
        }
    }

    @Nullable
    public String getUsername() {
        return preferences.getString(KEY_USERNAME, null);
    }

    @Nullable
    public String getName() {
        return preferences.getString(KEY_NAME, null);
    }

    public boolean hasSession() {
        String encryptedToken = preferences.getString(KEY_ENCRYPTED_TOKEN, null);
        return encryptedToken != null && !encryptedToken.trim().isEmpty();
    }

    public synchronized void clearSession() {
        preferences.edit()
                .remove(KEY_ENCRYPTED_TOKEN)
                .remove(KEY_USERNAME)
                .remove(KEY_NAME)
                .apply();
    }

    public void setOnSessionInvalidatedListener(OnSessionInvalidatedListener listener) {
        this.sessionInvalidatedListener = listener;
    }

    public void notifySessionInvalidated() {
        clearSession();
        if (sessionInvalidatedListener != null) {
            sessionInvalidatedListener.onSessionInvalidated();
        }
    }
}
