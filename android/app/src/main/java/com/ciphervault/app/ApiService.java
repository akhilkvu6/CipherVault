package com.ciphervault.app;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
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

    @POST("api/auth/change-password")
    Call<Map<String, Object>> changePassword(@Body ChangePasswordRequest request);

    @POST("api/auth/logout")
    Call<Map<String, Object>> logout();

    @PUT("api/user/name")
    Call<Map<String, Object>> updateName(@Body Map<String, String> request);

    @PUT("api/user/username")
    Call<Map<String, Object>> updateUsername(@Body Map<String, String> request);

    @PUT("api/user/password")
    Call<Map<String, Object>> updateUserPassword(@Body ChangePasswordRequest request);

    @retrofit2.http.HTTP(method = "DELETE", path = "api/user/data", hasBody = true)
    Call<Map<String, Object>> purgeVaultData(@Body Map<String, String> request);

    @retrofit2.http.HTTP(method = "DELETE", path = "api/user/account", hasBody = true)
    Call<Map<String, Object>> deleteAccount(@Body Map<String, String> request);

    @Multipart
    @POST("api/user/profile-photo")
    Call<Map<String, Object>> uploadProfilePhoto(@Part MultipartBody.Part photo);

    @GET("api/user/profile-photo")
    Call<ResponseBody> getProfilePhoto();

    @DELETE("api/user/profile-photo")
    Call<Map<String, Object>> deleteProfilePhoto();

    @GET("api/storage/summary")
    Call<Map<String, Object>> getStorageSummary();

    @GET("api/storage/categories")
    Call<List<Map<String, Object>>> getStorageCategories();

    @GET("api/user/profile")
    Call<UserProfileResponse> getUserProfile();

    @GET("api/files")
    Call<List<StoredFile>> getFiles(
            @Query("page") Integer page,
            @Query("size") Integer size,
            @Query("sort") String sort
    );

    @GET("api/files")
    Call<List<StoredFile>> getFiles();

    @GET("api/files/check-duplicate")
    Call<DuplicateCheckResponse> checkDuplicate(@Query("hash") String hash);

    @GET("api/files/search")
    Call<List<StoredFile>> searchFiles(
            @Query("query") String query,
            @Query("category") String category,
            @Query("cameraMake") String cameraMake,
            @Query("cameraModel") String cameraModel,
            @Query("resolution") String resolution,
            @Query("codec") String codec,
            @Query("artist") String artist,
            @Query("author") String author,
            @Query("genre") String genre
    );

    @GET("api/files/search")
    Call<List<StoredFile>> searchFiles(
            @Query("query") String query,
            @Query("category") String category,
            @Query("cameraMake") String cameraMake,
            @Query("codec") String codec,
            @Query("author") String author
    );

    @GET("api/files/search")
    Call<List<StoredFile>> searchFiles(
            @Query("query") String query,
            @Query("category") String category
    );

    @GET("api/files/suggestions")
    Call<List<String>> getSuggestions(@Query("prefix") String prefix);

    @Multipart
    @POST("api/files/upload")
    Call<UploadResponse> uploadFile(
            @Part MultipartBody.Part file
    );

    @Streaming
    @GET("api/files/{id}/download")
    Call<ResponseBody> downloadFile(
            @Path("id") Long id,
            @Query("decrypt") boolean decrypt
    );

    @Streaming
    @GET("api/files/{id}/preview")
    Call<ResponseBody> getFilePreview(@Path("id") Long id);

    @DELETE("api/files/{id}")
    Call<ResponseBody> deleteFile(@Path("id") Long id);

    @GET("api/transfers")
    Call<Map<String, Object>> getTransfers();

    @GET("api/transfers")
    Call<Map<String, Object>> getTransfers(
            @Query("page") Integer page,
            @Query("size") Integer size
    );

    @POST("api/transfers/{id}/cancel")
    Call<Map<String, Object>> cancelTransfer(@Path("id") Long id);

    @GET("api/activity")
    Call<Map<String, Object>> getActivity();

    @GET("api/activity")
    Call<Map<String, Object>> getActivity(
            @Query("page") Integer page,
            @Query("size") Integer size
    );
}