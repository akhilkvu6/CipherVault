package com.ciphervault.app.core.network;

import androidx.annotation.NonNull;

import com.ciphervault.app.auth.api.AuthApi;
import com.ciphervault.app.auth.api.HealthApi;
import com.ciphervault.app.core.session.SessionManager;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Centralized Retrofit/OkHttp API client (Sections 11, 31).
 * Manages authorization header attachment, 401 session invalidation, and base URL updates.
 */
public class ApiClient {

    private static final int TIMEOUT_SECONDS = 15;

    private final ServerConnectionPreferences serverPreferences;
    private final SessionManager sessionManager;

    private Retrofit retrofit;
    private String currentBaseUrl;

    public ApiClient(
            @NonNull ServerConnectionPreferences serverPreferences,
            @NonNull SessionManager sessionManager
    ) {
        this.serverPreferences = serverPreferences;
        this.sessionManager = sessionManager;
        rebuildClient();
    }

    public synchronized void rebuildClient() {
        String baseUrl = serverPreferences.getServerUrl();
        this.currentBaseUrl = baseUrl;

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor())
                .build();

        this.retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @NonNull
    public synchronized Retrofit getRetrofit() {
        String latestUrl = serverPreferences.getServerUrl();
        if (!latestUrl.equals(currentBaseUrl) || retrofit == null) {
            rebuildClient();
        }
        return retrofit;
    }

    public AuthApi getAuthApi() {
        return getRetrofit().create(AuthApi.class);
    }

    public HealthApi getHealthApi() {
        return getRetrofit().create(HealthApi.class);
    }

    /**
     * Builds a standalone HealthApi targeting a specific candidate URL for connection testing.
     */
    public HealthApi createHealthApiForUrl(@NonNull String url) {
        String normalized = ServerConnectionPreferences.normalizeUrl(url);
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();

        Retrofit customRetrofit = new Retrofit.Builder()
                .baseUrl(normalized)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return customRetrofit.create(HealthApi.class);
    }

    private class AuthInterceptor implements Interceptor {
        @NonNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Request request = chain.request();
            String path = request.url().encodedPath();

            boolean isPublicAuthEndpoint = path.contains("/api/auth/login")
                    || path.contains("/api/auth/register")
                    || path.contains("/api/health");

            Request.Builder requestBuilder = request.newBuilder();

            if (!isPublicAuthEndpoint) {
                String token = sessionManager.getToken();
                if (token != null && !token.trim().isEmpty()) {
                    requestBuilder.header("Authorization", "Bearer " + token);
                }
            }

            Response response = chain.proceed(requestBuilder.build());

            // Section 11: 401 handling on authenticated endpoints
            if (response.code() == 401 && !isPublicAuthEndpoint) {
                sessionManager.notifySessionInvalidated();
            }

            return response;
        }
    }
}
