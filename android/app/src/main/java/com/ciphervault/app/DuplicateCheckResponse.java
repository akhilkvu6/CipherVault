package com.ciphervault.app;

public class DuplicateCheckResponse {

    private boolean duplicate;
    private String message;
    private String filename;
    private long fileSize;
    private String uploadedAt;
    private boolean encrypted;
    private String location;

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

    public String getUploadedAt() {
        return uploadedAt;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public String getLocation() {
        return location;
    }
}