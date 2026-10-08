package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.user.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Service encapsulating JPA criteria search and metadata suggestions for StoredFiles.
 */
@Service
public class FileSearchService {

    private final FileRepository fileRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileCategoryService fileCategoryService;

    public FileSearchService(
            FileRepository fileRepository,
            FileMetadataRepository fileMetadataRepository,
            FileCategoryService fileCategoryService) {
        this.fileRepository = fileRepository;
        this.fileMetadataRepository = fileMetadataRepository;
        this.fileCategoryService = fileCategoryService;
    }

    public Page<StoredFile> search(
            User user,
            String query,
            String category,
            String cameraMake,
            String cameraModel,
            String resolution,
            String codec,
            String artist,
            String author,
            String genre,
            Pageable pageable) {

        Specification<StoredFile> specification = buildSpecification(
                user,
                query,
                category,
                cameraMake,
                cameraModel,
                resolution,
                codec,
                artist,
                author,
                genre
        );

        return fileRepository.findAll(specification, pageable);
    }

    public List<StoredFile> search(
            User user,
            String query,
            String category,
            String cameraMake,
            String cameraModel,
            String resolution,
            String codec,
            String artist,
            String author,
            String genre) {        if (fileMetadataRepository == null) {
            if (query != null && !query.isBlank()) {
                return fileRepository
                        .findByUserAndOriginalFilenameContainingIgnoreCaseOrderByCreatedAtDesc(
                                user,
                                query.trim()
                        );
            }

            return fileRepository.findByUserOrderByCreatedAtDesc(user);
        }

        Specification<StoredFile> specification = buildSpecification(
                user,
                query,
                category,
                cameraMake,
                cameraModel,
                resolution,
                codec,
                artist,
                author,
                genre
        );

        return fileRepository.findAll(specification);
    }

    private Specification<StoredFile> buildSpecification(
            User user,
            String query,
            String category,
            String cameraMake,
            String cameraModel,
            String resolution,
            String codec,
            String artist,
            String author,
            String genre) {

        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory user isolation - results MUST belong only to authenticated user
            predicates.add(cb.equal(root.get("user"), user));

            // Join metadata table
            Join<StoredFile, FileMetadata> meta =
                    root.join("metadata", JoinType.LEFT);

            // 2. Free-text search matching filename OR any metadata tag (case-insensitive)
            if (query != null && !query.isBlank()) {
                String pattern = containsPattern(query);

                predicates.add(cb.or(
                        likeIgnoreCase(cb, root.get("originalFilename"), pattern),
                        likeIgnoreCase(cb, root.get("contentType"), pattern),
                        likeIgnoreCase(cb, meta.get("cameraMake"), pattern),
                        likeIgnoreCase(cb, meta.get("cameraModel"), pattern),
                        likeIgnoreCase(cb, meta.get("lens"), pattern),
                        likeIgnoreCase(cb, meta.get("resolution"), pattern),
                        likeIgnoreCase(cb, meta.get("videoCodec"), pattern),
                        likeIgnoreCase(cb, meta.get("audioCodec"), pattern),
                        likeIgnoreCase(cb, meta.get("artist"), pattern),
                        likeIgnoreCase(cb, meta.get("album"), pattern),
                        likeIgnoreCase(cb, meta.get("genre"), pattern),
                        likeIgnoreCase(cb, meta.get("title"), pattern),
                        likeIgnoreCase(cb, meta.get("author"), pattern),
                        likeIgnoreCase(cb, meta.get("creator"), pattern),
                        likeIgnoreCase(cb, meta.get("subject"), pattern),
                        likeIgnoreCase(cb, meta.get("keywords"), pattern)
                ));
            }

            // 3. Category filter at database level
            Predicate catPred =
                    fileCategoryService.buildCategoryPredicate(cb, root, category);

            if (catPred != null) {
                predicates.add(catPred);
            }

            // 4. Attribute-specific multi-filter matching
            addMetadataFilter(
                    predicates,
                    cb,
                    meta,
                    "cameraMake",
                    cameraMake
            );

            addMetadataFilter(
                    predicates,
                    cb,
                    meta,
                    "cameraModel",
                    cameraModel
            );

            addMetadataFilter(
                    predicates,
                    cb,
                    meta,
                    "resolution",
                    resolution
            );

            if (codec != null && !codec.isBlank()) {
                String pattern = containsPattern(codec);

                predicates.add(cb.or(
                        likeIgnoreCase(cb, meta.get("videoCodec"), pattern),
                        likeIgnoreCase(cb, meta.get("audioCodec"), pattern)
                ));
            }

            addMetadataFilter(
                    predicates,
                    cb,
                    meta,
                    "artist",
                    artist
            );

            addMetadataFilter(
                    predicates,
                    cb,
                    meta,
                    "author",
                    author
            );

            addMetadataFilter(
                    predicates,
                    cb,
                    meta,
                    "genre",
                    genre
            );

            if (cq != null) {
                cq.distinct(true);
                cq.orderBy(cb.desc(root.get("createdAt")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public List<String> getSuggestions(User user, String prefix) {
        if (user == null || fileMetadataRepository == null) {
            return Collections.emptyList();
        }

        String normalizedPrefix = prefix != null
                ? prefix.trim().toLowerCase(Locale.ROOT)
                : "";

        Map<String, Integer> frequencyMap = new LinkedHashMap<>();

        // 1. Process metadata attributes from file_metadata
        List<Object[]> rows = fileMetadataRepository.findSuggestions(user, normalizedPrefix);
        if (rows != null) {
            for (Object[] row : rows) {
                for (Object valueObj : row) {
                    if (valueObj == null) continue;
                    String value = valueObj.toString().trim();
                    if (isValidMetadataValue(value)) {
                        if (normalizedPrefix.isEmpty() || value.toLowerCase(Locale.ROOT).contains(normalizedPrefix)) {
                            frequencyMap.put(value, frequencyMap.getOrDefault(value, 0) + 1);
                        }
                    }
                }
            }
        }

        // 2. Process file extensions and formats from stored_files
        if (fileRepository != null) {
            List<Object[]> fileStats = fileRepository.findFileStatsByUser(user);
            if (fileStats != null) {
                for (Object[] stat : fileStats) {
                    if (stat.length > 0 && stat[0] != null) {
                        String origName = stat[0].toString();
                        int dot = origName.lastIndexOf('.');
                        if (dot > 0 && dot < origName.length() - 1) {
                            String ext = origName.substring(dot + 1).trim().toUpperCase(Locale.ROOT);
                            if (isValidMetadataValue(ext)) {
                                if (normalizedPrefix.isEmpty() || ext.toLowerCase(Locale.ROOT).contains(normalizedPrefix)) {
                                    frequencyMap.put(ext, frequencyMap.getOrDefault(ext, 0) + 1);
                                }
                            }
                        }
                    }
                    if (stat.length > 1 && stat[1] != null) {
                        String mime = stat[1].toString().trim().toLowerCase(Locale.ROOT);
                        String friendlyType = null;
                        if (mime.contains("pdf")) friendlyType = "PDF";
                        else if (mime.contains("jpeg") || mime.contains("jpg")) friendlyType = "JPEG";
                        else if (mime.contains("png")) friendlyType = "PNG";
                        else if (mime.contains("mp4")) friendlyType = "MP4";
                        else if (mime.contains("zip") || mime.contains("tar")) friendlyType = "Archive";
                        if (friendlyType != null && (normalizedPrefix.isEmpty() || friendlyType.toLowerCase(Locale.ROOT).contains(normalizedPrefix))) {
                            frequencyMap.put(friendlyType, frequencyMap.getOrDefault(friendlyType, 0) + 1);
                        }
                    }
                }
            }
        }

        // 3. Sort by occurrence count descending, then alphabetical tie-breaker
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(frequencyMap.entrySet());
        entryList.sort((e1, e2) -> {
            int cmp = Integer.compare(e2.getValue(), e1.getValue());
            if (cmp != 0) return cmp;
            return e1.getKey().compareToIgnoreCase(e2.getKey());
        });

        List<String> results = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : entryList) {
            results.add(entry.getKey());
            if (results.size() >= 10) break;
        }

        return results;
    }

    private boolean isValidMetadataValue(String value) {
        if (value == null) return false;
        String trimmed = value.trim();
        if (trimmed.length() < 2 || trimmed.length() > 64) return false;
        if (trimmed.startsWith("/") || trimmed.startsWith("http") || trimmed.contains(".bin")) return false;
        if (trimmed.matches("^[0-9a-fA-F]{32,}$")) return false;
        return true;
    }

    private void addMetadataFilter(
            List<Predicate> predicates,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Join<StoredFile, FileMetadata> meta,
            String field,
            String value) {

        if (value != null && !value.isBlank()) {
            predicates.add(
                    likeIgnoreCase(
                            cb,
                            meta.get(field),
                            containsPattern(value)
                    )
            );
        }
    }

    private Predicate likeIgnoreCase(
            jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Expression<String> expression,
            String pattern) {

        return cb.like(
                cb.lower(expression),
                pattern.toLowerCase(Locale.ROOT)
        );
    }

    private String containsPattern(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private List<String> safeList(List<String> list) {
        return list != null ? list : Collections.emptyList();
    }
}