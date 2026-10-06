package com.ciphervault.app.core.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecureTokenStorage {
    private static final String PREFS_NAME = "ciphervault_secure_auth";
    private static final String KEY_JWT = "jwt_token";
    private static final String KEY_USERNAME = "username";

    private SharedPreferences encryptedPrefs;

    public SecureTokenStorage(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            encryptedPrefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.e("SecureTokenStorage", "Failed to initialize EncryptedSharedPreferences", e);
            // Fallback to normal shared prefs if crypto fails (e.g., on some devices/emulators)
            encryptedPrefs = context.getSharedPreferences("ciphervault_auth_fallback", Context.MODE_PRIVATE);
        }
    }

    public void saveSession(String token, String username) {
        encryptedPrefs.edit()
                .putString(KEY_JWT, token)
                .putString(KEY_USERNAME, username)
                .apply();
    }

    public String getToken() {
        return encryptedPrefs.getString(KEY_JWT, null);
    }
    
    public String getUsername() {
        return encryptedPrefs.getString(KEY_USERNAME, null);
    }

    public void clearSession() {
        encryptedPrefs.edit().clear().apply();
    }
}
