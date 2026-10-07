package com.ciphervault.app.main.api;

import com.ciphervault.app.main.model.FileResponse;
import com.ciphervault.app.main.model.PageResponse;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import retrofit2.http.DELETE;

public interface FileApi {
    @GET("/api/files")
    Call<List<FileResponse>> listFiles(
            @Query("page") Integer page,
            @Query("size") Integer size,
            @Query("sort") String sort
    );

    @GET("/api/files/search")
    Call<List<FileResponse>> searchFiles(
            @Query("query") String query
    );

    @GET("/api/files/check-duplicate")
    Call<java.util.Map<String, Object>> checkDuplicate(@Query("hash") String hash);

    @Multipart
    @POST("/api/files/upload")
    Call<com.ciphervault.app.main.model.FileUploadResponse> uploadFile(@Part MultipartBody.Part file);

    @Streaming
    @GET("/api/files/{id}/download")
    Call<ResponseBody> downloadFile(@Path("id") Long id);

    @Streaming
    @GET("/api/files/{id}/download")
    Call<ResponseBody> downloadFile(@Path("id") Long id, @Query("decrypt") boolean decrypt);

    @GET("/api/files/{id}/preview")
    Call<ResponseBody> getFilePreview(@Path("id") Long id);

    @DELETE("/api/files/{id}")
    Call<Void> deleteFile(@Path("id") Long id);
}