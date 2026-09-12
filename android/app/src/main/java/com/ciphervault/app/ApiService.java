package com.ciphervault.app;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

public interface ApiService {

    @GET("api/health")
    Call<Map<String, Object>> checkHealth();

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);

    @GET("api/files")
    Call<List<StoredFile>> getFiles();

    @Multipart
    @POST("api/files/upload")
    Call<UploadResponse> uploadFile(
            @Part MultipartBody.Part file,
            @Part("encrypt") RequestBody encrypt
    );

    @Streaming
    @GET("api/files/{id}/download")
    Call<ResponseBody> downloadFile(
            @Path("id") Long id,
            @Query("decrypt") boolean decrypt
    );

    @Streaming
    @GET("api/files/{id}/download")
    Call<ResponseBody> downloadFile(@Path("id") Long id);
}