package com.ciphervault.app.auth.data;

import android.content.Context;

import com.ciphervault.app.auth.api.AuthApi;
import com.ciphervault.app.auth.api.HealthApi;
import com.ciphervault.app.auth.model.HealthResponse;
import com.ciphervault.app.auth.model.LoginRequest;
import com.ciphervault.app.auth.model.LoginResponse;
import com.ciphervault.app.auth.model.RegisterRequest;
import com.ciphervault.app.auth.model.RegisterResponse;
import com.ciphervault.app.core.network.ApiClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private final Context context;

    public AuthRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public interface RepoCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    private <T> Callback<T> createCallback(RepoCallback<T> callback) {
        return new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    String errorMsg = "Server error";
                    try {
                        if (response.errorBody() != null) {
                            String errString = response.errorBody().string();
                            JsonObject errObj = new Gson().fromJson(errString, JsonObject.class);
                            if (errObj != null && errObj.has("message")) {
                                errorMsg = errObj.get("message").getAsString();
                            } else if (errString != null && !errString.isEmpty()) {
                                errorMsg = errString; // fallback
                            }
                        } else {
                            errorMsg = "HTTP " + response.code();
                        }
                    } catch (Exception e) {
                        // ignore parsing error
                    }
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                if (t instanceof IOException) {
                    callback.onError("Network error. Please check your connection.");
                } else {
                    callback.onError("An unexpected error occurred.");
                }
            }
        };
    }

    public void checkHealth(RepoCallback<HealthResponse> callback) {
        if (ApiClient.getClient(context) == null) {
            callback.onError("Server URL not configured.");
            return;
        }
        HealthApi api = ApiClient.getClient(context).create(HealthApi.class);
        api.getHealth().enqueue(createCallback(callback));
    }

    public void login(String email, String password, RepoCallback<LoginResponse> callback) {
        if (ApiClient.getClient(context) == null) {
            callback.onError("Server URL not configured.");
            return;
        }
        AuthApi api = ApiClient.getClient(context).create(AuthApi.class);
        api.login(new LoginRequest(email, password)).enqueue(createCallback(callback));
    }

    public void register(String username, String email, String password, RepoCallback<RegisterResponse> callback) {
        if (ApiClient.getClient(context) == null) {
            callback.onError("Server URL not configured.");
            return;
        }
        AuthApi api = ApiClient.getClient(context).create(AuthApi.class);
        api.register(new RegisterRequest(username, email, password)).enqueue(createCallback(callback));
    }
}
