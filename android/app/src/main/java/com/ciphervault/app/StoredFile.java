package com.ciphervault.app;

import com.google.gson.annotations.SerializedName;

public class StoredFile {

    public enum FileCategory {
        ALL, IMAGES, VIDEOS, PDFS, OTHER
    }

    @SerializedName("id")
    private Long id;

    @SerializedName(value = "filename", alternate = {"originalFilename", "name", "fileName"})
    private String filename;

    @SerializedName(value = "fileSize", alternate = {"size"})
    private Long fileSize;

    @SerializedName("contentType")
    private String contentType;

    @SerializedName("encrypted")
    private boolean encrypted;

    @SerializedName("sha256Hash")
    private String sha256Hash;

    @SerializedName("createdAt")
    private String createdAt;

    public Long getId() {
        return id;
    }

    public String getFilename() {
        return (filename != null && !filename.trim().isEmpty()) ? filename : "Unnamed File";
    }

    public String getOriginalFilename() {
        return getFilename();
    }

    public Long getFileSize() {
        return fileSize != null ? fileSize : 0L;
    }

    public String getContentType() {
        return contentType != null ? contentType : "";
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public FileCategory getCategory() {
        String mime = getContentType().toLowerCase();
        String name = getFilename().toLowerCase();

        if (mime.startsWith("image/") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".png") || name.endsWith(".gif") || name.endsWith(".webp")
                || name.endsWith(".bmp") || name.endsWith(".svg")) {
            return FileCategory.IMAGES;
        }
        if (mime.startsWith("video/") || name.endsWith(".mp4") || name.endsWith(".mkv")
                || name.endsWith(".avi") || name.endsWith(".mov") || name.endsWith(".webm")
                || name.endsWith(".3gp")) {
            return FileCategory.VIDEOS;
        }
        if (mime.equals("application/pdf") || name.endsWith(".pdf")) {
            return FileCategory.PDFS;
        }
        return FileCategory.OTHER;
    }
}