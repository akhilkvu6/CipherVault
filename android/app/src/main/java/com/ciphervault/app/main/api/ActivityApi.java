package com.ciphervault.app.main.api;

import com.ciphervault.app.main.model.ActivityEvent;
import com.ciphervault.app.main.model.PageResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ActivityApi {
    @GET("/api/activity")
    Call<PageResponse<ActivityEvent>> listActivity(
            @Query("page") int page,
            @Query("size") int size,
            @Query("type") String type,
            @Query("period") String period
    );
}