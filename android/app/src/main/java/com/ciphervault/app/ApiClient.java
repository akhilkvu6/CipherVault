package com.ciphervault.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String PREF_NAME = "CipherVaultNetwork";
    private static final String KEY_SERVER_URL = "server_base_url";
    /**
     * Default fallback URL for local development / loopback.
     */
    public static final String DEFAULT_DEV_URL = "http://127.0.0.1:8080/";

    /**
     * Clear production server URL configuration.
     * When accessing CipherVault across the internet via Cloudflare Tunnel,
     * traffic must always travel over secure HTTPS.
     */
    public static final String DEFAULT_PROD_URL = "https://vault.ciphervault.local/";

    private static final String DEFAULT_URL = DEFAULT_DEV_URL;

    private static Retrofit retrofit;
    private static String currentBaseUrl;

    public static String sanitizeAndValidateUrl(String rawUrl) {
        if (rawUrl == null) return DEFAULT_URL;
        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty()) return DEFAULT_URL;

        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            // Automatically select HTTPS for internet domains (e.g., trycloudflare.com or custom domain)
            if (trimmed.startsWith("127.0.0.1") || trimmed.startsWith("localhost") || 
                trimmed.startsWith("10.0.") || trimmed.startsWith("192.168.")) {
                trimmed = "http://" + trimmed;
            } else {
                trimmed = "https://" + trimmed;
            }
        }
        if (!trimmed.endsWith("/")) {
            trimmed = trimmed + "/";
        }

        okhttp3.HttpUrl parsed = okhttp3.HttpUrl.parse(trimmed);
        if (parsed == null) {
            return DEFAULT_URL;
        }
        return parsed.toString();
    }

    public static synchronized void setBaseUrl(Context context, String rawUrl) {
        String formatted = sanitizeAndValidateUrl(rawUrl);

        if (context != null) {
            try {
                SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                prefs.edit().putString(KEY_SERVER_URL, formatted).apply();
            } catch (Exception ignored) {}
        }

        currentBaseUrl = formatted;
        retrofit = null;
    }

    public static synchronized String getBaseUrl(Context context) {
        if (currentBaseUrl != null && !currentBaseUrl.trim().isEmpty()) {
            return sanitizeAndValidateUrl(currentBaseUrl);
        }
        if (context != null) {
            try {
                SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                Object val = prefs.getAll().get(KEY_SERVER_URL);
                if (val instanceof String) {
                    currentBaseUrl = (String) val;
                } else if (val != null) {
                    currentBaseUrl = String.valueOf(val);
                } else {
                    currentBaseUrl = DEFAULT_URL;
                }
            } catch (Exception t) {
                currentBaseUrl = DEFAULT_URL;
            }
        }
        currentBaseUrl = sanitizeAndValidateUrl(currentBaseUrl);
        return currentBaseUrl;
    }

    public static synchronized Retrofit getRetrofit(Context context) {
        String baseUrl = getBaseUrl(context);

        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(180, TimeUnit.SECONDS)
                    .writeTimeout(180, TimeUnit.SECONDS)
                    .addInterceptor(new AuthInterceptor(context))
                    .build();

            try {
                retrofit = new Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
            } catch (IllegalArgumentException e) {
                // If baseUrl somehow failed Retrofit validation, reset persisted setting and recover with DEFAULT_URL
                if (context != null) {
                    try {
                        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                        prefs.edit().putString(KEY_SERVER_URL, DEFAULT_URL).apply();
                    } catch (Exception ignored) {}
                }
                currentBaseUrl = DEFAULT_URL;
                retrofit = new Retrofit.Builder()
                        .baseUrl(DEFAULT_URL)
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
            }
        }

        return retrofit;
    }

    public static ApiService getApiService(Context context) {
        return getRetrofit(context).create(ApiService.class);
    }
}