package com.ciphervault.app.main.api;

import com.ciphervault.app.main.model.StorageCategoryResponse;
import com.ciphervault.app.main.model.StorageSummaryResponse;
import com.ciphervault.app.main.model.DuplicateFileGroup;
import com.ciphervault.app.main.model.FileResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface StorageApi {
    @GET("/api/storage/summary")
    Call<StorageSummaryResponse> getStorageSummary();

    @GET("/api/storage/categories")
    Call<List<StorageCategoryResponse>> getStorageCategories();

    @GET("/api/storage/duplicates")
    Call<List<DuplicateFileGroup>> getDuplicateFiles();

    @GET("/api/storage/large-files")
    Call<List<FileResponse>> getLargeFiles();
}