package com.ciphervault.app.auth.api;

import com.ciphervault.app.auth.model.HealthResponse;

import retrofit2.Call;
import retrofit2.http.GET;

public interface HealthApi {

    @GET("api/health")
    Call<HealthResponse> getHealth();
}
