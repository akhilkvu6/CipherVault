package com.ciphervault.app.core.network;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.URI;

/**
 * Preferences abstraction for non-sensitive server connection URL (Sections 24, 25).
 */
public class ServerConnectionPreferences {

    public static final String PREF_NAME = "ciphervault_server_preferences";
    public static final String KEY_SERVER_URL = "key_server_url";
    public static final String DEFAULT_SERVER_URL = "http://10.0.2.2:8080/";

    private final SharedPreferences preferences;

    public ServerConnectionPreferences(@NonNull Context context) {
        this(context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE));
    }

    public ServerConnectionPreferences(@NonNull SharedPreferences preferences) {
        this.preferences = preferences;
    }

    @NonNull
    public String getServerUrl() {
        String saved = preferences.getString(KEY_SERVER_URL, null);
        if (saved == null || saved.trim().isEmpty()) {
            return DEFAULT_SERVER_URL;
        }
        return normalizeUrl(saved);
    }

    public void setServerUrl(@Nullable String url) {
        if (url == null || url.trim().isEmpty()) {
            preferences.edit().remove(KEY_SERVER_URL).apply();
            return;
        }
        String normalized = normalizeUrl(url);
        preferences.edit().putString(KEY_SERVER_URL, normalized).apply();
    }

    /**
     * Normalizes server URLs (Section 24):
     * - Trims whitespace
     * - Adds http:// if no scheme is provided
     * - Ensures exactly one trailing slash
     */
    @NonNull
    public static String normalizeUrl(@NonNull String rawUrl) {
        if (rawUrl == null) {
            return DEFAULT_SERVER_URL;
        }
        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT_SERVER_URL;
        }

        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "http://" + trimmed;
        }

        // Remove multiple trailing slashes and ensure exactly one
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        return trimmed + "/";
    }

    public static boolean isValidUrl(@Nullable String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        try {
            String normalized = normalizeUrl(url);
            URI uri = URI.create(normalized);
            return uri.getHost() != null && !uri.getHost().trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
