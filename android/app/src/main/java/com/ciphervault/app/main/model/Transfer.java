package com.ciphervault.app.main.model;

public class Transfer {
    public Long id;
    public String filename;
    public String transferType; // UPLOAD, DOWNLOAD
    public String status; // QUEUED, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
    public Long totalBytes;
    public Long bytesTransferred;
    public String startedAt;
    public String completedAt;
    public String errorMessage;
    public Long fileId;
}