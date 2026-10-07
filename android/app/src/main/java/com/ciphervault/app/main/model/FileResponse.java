package com.ciphervault.app.main.model;

import com.google.gson.annotations.SerializedName;

public class FileResponse {
    public Long id;
    public String filename;

    @SerializedName(value = "fileSize", alternate = {"size"})
    public Long fileSize;

    @SerializedName(value = "contentType", alternate = {"mimeType"})
    public String contentType;

    public boolean encrypted;
    public String sha256Hash;

    @SerializedName(value = "createdAt", alternate = {"uploadedAt"})
    public String createdAt;

    public boolean hasPreview;
    public String category;
    public FileMetadataDTO metadata;

    public long getSafeFileSize() {
        return fileSize != null ? fileSize : 0L;
    }

    public String getSafeFilename() {
        return filename != null ? filename : "Unknown";
    }

    public String getSafeContentType() {
        return contentType != null ? contentType : "application/octet-stream";
    }
}