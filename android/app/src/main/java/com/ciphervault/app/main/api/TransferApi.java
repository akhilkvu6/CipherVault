package com.ciphervault.app.main.api;

import com.ciphervault.app.main.model.Transfer;
import com.ciphervault.app.main.model.PageResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TransferApi {
    @GET("/api/transfers")
    Call<PageResponse<Transfer>> listTransfers(
            @Query("page") int page,
            @Query("size") int size
    );

    @POST("/api/transfers/{id}/cancel")
    Call<Void> cancelTransfer(@Path("id") Long id);
}