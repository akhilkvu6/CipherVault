package com.ciphervault.app.auth.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ciphervault.app.auth.api.AuthApi;
import com.ciphervault.app.auth.api.HealthApi;
import com.ciphervault.app.auth.model.HealthResponse;
import com.ciphervault.app.auth.model.LoginRequest;
import com.ciphervault.app.auth.model.LoginResponse;
import com.ciphervault.app.auth.model.RegisterRequest;
import com.ciphervault.app.auth.model.RegisterResponse;
import com.ciphervault.app.auth.validation.AuthErrorParser;
import com.ciphervault.app.core.network.ApiClient;
import com.ciphervault.app.core.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Authentication repository executing network calls and managing session state (Sections 9, 32).
 */
public class AuthRepository {

    private final ApiClient apiClient;
    private final SessionManager sessionManager;

    public interface ResultCallback<T> {
        void onSuccess(T result);
        void onError(String message, @Nullable Integer retryAfterSeconds);
    }

    public AuthRepository(@NonNull ApiClient apiClient, @NonNull SessionManager sessionManager) {
        this.apiClient = apiClient;
        this.sessionManager = sessionManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public void login(
            @NonNull String email,
            @NonNull String password,
            @NonNull ResultCallback<LoginResponse> callback
    ) {
        AuthApi api = apiClient.getAuthApi();
        api.login(new LoginRequest(email, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse body = response.body();
                    if (body.getToken() != null && !body.getToken().trim().isEmpty()) {
                        sessionManager.saveSession(body.getToken(), body.getUsername(), body.getName());
                        callback.onSuccess(body);
                        return;
                    }
                }

                if (response.code() == 429) {
                    int retryAfter = AuthErrorParser.parseRetryAfter(response);
                    callback.onError(AuthErrorParser.parseError(response), retryAfter);
                } else {
                    callback.onError(AuthErrorParser.parseError(response), null);
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                callback.onError(AuthErrorParser.parseException(t), null);
            }
        });
    }

    public void register(
            @NonNull String name,
            @NonNull String username,
            @NonNull String email,
            @NonNull String password,
            @NonNull ResultCallback<RegisterResponse> callback
    ) {
        AuthApi api = apiClient.getAuthApi();
        api.register(new RegisterRequest(name, username, email, password)).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Registration does NOT establish a session (Section 7)
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(AuthErrorParser.parseError(response), null);
                }
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                callback.onError(AuthErrorParser.parseException(t), null);
            }
        });
    }

    /**
     * Best-effort server logout with immediate local session clearing (Section 9).
     */
    public void logout(@Nullable ResultCallback<Void> callback) {
        // Immediate local session clearance
        sessionManager.clearSession();

        // Best effort server invalidation
        try {
            apiClient.getAuthApi().logout().enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    // Local session is already cleared; safe to report success
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                }
            });
        } catch (Exception e) {
            if (callback != null) {
                callback.onSuccess(null);
            }
        }
    }

    public void checkHealth(@Nullable String candidateUrl, @NonNull ResultCallback<Boolean> callback) {
        HealthApi healthApi = candidateUrl != null && !candidateUrl.trim().isEmpty()
                ? apiClient.createHealthApiForUrl(candidateUrl)
                : apiClient.getHealthApi();

        healthApi.getHealth().enqueue(new Callback<HealthResponse>() {
            @Override
            public void onResponse(@NonNull Call<HealthResponse> call, @NonNull Response<HealthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean isUp = response.body().isUp();
                    callback.onSuccess(isUp);
                } else {
                    callback.onSuccess(false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<HealthResponse> call, @NonNull Throwable t) {
                callback.onSuccess(false);
            }
        });
    }
}
