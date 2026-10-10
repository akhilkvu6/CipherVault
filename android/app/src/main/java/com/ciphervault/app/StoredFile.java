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
        if (backendCategory != null && !backendCategory.trim().isEmpty()) {
            String cat = backendCategory.trim().toLowerCase();
            if (cat.equals("images") || cat.equals("image") || cat.equals("photos") || cat.equals("photo")) {
                return FileCategory.IMAGES;
            }
            if (cat.equals("videos") || cat.equals("video")) {
                return FileCategory.VIDEOS;
            }
            if (cat.equals("audio") || cat.equals("audios")) {
                return FileCategory.AUDIO;
            }
            if (cat.equals("documents") || cat.equals("document") || cat.equals("docs") || cat.equals("doc")) {
                return FileCategory.DOCUMENTS;
            }
            if (cat.equals("archives") || cat.equals("archive")) {
                return FileCategory.ARCHIVES;
            }
            if (cat.equals("media")) {
                String mime = getContentType().toLowerCase();
                String name = getFilename().toLowerCase();
                if (mime.startsWith("audio/") || name.endsWith(".mp3") || name.endsWith(".wav")
                        || name.endsWith(".flac") || name.endsWith(".m4a") || name.endsWith(".aac")
                        || name.endsWith(".ogg") || name.endsWith(".oga") || name.endsWith(".wma")
                        || name.endsWith(".opus")) {
                    return FileCategory.AUDIO;
                }
                return FileCategory.VIDEOS;
            }
        }

        String mime = getContentType().toLowerCase();
        String name = getFilename().toLowerCase();

        if (mime.startsWith("image/") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".png") || name.endsWith(".gif") || name.endsWith(".webp")
                || name.endsWith(".bmp") || name.endsWith(".svg") || name.endsWith(".heic")
                || name.endsWith(".heif") || name.endsWith(".ico") || name.endsWith(".tiff")) {
            return FileCategory.IMAGES;
        }
        if (mime.startsWith("video/") || name.endsWith(".mp4") || name.endsWith(".mkv")
                || name.endsWith(".avi") || name.endsWith(".mov") || name.endsWith(".webm")
                || name.endsWith(".3gp") || name.endsWith(".m4v") || name.endsWith(".flv")
                || name.endsWith(".wmv") || name.endsWith(".ts") || name.endsWith(".asf")
                || name.endsWith(".vob") || name.endsWith(".ogv") || name.endsWith(".rmvb")
                || name.endsWith(".mpg") || name.endsWith(".mpeg") || name.endsWith(".m2ts")) {
            return FileCategory.VIDEOS;
        }
        if (mime.startsWith("audio/") || name.endsWith(".mp3") || name.endsWith(".wav")
                || name.endsWith(".flac") || name.endsWith(".m4a") || name.endsWith(".aac")
                || name.endsWith(".ogg") || name.endsWith(".oga") || name.endsWith(".wma")
                || name.endsWith(".opus") || name.endsWith(".mid") || name.endsWith(".midi")
                || name.endsWith(".aiff")) {
            return FileCategory.AUDIO;
        }
        if (mime.equals("application/pdf") || name.endsWith(".pdf")) {
            return FileCategory.PDFS;
        }
        if (mime.contains("word") || mime.contains("document") || mime.contains("sheet")
                || mime.contains("excel") || mime.contains("text") || mime.contains("presentation")
                || name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".txt")
                || name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".ppt")
                || name.endsWith(".pptx") || name.endsWith(".csv") || name.endsWith(".rtf")
                || name.endsWith(".odt") || name.endsWith(".ods") || name.endsWith(".odp")
                || name.endsWith(".md")) {
            return FileCategory.DOCUMENTS;
        }
        if (mime.contains("zip") || mime.contains("tar") || mime.contains("compressed")
                || name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z")
                || name.endsWith(".tar") || name.endsWith(".gz") || name.endsWith(".bz2")
                || name.endsWith(".xz") || name.endsWith(".iso") || name.endsWith(".apk")) {
            return FileCategory.ARCHIVES;
        }
        return FileCategory.OTHER;
    }
}