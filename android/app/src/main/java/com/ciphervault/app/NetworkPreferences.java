package com.ciphervault.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Pattern;

import okhttp3.HttpUrl;

/**
 * Single authoritative abstraction for network preferences and server URL persistence (Section 3).
 * Persists only the non-sensitive server base URL in SharedPreferences.
 * Sensitive data (tokens, keys, passwords) must never be stored here.
 */
public class NetworkPreferences {

    public static final String PREF_NAME = "CipherVaultNetwork";
    public static final String KEY_SERVER_URL = "server_base_url";
    public static final String DEFAULT_BASE_URL = "http://10.0.2.2:8080/";

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])$"
    );

    private final SharedPreferences preferences;

    public NetworkPreferences(@NonNull Context context) {
        this.preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public NetworkPreferences(@NonNull SharedPreferences preferences) {
        this.preferences = preferences;
    }

    @NonNull
    public String getSavedBaseUrl() {
        String saved = preferences.getString(KEY_SERVER_URL, null);
        if (saved == null || saved.trim().isEmpty()) {
            return DEFAULT_BASE_URL;
        }
        return normalizeUrl(saved);
    }

    public boolean hasSavedBaseUrl() {
        return preferences.contains(KEY_SERVER_URL);
    }

    public void saveBaseUrl(@NonNull String url) {
        String normalized = normalizeUrl(url);
        preferences.edit().putString(KEY_SERVER_URL, normalized).apply();
    }

    public void clearBaseUrl() {
        preferences.edit().remove(KEY_SERVER_URL).apply();
    }

    /**
     * Validates an IPv4 address according to RFC 791 (Section 11).
     * Rejects invalid octets (e.g. 999.999.999.999), incomplete subnets (e.g. 192.168.1),
     * extra octets (192.168.1.1.5), malformed characters (abc.def.1.1), or consecutive dots (192.168..1).
     */
    public static boolean isValidIpv4(@Nullable String ip) {
        if (ip == null || ip.trim().isEmpty()) return false;
        return IPV4_PATTERN.matcher(ip.trim()).matches();
    }

    /**
     * Validates a network port (1–65535).
     */
    public static boolean isValidPort(int port) {
        return port >= 1 && port <= 65535;
    }

    /**
     * Normalizes server URLs:
     * - Trims whitespace
     * - Adds http:// if no scheme is provided
     * - Ensures exactly one trailing slash
     * - Forces http for private/local IP ranges
     */
    @NonNull
    public static String normalizeUrl(@Nullable String rawUrl) {
        if (rawUrl == null) return DEFAULT_BASE_URL;
        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty()) return DEFAULT_BASE_URL;

        if (trimmed.startsWith("https://127.0.0.1") || trimmed.startsWith("https://localhost")
                || trimmed.startsWith("https://10.") || trimmed.startsWith("https://192.168.")
                || trimmed.startsWith("https://172.")) {
            trimmed = "http://" + trimmed.substring(8);
        } else if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "http://" + trimmed;
        }

        while (trimmed.endsWith("//")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        if (!trimmed.endsWith("/")) {
            trimmed = trimmed + "/";
        }

        HttpUrl parsed = HttpUrl.parse(trimmed);
        if (parsed == null) {
            return DEFAULT_BASE_URL;
        }
        return parsed.toString();
    }
}
