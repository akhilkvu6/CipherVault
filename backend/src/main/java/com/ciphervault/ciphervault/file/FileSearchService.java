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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
                
        Set<String> resultSet = new LinkedHashSet<>();
        
        // If empty prefix, just use % in the repository (which matches everything)
        // Wait, findSuggestions already handles it if we pass "".
        List<Object[]> rows = fileMetadataRepository.findSuggestions(user, normalizedPrefix);

        for (Object[] row : rows) {
            for (Object valueObj : row) {
                if (valueObj == null) continue;
                String value = valueObj.toString().trim();
                if (value.isBlank()) continue;
                
                if (normalizedPrefix.isEmpty() || value.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix)) {
                    resultSet.add(value);
                    if (resultSet.size() >= 10) {
                        return new ArrayList<>(resultSet);
                    }
                }
            }
        }

        // If prefix didn't match startsWith, also check contains if space permits
        if (resultSet.size() < 10) {
            for (Object[] row : rows) {
                for (Object valueObj : row) {
                    if (valueObj == null) continue;
                    String value = valueObj.toString().trim();
                    if (value.isBlank()) continue;

                    if (value.toLowerCase(Locale.ROOT).contains(normalizedPrefix)) {
                        resultSet.add(value);
                        if (resultSet.size() >= 10) {
                            return new ArrayList<>(resultSet);
                        }
                    }
                }
            }
        }

        return new ArrayList<>(resultSet);
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