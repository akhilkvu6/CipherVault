package com.ciphervault.app;

import com.google.gson.annotations.SerializedName;

public class StoredFile {

    public enum FileCategory {
        ALL, IMAGES, VIDEOS, PDFS, DOCUMENTS, AUDIO, ARCHIVES, OTHER
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

    @SerializedName("hasPreview")
    private boolean hasPreview;

    @SerializedName("category")
    private String backendCategory;

    @SerializedName("metadata")
    private FileMetadataDTO metadata;

    public Long getId() {
        return id;
    }

    public boolean hasPreview() {
        return hasPreview;
    }

    public FileMetadataDTO getMetadata() {
        return metadata;
    }

    public String getResolution() {
        return (metadata != null && metadata.getResolution() != null) ? metadata.getResolution() : null;
    }

    public String getCameraInfo() {
        if (metadata == null) return null;
        String make = metadata.getCameraMake();
        String model = metadata.getCameraModel();
        if (make != null && model != null) {
            if (model.toLowerCase().startsWith(make.toLowerCase())) {
                return model;
            }
            return make + " " + model;
        }
        return make != null ? make : model;
    }

    public String getDuration() {
        return (metadata != null && metadata.getDuration() != null) ? metadata.getDuration() : null;
    }

    public String getCodec() {
        if (metadata == null) return null;
        return metadata.getVideoCodec() != null ? metadata.getVideoCodec() : metadata.getAudioCodec();
    }

    public String getArtistOrAuthor() {
        if (metadata == null) return null;
        if (metadata.getArtist() != null) return metadata.getArtist();
        if (metadata.getAuthor() != null) return metadata.getAuthor();
        return metadata.getCreator();
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
        if (mime.startsWith("audio/") || name.endsWith(".mp3") || name.endsWith(".wav")
                || name.endsWith(".flac") || name.endsWith(".m4a") || name.endsWith(".aac")) {
            return FileCategory.AUDIO;
        }
        if (mime.equals("application/pdf") || name.endsWith(".pdf")) {
            return FileCategory.PDFS;
        }
        if (mime.contains("word") || mime.contains("document") || mime.contains("sheet")
                || mime.contains("excel") || mime.contains("text")
                || name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".txt")
                || name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".ppt")
                || name.endsWith(".pptx") || name.endsWith(".csv")) {
            return FileCategory.DOCUMENTS;
        }
        if (mime.contains("zip") || mime.contains("tar") || mime.contains("compressed")
                || name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z")
                || name.endsWith(".tar") || name.endsWith(".gz")) {
            return FileCategory.ARCHIVES;
        }
        return FileCategory.OTHER;
    }
}