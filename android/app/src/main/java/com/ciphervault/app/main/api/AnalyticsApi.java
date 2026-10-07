package com.ciphervault.app.main.api;

import com.ciphervault.app.main.model.AnalyticsResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AnalyticsApi {
    @GET("/api/analytics")
    Call<AnalyticsResponse> getAnalytics(@Query("period") String period);
}
