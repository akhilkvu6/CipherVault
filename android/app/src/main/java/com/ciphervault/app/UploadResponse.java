package com.ciphervault.app;

public class UploadResponse {
    private boolean success;
    private String message;
    private Long fileId;
    private String filename;
    private Long fileSize;
    private Boolean encrypted;
    private String sha256Hash;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Long getFileId() { return fileId; }
    public String getFilename() { return filename; }
}