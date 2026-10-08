package com.ciphervault.app.auth.api;

import com.ciphervault.app.auth.model.LoginRequest;
import com.ciphervault.app.auth.model.LoginResponse;
import com.ciphervault.app.auth.model.RegisterRequest;
import com.ciphervault.app.auth.model.RegisterResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {

    @POST("api/auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/auth/logout")
    Call<Void> logout();
}
