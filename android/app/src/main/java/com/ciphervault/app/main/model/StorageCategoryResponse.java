package com.ciphervault.app.main.model;

public class StorageCategoryResponse {
    public String category;
    public Long fileCount;
    public Long totalBytes;
    public Double percentage;

    public long getSafeTotalBytes() {
        return totalBytes != null ? totalBytes : 0L;
    }

    public long getSafeFileCount() {
        return fileCount != null ? fileCount : 0L;
    }

    public double getSafePercentage() {
        return percentage != null ? percentage : 0.0;
    }
}