package com.ciphervault.app;

import com.google.gson.annotations.SerializedName;

public class DuplicateCheckResponse {

    @SerializedName(value = "isDuplicate", alternate = {"duplicate"})
    private boolean duplicate;

    @SerializedName("message")
    private String message;

    @SerializedName(value = "existingFileName", alternate = {"filename"})
    private String filename;

    @SerializedName("fileSize")
    private long fileSize;

    @SerializedName("fileId")
    private Long fileId;

    @SerializedName("uploadedAt")
    private String uploadedAt;

    @SerializedName("encrypted")
    private boolean encrypted;

    public boolean isDuplicate() {
        return duplicate;
    }

    public String getMessage() {
        return message;
    }

    public String getFilename() {
        return filename;
    }

    public long getFileSize() {
        return fileSize;
    }

    public Long getFileId() {
        return fileId;
    }

    public String getUploadedAt() {
        return uploadedAt;
    }

    public boolean isEncrypted() {
        return encrypted;
    }
}