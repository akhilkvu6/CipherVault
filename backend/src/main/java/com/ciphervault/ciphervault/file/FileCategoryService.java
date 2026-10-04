package com.ciphervault.ciphervault.file;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Authoritative service for determining file categories and building
 * database category predicates for search queries.
 */
@Service
public class FileCategoryService {

    public static final String CATEGORY_IMAGES = "Images";
    public static final String CATEGORY_DOCUMENTS = "Documents";
    public static final String CATEGORY_MEDIA = "Media";
    public static final String CATEGORY_ARCHIVES = "Archives";
    public static final String CATEGORY_OTHER = "Other";

    private static final List<String> STANDARD_CATEGORIES = List.of(
            CATEGORY_IMAGES,
            CATEGORY_DOCUMENTS,
            CATEGORY_MEDIA,
            CATEGORY_ARCHIVES,
            CATEGORY_OTHER
    );

    public List<String> getStandardCategories() {
        return STANDARD_CATEGORIES;
    }

    public String determineCategory(String filename, String contentType) {
        String name = filename != null ? filename.toLowerCase() : "";
        String mime = contentType != null ? contentType.toLowerCase() : "";

        if (mime.startsWith("image/") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".gif")
                || name.endsWith(".bmp") || name.endsWith(".svg")) {
            return CATEGORY_IMAGES;
        }
        if (name.endsWith(".pdf") || mime.contains("pdf") || mime.contains("word")
                || mime.contains("document") || mime.contains("sheet") || mime.contains("excel")
                || mime.contains("text") || name.endsWith(".doc") || name.endsWith(".docx")
                || name.endsWith(".txt") || name.endsWith(".xls") || name.endsWith(".xlsx")
                || name.endsWith(".ppt") || name.endsWith(".pptx") || name.endsWith(".csv")) {
            return CATEGORY_DOCUMENTS;
        }
        if (mime.startsWith("video/") || mime.startsWith("audio/") || name.endsWith(".mp4")
                || name.endsWith(".mkv") || name.endsWith(".avi") || name.endsWith(".mov")
                || name.endsWith(".webm") || name.endsWith(".mp3") || name.endsWith(".wav")
                || name.endsWith(".flac") || name.endsWith(".m4a") || name.endsWith(".aac")) {
            return CATEGORY_MEDIA;
        }
        if (mime.contains("zip") || mime.contains("tar") || mime.contains("compressed")
                || name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z")
                || name.endsWith(".tar") || name.endsWith(".gz")) {
            return CATEGORY_ARCHIVES;
        }
        return CATEGORY_OTHER;
    }

    public Predicate buildCategoryPredicate(CriteriaBuilder cb, Root<StoredFile> root, String category) {
        if (category == null || category.isBlank() || category.equalsIgnoreCase("all")) {
            return null;
        }

        String catLower = category.trim().toLowerCase();
        Predicate isImage = cb.or(
                cb.like(cb.lower(root.get("contentType")), "image/%"),
                cb.like(cb.lower(root.get("originalFilename")), "%.jpg"),
                cb.like(cb.lower(root.get("originalFilename")), "%.jpeg"),
                cb.like(cb.lower(root.get("originalFilename")), "%.png"),
                cb.like(cb.lower(root.get("originalFilename")), "%.webp"),
                cb.like(cb.lower(root.get("originalFilename")), "%.gif"),
                cb.like(cb.lower(root.get("originalFilename")), "%.bmp"),
                cb.like(cb.lower(root.get("originalFilename")), "%.svg")
        );

        Predicate isVideo = cb.or(
                cb.like(cb.lower(root.get("contentType")), "video/%"),
                cb.like(cb.lower(root.get("originalFilename")), "%.mp4"),
                cb.like(cb.lower(root.get("originalFilename")), "%.mkv"),
                cb.like(cb.lower(root.get("originalFilename")), "%.avi"),
                cb.like(cb.lower(root.get("originalFilename")), "%.mov"),
                cb.like(cb.lower(root.get("originalFilename")), "%.webm"),
                cb.like(cb.lower(root.get("originalFilename")), "%.3gp")
        );

        Predicate isPdf = cb.or(
                cb.equal(cb.lower(root.get("contentType")), "application/pdf"),
                cb.like(cb.lower(root.get("originalFilename")), "%.pdf")
        );

        Predicate isDocument = cb.or(
                isPdf,
                cb.like(cb.lower(root.get("contentType")), "%word%"),
                cb.like(cb.lower(root.get("contentType")), "%document%"),
                cb.like(cb.lower(root.get("contentType")), "%sheet%"),
                cb.like(cb.lower(root.get("contentType")), "%excel%"),
                cb.like(cb.lower(root.get("contentType")), "%text%"),
                cb.like(cb.lower(root.get("originalFilename")), "%.doc"),
                cb.like(cb.lower(root.get("originalFilename")), "%.docx"),
                cb.like(cb.lower(root.get("originalFilename")), "%.txt"),
                cb.like(cb.lower(root.get("originalFilename")), "%.xls"),
                cb.like(cb.lower(root.get("originalFilename")), "%.xlsx"),
                cb.like(cb.lower(root.get("originalFilename")), "%.ppt"),
                cb.like(cb.lower(root.get("originalFilename")), "%.pptx"),
                cb.like(cb.lower(root.get("originalFilename")), "%.csv")
        );

        Predicate isAudio = cb.or(
                cb.like(cb.lower(root.get("contentType")), "audio/%"),
                cb.like(cb.lower(root.get("originalFilename")), "%.mp3"),
                cb.like(cb.lower(root.get("originalFilename")), "%.wav"),
                cb.like(cb.lower(root.get("originalFilename")), "%.flac"),
                cb.like(cb.lower(root.get("originalFilename")), "%.m4a"),
                cb.like(cb.lower(root.get("originalFilename")), "%.aac")
        );

        Predicate isMedia = cb.or(isVideo, isAudio);

        Predicate isArchive = cb.or(
                cb.like(cb.lower(root.get("contentType")), "%zip%"),
                cb.like(cb.lower(root.get("contentType")), "%tar%"),
                cb.like(cb.lower(root.get("contentType")), "%compressed%"),
                cb.like(cb.lower(root.get("originalFilename")), "%.zip"),
                cb.like(cb.lower(root.get("originalFilename")), "%.rar"),
                cb.like(cb.lower(root.get("originalFilename")), "%.7z"),
                cb.like(cb.lower(root.get("originalFilename")), "%.tar"),
                cb.like(cb.lower(root.get("originalFilename")), "%.gz")
        );

        if (catLower.equals("images")) {
            return isImage;
        } else if (catLower.equals("videos")) {
            return isVideo;
        } else if (catLower.equals("pdfs")) {
            return isPdf;
        } else if (catLower.equals("documents")) {
            return isDocument;
        } else if (catLower.equals("media")) {
            return isMedia;
        } else if (catLower.equals("archives")) {
            return isArchive;
        } else if (catLower.equals("other")) {
            return cb.not(cb.or(isImage, isMedia, isDocument, isArchive));
        }

        return null;
    }
}