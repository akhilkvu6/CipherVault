package com.ciphervault.app.main.model;

public class FileUploadResponse {
    public boolean success;
    public String message;
    public Long fileId;
    public String filename;
    public Long fileSize;
    public Boolean encrypted;
    public String sha256Hash;
}