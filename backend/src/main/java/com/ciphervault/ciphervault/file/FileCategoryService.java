package com.ciphervault.ciphervault.file;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

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
        String name = filename != null
                ? filename.toLowerCase(Locale.ROOT)
                : "";

        String mime = contentType != null
                ? contentType.toLowerCase(Locale.ROOT)
                : "";

        if (mime.startsWith("image/") || hasExtension(
                name,
                ".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp", ".svg")) {
            return CATEGORY_IMAGES;
        }

        if (mime.contains("pdf")
                || mime.contains("word")
                || mime.contains("document")
                || mime.contains("sheet")
                || mime.contains("excel")
                || mime.contains("text")
                || hasExtension(
                name,
                ".pdf", ".doc", ".docx", ".txt",
                ".xls", ".xlsx", ".ppt", ".pptx", ".csv")) {
            return CATEGORY_DOCUMENTS;
        }

        if (mime.startsWith("video/")
                || mime.startsWith("audio/")
                || hasExtension(
                name,
                ".mp4", ".mkv", ".avi", ".mov", ".webm", ".3gp",
                ".mp3", ".wav", ".flac", ".m4a", ".aac")) {
            return CATEGORY_MEDIA;
        }

        if (mime.contains("zip")
                || mime.contains("tar")
                || mime.contains("compressed")
                || hasExtension(
                name,
                ".zip", ".rar", ".7z", ".tar", ".gz")) {
            return CATEGORY_ARCHIVES;
        }

        return CATEGORY_OTHER;
    }

    public Predicate buildCategoryPredicate(
            CriteriaBuilder cb,
            Root<StoredFile> root,
            String category) {

        if (category == null
                || category.isBlank()
                || category.equalsIgnoreCase("all")) {
            return null;
        }

        String catLower = category.trim().toLowerCase(Locale.ROOT);
        String filename = "originalFilename";
        String contentType = "contentType";

        Predicate isImage = cb.or(
                cb.like(
                        cb.lower(root.get(contentType)),
                        "image/%"
                ),
                extensionPredicate(
                        cb,
                        root,
                        filename,
                        ".jpg", ".jpeg", ".png", ".webp",
                        ".gif", ".bmp", ".svg"
                )
        );

        Predicate isVideo = cb.or(
                cb.like(
                        cb.lower(root.get(contentType)),
                        "video/%"
                ),
                extensionPredicate(
                        cb,
                        root,
                        filename,
                        ".mp4", ".mkv", ".avi", ".mov",
                        ".webm", ".3gp"
                )
        );

        Predicate isPdf = cb.or(
                cb.equal(
                        cb.lower(root.get(contentType)),
                        "application/pdf"
                ),
                cb.like(
                        cb.lower(root.get(filename)),
                        "%.pdf"
                )
        );

        Predicate isDocument = cb.or(
                isPdf,
                cb.like(cb.lower(root.get(contentType)), "%word%"),
                cb.like(cb.lower(root.get(contentType)), "%document%"),
                cb.like(cb.lower(root.get(contentType)), "%sheet%"),
                cb.like(cb.lower(root.get(contentType)), "%excel%"),
                cb.like(cb.lower(root.get(contentType)), "%text%"),
                extensionPredicate(
                        cb,
                        root,
                        filename,
                        ".doc", ".docx", ".txt",
                        ".xls", ".xlsx",
                        ".ppt", ".pptx", ".csv"
                )
        );

        Predicate isAudio = cb.or(
                cb.like(
                        cb.lower(root.get(contentType)),
                        "audio/%"
                ),
                extensionPredicate(
                        cb,
                        root,
                        filename,
                        ".mp3", ".wav", ".flac", ".m4a", ".aac"
                )
        );

        Predicate isMedia = cb.or(isVideo, isAudio);

        Predicate isArchive = cb.or(
                cb.like(cb.lower(root.get(contentType)), "%zip%"),
                cb.like(cb.lower(root.get(contentType)), "%tar%"),
                cb.like(cb.lower(root.get(contentType)), "%compressed%"),
                extensionPredicate(
                        cb,
                        root,
                        filename,
                        ".zip", ".rar", ".7z", ".tar", ".gz"
                )
        );

        return switch (catLower) {
            case "images" -> isImage;
            case "videos" -> isVideo;
            case "pdfs" -> isPdf;
            case "documents" -> isDocument;
            case "media" -> isMedia;
            case "archives" -> isArchive;
            case "other" -> cb.not(
                    cb.or(isImage, isMedia, isDocument, isArchive)
            );
            default -> null;
        };
    }

    private boolean hasExtension(String filename, String... extensions) {
        for (String extension : extensions) {
            if (filename.endsWith(extension)) {
                return true;
            }
        }

        return false;
    }

    private Predicate extensionPredicate(
            CriteriaBuilder cb,
            Root<StoredFile> root,
            String field,
            String... extensions) {

        Predicate[] predicates = new Predicate[extensions.length];

        for (int i = 0; i < extensions.length; i++) {
            predicates[i] = cb.like(
                    cb.lower(root.get(field)),
                    "%" + extensions[i]
            );
        }

        return cb.or(predicates);
    }
}