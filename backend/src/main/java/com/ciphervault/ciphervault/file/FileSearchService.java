package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.user.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Service encapsulating JPA criteria search and metadata suggestions for StoredFiles.
 */
@Service
public class FileSearchService {

    private final FileRepository fileRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileCategoryService fileCategoryService;

    public FileSearchService(FileRepository fileRepository,
                             FileMetadataRepository fileMetadataRepository,
                             FileCategoryService fileCategoryService) {
        this.fileRepository = fileRepository;
        this.fileMetadataRepository = fileMetadataRepository;
        this.fileCategoryService = fileCategoryService;
    }

    public org.springframework.data.domain.Page<StoredFile> search(User user,
                                                   String query,
                                                   String category,
                                                   String cameraMake,
                                                   String cameraModel,
                                                   String resolution,
                                                   String codec,
                                                   String artist,
                                                   String author,
                                                   String genre,
                                                   org.springframework.data.domain.Pageable pageable) {
        Specification<StoredFile> spec = buildSpecification(user, query, category, cameraMake, cameraModel, resolution, codec, artist, author, genre);
        return fileRepository.findAll(spec, pageable);
    }

    public List<StoredFile> search(User user,
                                   String query,
                                   String category,
                                   String cameraMake,
                                   String cameraModel,
                                   String resolution,
                                   String codec,
                                   String artist,
                                   String author,
                                   String genre) {
        if (fileMetadataRepository == null) {
            if (query != null && !query.isBlank()) {
                return fileRepository.findByUserAndOriginalFilenameContainingIgnoreCaseOrderByCreatedAtDesc(user, query.trim());
            }
            return fileRepository.findByUserOrderByCreatedAtDesc(user);
        }
        Specification<StoredFile> spec = buildSpecification(user, query, category, cameraMake, cameraModel, resolution, codec, artist, author, genre);
        return fileRepository.findAll(spec);
    }

    private Specification<StoredFile> buildSpecification(User user,
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
            Join<StoredFile, FileMetadata> meta = root.join("metadata", JoinType.LEFT);

            // 2. Free-text search matching filename OR any metadata tag (case-insensitive)
            if (query != null && !query.isBlank()) {
                String p = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("originalFilename")), p),
                        cb.like(cb.lower(meta.get("cameraMake")), p),
                        cb.like(cb.lower(meta.get("cameraModel")), p),
                        cb.like(cb.lower(meta.get("lens")), p),
                        cb.like(cb.lower(meta.get("resolution")), p),
                        cb.like(cb.lower(meta.get("videoCodec")), p),
                        cb.like(cb.lower(meta.get("audioCodec")), p),
                        cb.like(cb.lower(meta.get("artist")), p),
                        cb.like(cb.lower(meta.get("album")), p),
                        cb.like(cb.lower(meta.get("genre")), p),
                        cb.like(cb.lower(meta.get("title")), p),
                        cb.like(cb.lower(meta.get("author")), p),
                        cb.like(cb.lower(meta.get("creator")), p),
                        cb.like(cb.lower(meta.get("subject")), p),
                        cb.like(cb.lower(meta.get("keywords")), p)
                ));
            }

            // 3. Category filter at database level
            Predicate catPred = fileCategoryService.buildCategoryPredicate(cb, root, category);
            if (catPred != null) {
                predicates.add(catPred);
            }

            // 4. Attribute-specific multi-filter matching
            if (cameraMake != null && !cameraMake.isBlank()) {
                predicates.add(cb.like(cb.lower(meta.get("cameraMake")), "%" + cameraMake.trim().toLowerCase() + "%"));
            }
            if (cameraModel != null && !cameraModel.isBlank()) {
                predicates.add(cb.like(cb.lower(meta.get("cameraModel")), "%" + cameraModel.trim().toLowerCase() + "%"));
            }
            if (resolution != null && !resolution.isBlank()) {
                predicates.add(cb.like(cb.lower(meta.get("resolution")), "%" + resolution.trim().toLowerCase() + "%"));
            }
            if (codec != null && !codec.isBlank()) {
                String c = "%" + codec.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(meta.get("videoCodec")), c),
                        cb.like(cb.lower(meta.get("audioCodec")), c)
                ));
            }
            if (artist != null && !artist.isBlank()) {
                predicates.add(cb.like(cb.lower(meta.get("artist")), "%" + artist.trim().toLowerCase() + "%"));
            }
            if (author != null && !author.isBlank()) {
                predicates.add(cb.like(cb.lower(meta.get("author")), "%" + author.trim().toLowerCase() + "%"));
            }
            if (genre != null && !genre.isBlank()) {
                predicates.add(cb.like(cb.lower(meta.get("genre")), "%" + genre.trim().toLowerCase() + "%"));
            }

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

        Set<String> resultSet = new LinkedHashSet<>();
        String p = (prefix != null) ? prefix.trim().toLowerCase() : "";

        List<List<String>> attributeLists = List.of(
                safeList(fileMetadataRepository.findDistinctCameraMakesByUser(user)),
                safeList(fileMetadataRepository.findDistinctCameraModelsByUser(user)),
                safeList(fileMetadataRepository.findDistinctResolutionsByUser(user)),
                safeList(fileMetadataRepository.findDistinctVideoCodecsByUser(user)),
                safeList(fileMetadataRepository.findDistinctArtistsByUser(user)),
                safeList(fileMetadataRepository.findDistinctAuthorsByUser(user)),
                safeList(fileMetadataRepository.findDistinctAlbumsByUser(user)),
                safeList(fileMetadataRepository.findDistinctGenresByUser(user)),
                safeList(fileMetadataRepository.findDistinctTitlesByUser(user))
        );

        for (List<String> list : attributeLists) {
            for (String val : list) {
                if (val != null && !val.isBlank()) {
                    String trimmed = val.trim();
                    if (p.isEmpty() || trimmed.toLowerCase().startsWith(p)) {
                        resultSet.add(trimmed);
                        if (resultSet.size() >= 10) {
                            return new ArrayList<>(resultSet);
                        }
                    }
                }
            }
        }

        // If prefix didn't match startsWith, also check contains if space permits
        if (!p.isEmpty() && resultSet.size() < 10) {
            for (List<String> list : attributeLists) {
                for (String val : list) {
                    if (val != null && !val.isBlank()) {
                        String trimmed = val.trim();
                        if (trimmed.toLowerCase().contains(p)) {
                            resultSet.add(trimmed);
                            if (resultSet.size() >= 10) {
                                return new ArrayList<>(resultSet);
                            }
                        }
                    }
                }
            }
        }

        return new ArrayList<>(resultSet);
    }

    private List<String> safeList(List<String> list) {
        return list != null ? list : Collections.emptyList();
    }
}
