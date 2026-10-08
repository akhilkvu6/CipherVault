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

    private static final String DEFAULT_URL = DEFAULT_DEV_URL;

    private static Retrofit retrofit;
    private static String currentBaseUrl;

    public static String sanitizeAndValidateUrl(String rawUrl) {
        if (rawUrl == null) return DEFAULT_URL;
        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty()) return DEFAULT_URL;

        // Force HTTP for local development loopback and private IP addresses
        if (trimmed.startsWith("https://127.0.0.1") || trimmed.startsWith("https://localhost")
                || trimmed.startsWith("https://10.") || trimmed.startsWith("https://192.168.")
                || trimmed.startsWith("https://172.")) {
            trimmed = "http://" + trimmed.substring(8);
        } else if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "http://" + trimmed;
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
            new NetworkPreferences(context).saveBaseUrl(formatted);
        }

        currentBaseUrl = formatted;
        retrofit = null;
    }

    public static synchronized String getBaseUrl(Context context) {
        if (currentBaseUrl != null && !currentBaseUrl.trim().isEmpty()) {
            return sanitizeAndValidateUrl(currentBaseUrl);
        }
        if (context != null) {
            currentBaseUrl = new NetworkPreferences(context).getSavedBaseUrl();
        } else {
            currentBaseUrl = DEFAULT_URL;
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
                    new NetworkPreferences(context).saveBaseUrl(DEFAULT_URL);
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

    public static boolean isServerSaved(Context context) {
        if (context == null) return false;
        return new NetworkPreferences(context).hasSavedBaseUrl();
    }

    public interface HealthCallback {
        void onResult(boolean isOnline, long latencyMs);
    }

    public static void checkHealthFast(Context context, HealthCallback callback) {
        String baseUrl = getBaseUrl(context);
        OkHttpClient fastClient = new OkHttpClient.Builder()
                .connectTimeout(1800, TimeUnit.MILLISECONDS)
                .readTimeout(1800, TimeUnit.MILLISECONDS)
                .build();

        Retrofit fastRetrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(fastClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        long startTime = System.currentTimeMillis();
        fastRetrofit.create(ApiService.class).checkHealth().enqueue(new retrofit2.Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(@androidx.annotation.NonNull retrofit2.Call<java.util.Map<String, Object>> call,
                                   @androidx.annotation.NonNull retrofit2.Response<java.util.Map<String, Object>> response) {
                long latency = System.currentTimeMillis() - startTime;
                if (callback != null) {
                    callback.onResult(response.isSuccessful(), latency);
                }
            }

            @Override
            public void onFailure(@androidx.annotation.NonNull retrofit2.Call<java.util.Map<String, Object>> call,
                                  @androidx.annotation.NonNull Throwable t) {
                long latency = System.currentTimeMillis() - startTime;
                if (callback != null) {
                    callback.onResult(false, latency);
                }
            }
        });
    }
}