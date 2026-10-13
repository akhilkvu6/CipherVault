package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional(readOnly = true)
public class FileFacetService {

    private static final Logger log = LoggerFactory.getLogger(FileFacetService.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final FileRepository fileRepository;
    private final FileSearchService fileSearchService;
    private final FileCategoryService fileCategoryService;

    public FileFacetService(
            FileRepository fileRepository,
            FileSearchService fileSearchService,
            FileCategoryService fileCategoryService) {
        this.fileRepository = fileRepository;
        this.fileSearchService = fileSearchService;
        this.fileCategoryService = fileCategoryService;
    }

    public FileFacetsResponse getFacets(
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
            String lens,
            Long minSize,
            Long maxSize,
            String dateTaken) {

        if (user == null) {
            return new FileFacetsResponse(0, new ArrayList<>());
        }

        // 1. Calculate total matching files for current active criteria
        Specification<StoredFile> totalSpec = fileSearchService.buildSpecification(
                user, query, category, cameraMake, cameraModel, resolution, codec,
                artist, author, genre, lens, minSize, maxSize, dateTaken
        );
        long totalMatchingFiles = fileRepository.count(totalSpec);

        List<FileFacetsResponse.FacetField> facetFields = new ArrayList<>();

        // 2. File Type / Category Facet (always evaluated to allow switching types)
        FileFacetsResponse.FacetField categoryFacet = computeCategoryFacet(user, query, minSize, maxSize);
        if (categoryFacet != null && !categoryFacet.getValues().isEmpty()) {
            facetFields.add(categoryFacet);
        }

        String normCategory = category != null ? category.trim().toUpperCase(Locale.ROOT) : "ALL";

        // 3. Dynamic metadata facets depending on category context
        boolean isAll = normCategory.equals("ALL") || normCategory.isEmpty();
        boolean isPhotos = normCategory.equals("IMAGES") || normCategory.equals("PHOTOS");
        boolean isVideos = normCategory.equals("VIDEOS");
        boolean isAudio = normCategory.equals("AUDIO");
        boolean isDocs = normCategory.equals("DOCUMENTS") || normCategory.equals("DOCS");

        if (isPhotos || isAll || isVideos) {
            // Camera Make (exclude cameraMake & cameraModel from its own count calculation)
            Specification<StoredFile> makeSpec = fileSearchService.buildSpecification(
                    user, query, category, null, null, resolution, codec,
                    artist, author, genre, lens, minSize, maxSize, dateTaken
            );
            FileFacetsResponse.FacetField makeFacet = computeAttributeFacet(
                    user, makeSpec, "cameraMake", "Camera Make", 20
            );
            if (makeFacet != null && !makeFacet.getValues().isEmpty()) {
                facetFields.add(makeFacet);
            }

            // Camera Model (applies cameraMake if selected, but excludes cameraModel from its own calculation)
            Specification<StoredFile> modelSpec = fileSearchService.buildSpecification(
                    user, query, category, cameraMake, null, resolution, codec,
                    artist, author, genre, lens, minSize, maxSize, dateTaken
            );
            FileFacetsResponse.FacetField modelFacet = computeAttributeFacet(
                    user, modelSpec, "cameraModel", "Camera Model", 25
            );
            if (modelFacet != null && !modelFacet.getValues().isEmpty()) {
                facetFields.add(modelFacet);
            }
        }

        if (isPhotos || isVideos || isAll) {
            // Resolution (exclude resolution from its own count calculation)
            Specification<StoredFile> resSpec = fileSearchService.buildSpecification(
                    user, query, category, cameraMake, cameraModel, null, codec,
                    artist, author, genre, lens, minSize, maxSize, dateTaken
            );
            FileFacetsResponse.FacetField resFacet = computeAttributeFacet(
                    user, resSpec, "resolution", "Resolution", 20
            );
            if (resFacet != null && !resFacet.getValues().isEmpty()) {
                facetFields.add(resFacet);
            }
        }

        if (isPhotos) {
            // Lens (only for Photos / DSLR / mirrorless camera photos)
            Specification<StoredFile> lensSpec = fileSearchService.buildSpecification(
                    user, query, category, cameraMake, cameraModel, resolution, codec,
                    artist, author, genre, null, minSize, maxSize, dateTaken
            );
            FileFacetsResponse.FacetField lensFacet = computeAttributeFacet(
                    user, lensSpec, "lens", "Lens", 15
            );
            if (lensFacet != null && !lensFacet.getValues().isEmpty()) {
                facetFields.add(lensFacet);
            }
        }

        if (isVideos || isAudio) {
            // Video / Audio Codec
            Specification<StoredFile> codecSpec = fileSearchService.buildSpecification(
                    user, query, category, cameraMake, cameraModel, resolution, null,
                    artist, author, genre, lens, minSize, maxSize, dateTaken
            );
            FileFacetsResponse.FacetField codecFacet = computeCodecFacet(user, codecSpec, isVideos);
            if (codecFacet != null && !codecFacet.getValues().isEmpty()) {
                facetFields.add(codecFacet);
            }
        }

        if (isPhotos || isVideos || isAll) {
            // Date / Year (exclude dateTaken from its own count calculation)
            Specification<StoredFile> dateSpec = fileSearchService.buildSpecification(
                    user, query, category, cameraMake, cameraModel, resolution, codec,
                    artist, author, genre, lens, minSize, maxSize, null
            );
            FileFacetsResponse.FacetField dateFacet = computeDateYearFacet(user, dateSpec);
            if (dateFacet != null && !dateFacet.getValues().isEmpty()) {
                facetFields.add(dateFacet);
            }
        }

        if (isAudio) {
            // Artist
            Specification<StoredFile> artistSpec = fileSearchService.buildSpecification(
                    user, query, category, cameraMake, cameraModel, resolution, codec,
                    null, author, genre, lens, minSize, maxSize, dateTaken
            );
            FileFacetsResponse.FacetField artistFacet = computeAttributeFacet(
                    user, artistSpec, "artist", "Artist", 15
            );
            if (artistFacet != null && !artistFacet.getValues().isEmpty()) {
                facetFields.add(artistFacet);
            }

            // Genre
            Specification<StoredFile> genreSpec = fileSearchService.buildSpecification(
                    user, query, category, cameraMake, cameraModel, resolution, codec,
                    artist, author, null, lens, minSize, maxSize, dateTaken
            );
            FileFacetsResponse.FacetField genreFacet = computeAttributeFacet(
                    user, genreSpec, "genre", "Genre", 10
            );
            if (genreFacet != null && !genreFacet.getValues().isEmpty()) {
                facetFields.add(genreFacet);
            }
        }

        if (isDocs) {
            // Author
            Specification<StoredFile> authorSpec = fileSearchService.buildSpecification(
                    user, query, category, cameraMake, cameraModel, resolution, codec,
                    artist, null, genre, lens, minSize, maxSize, dateTaken
            );
            FileFacetsResponse.FacetField authorFacet = computeAttributeFacet(
                    user, authorSpec, "author", "Author", 15
            );
            if (authorFacet != null && !authorFacet.getValues().isEmpty()) {
                facetFields.add(authorFacet);
            }
        }

        return new FileFacetsResponse(totalMatchingFiles, facetFields);
    }

    private FileFacetsResponse.FacetField computeCategoryFacet(
            User user, String query, Long minSize, Long maxSize) {

        List<FileFacetsResponse.FacetValue> values = new ArrayList<>();
        String[] categories = new String[]{"ALL", "IMAGES", "VIDEOS", "DOCUMENTS", "AUDIO", "ARCHIVES"};
        String[] labels = new String[]{"All", "Photos", "Videos", "Documents", "Audio", "Archives"};

        for (int i = 0; i < categories.length; i++) {
            String cat = categories[i];
            String label = labels[i];

            Specification<StoredFile> spec = (root, cq, cb) -> {
                List<Predicate> preds = new ArrayList<>();
                preds.add(cb.equal(root.get("user"), user));

                if (!cat.equals("ALL")) {
                    Predicate cp = fileCategoryService.buildCategoryPredicate(cb, root, cat);
                    if (cp != null) preds.add(cp);
                }

                if (query != null && !query.isBlank()) {
                    String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                    preds.add(cb.or(
                            cb.like(cb.lower(root.get("originalFilename")), pattern),
                            cb.like(cb.lower(root.get("contentType")), pattern)
                    ));
                }

                if (minSize != null && minSize > 0) {
                    preds.add(cb.greaterThanOrEqualTo(root.get("fileSize"), minSize));
                }
                if (maxSize != null && maxSize > 0) {
                    preds.add(cb.lessThanOrEqualTo(root.get("fileSize"), maxSize));
                }

                return cb.and(preds.toArray(new Predicate[0]));
            };

            long count = fileRepository.count(spec);
            if (cat.equals("ALL") || count > 0) {
                values.add(new FileFacetsResponse.FacetValue(cat, label, count));
            }
        }

        return new FileFacetsResponse.FacetField("category", "File Type", true, values);
    }

    private FileFacetsResponse.FacetField computeAttributeFacet(
            User user,
            Specification<StoredFile> baseSpec,
            String attributeName,
            String label,
            int maxValues) {

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
            Root<StoredFile> root = cq.from(StoredFile.class);
            Join<StoredFile, FileMetadata> meta = root.join("metadata", JoinType.INNER);

            Expression<String> expr = meta.get(attributeName);
            cq.multiselect(expr, cb.count(root));

            Predicate basePredicate = baseSpec.toPredicate(root, cq, cb);
            Predicate notNull = cb.isNotNull(expr);
            Predicate notBlank = cb.notEqual(cb.trim(expr), "");

            Predicate combined = basePredicate != null
                    ? cb.and(basePredicate, notNull, notBlank)
                    : cb.and(notNull, notBlank);

            cq.where(combined);
            cq.groupBy(expr);
            cq.orderBy(cb.desc(cb.count(root)), cb.asc(expr));

            List<Object[]> rows = entityManager.createQuery(cq)
                    .setMaxResults(maxValues * 2)
                    .getResultList();

            List<FileFacetsResponse.FacetValue> values = new ArrayList<>();
            for (Object[] row : rows) {
                if (row.length < 2 || row[0] == null) continue;
                String val = row[0].toString().trim();
                long cnt = ((Number) row[1]).longValue();

                // Sanitize invalid placeholder values
                if (val.isEmpty()
                        || val.equalsIgnoreCase("undef")
                        || val.equalsIgnoreCase("null")
                        || val.equals("0")
                        || val.startsWith("0000:")) {
                    continue;
                }

                values.add(new FileFacetsResponse.FacetValue(val, val, cnt));
                if (values.size() >= maxValues) break;
            }

            return new FileFacetsResponse.FacetField(attributeName, label, !values.isEmpty(), values);

        } catch (Exception e) {
            log.error("Failed to compute attribute facet for field '{}': {}", attributeName, e.getMessage(), e);
            return new FileFacetsResponse.FacetField(attributeName, label, false, new ArrayList<>());
        }
    }

    private FileFacetsResponse.FacetField computeCodecFacet(
            User user, Specification<StoredFile> baseSpec, boolean preferVideo) {

        try {
            String attr = preferVideo ? "videoCodec" : "audioCodec";
            return computeAttributeFacet(user, baseSpec, attr, preferVideo ? "Video Codec" : "Audio Codec", 10);
        } catch (Exception e) {
            return new FileFacetsResponse.FacetField("codec", "Codec", false, new ArrayList<>());
        }
    }

    private FileFacetsResponse.FacetField computeDateYearFacet(
            User user, Specification<StoredFile> baseSpec) {

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
            Root<StoredFile> root = cq.from(StoredFile.class);
            Join<StoredFile, FileMetadata> meta = root.join("metadata", JoinType.INNER);

            Expression<String> expr = meta.get("dateTaken");
            cq.multiselect(expr, cb.count(root));

            Predicate basePredicate = baseSpec.toPredicate(root, cq, cb);
            Predicate notNull = cb.isNotNull(expr);
            Predicate notBlank = cb.notEqual(cb.trim(expr), "");

            Predicate combined = basePredicate != null
                    ? cb.and(basePredicate, notNull, notBlank)
                    : cb.and(notNull, notBlank);

            cq.where(combined);
            cq.groupBy(expr);

            List<Object[]> rows = entityManager.createQuery(cq).getResultList();

            // Aggregate by 4-digit Year
            Map<String, Long> yearCounts = new LinkedHashMap<>();
            for (Object[] row : rows) {
                if (row.length < 2 || row[0] == null) continue;
                String dt = row[0].toString().trim();
                long cnt = ((Number) row[1]).longValue();

                if (dt.length() >= 4 && !dt.startsWith("0000")) {
                    String year = dt.substring(0, 4);
                    if (year.matches("\\d{4}")) {
                        yearCounts.put(year, yearCounts.getOrDefault(year, 0L) + cnt);
                    }
                }
            }

            List<FileFacetsResponse.FacetValue> values = new ArrayList<>();
            // Sort years descending
            List<Map.Entry<String, Long>> sortedYears = new ArrayList<>(yearCounts.entrySet());
            sortedYears.sort((e1, e2) -> e2.getKey().compareTo(e1.getKey()));

            for (Map.Entry<String, Long> entry : sortedYears) {
                values.add(new FileFacetsResponse.FacetValue(entry.getKey(), entry.getKey(), entry.getValue()));
                if (values.size() >= 10) break;
            }

            return new FileFacetsResponse.FacetField("dateTaken", "Year", !values.isEmpty(), values);

        } catch (Exception e) {
            log.error("Failed to compute date facet: {}", e.getMessage(), e);
            return new FileFacetsResponse.FacetField("dateTaken", "Year", false, new ArrayList<>());
        }
    }
}
