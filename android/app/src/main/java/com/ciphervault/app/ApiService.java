package com.ciphervault.app;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {

    @GET("api/health")
    Call<Map<String, Object>> checkHealth();
}