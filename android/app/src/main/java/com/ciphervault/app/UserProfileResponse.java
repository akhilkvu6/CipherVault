package com.ciphervault.app;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class UserProfileResponse {

    @SerializedName("id")
    private Long id;

    @SerializedName("username")
    private String username;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("storageLimit")
    private Long storageLimit;

    @SerializedName("usedStorage")
    private Long usedStorage;

    @SerializedName("fileCount")
    private Long fileCount;

    @SerializedName("encryptedCount")
    private Long encryptedCount;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("categoryBytes")
    private Map<String, Long> categoryBytes;

    @SerializedName("categoryCounts")
    private Map<String, Long> categoryCounts;

    @SerializedName(value = "profilePhotoAvailable", alternate = {"photoAvailable"})
    private Boolean photoAvailable;

    public Long getId() { return id; }
    public String getName() { return name != null ? name : (username != null ? username : ""); }
    public String getUsername() { return username != null ? username : ""; }
    public String getEmail() { return email != null ? email : ""; }
    public long getStorageLimit() { return storageLimit != null ? storageLimit : 10737418240L; }
    public long getUsedStorage() { return usedStorage != null ? usedStorage : 0L; }
    public long getFileCount() { return fileCount != null ? fileCount : 0L; }
    public long getEncryptedCount() { return encryptedCount != null ? encryptedCount : 0L; }
    public String getCreatedAt() { return createdAt; }
    public Map<String, Long> getCategoryBytes() { return categoryBytes; }
    public Map<String, Long> getCategoryCounts() { return categoryCounts; }
    public boolean isPhotoAvailable() { return Boolean.TRUE.equals(photoAvailable); }
}
