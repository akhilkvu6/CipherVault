package com.ciphervault.app.main.api;

import com.ciphervault.app.main.model.UserProfileResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PUT;

public interface UserApi {
    @GET("/api/user/profile")
    Call<UserProfileResponse> getUserProfile();

    @PUT("/api/user/username")
    Call<Map<String, Object>> updateUsername(@Body Map<String, String> request);

    @DELETE("/api/user/data")
    Call<Map<String, Object>> deleteAllData();

    @DELETE("/api/user/account")
    Call<Map<String, Object>> deleteAccount();
}