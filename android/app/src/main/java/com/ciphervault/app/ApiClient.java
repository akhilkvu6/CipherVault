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
    private static final String DEFAULT_URL = "http://127.0.0.1:8080/";

    private static Retrofit retrofit;
    private static String currentBaseUrl;

    public static synchronized void setBaseUrl(Context context, String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return;
        }

        String formatted = rawUrl.trim();
        if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
            formatted = "http://" + formatted;
        }
        if (!formatted.endsWith("/")) {
            formatted = formatted + "/";
        }

        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SERVER_URL, formatted).apply();

        currentBaseUrl = formatted;
        retrofit = null;
    }

    public static synchronized String getBaseUrl(Context context) {
        if (currentBaseUrl != null) {
            return currentBaseUrl;
        }
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        currentBaseUrl = prefs.getString(KEY_SERVER_URL, DEFAULT_URL);
        return currentBaseUrl;
    }

    public static synchronized Retrofit getRetrofit(Context context) {
        String baseUrl = getBaseUrl(context);

        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor(new AuthInterceptor(context))
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit;
    }

    public static ApiService getApiService(Context context) {
        return getRetrofit(context).create(ApiService.class);
    }
}