package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FileRepository
        extends JpaRepository<StoredFile, Long>, JpaSpecificationExecutor<StoredFile> {

    @Query("SELECT f FROM StoredFile f LEFT JOIN FETCH f.user WHERE f.metadata IS NULL")
    List<StoredFile> findByMetadataIsNull();

    @EntityGraph(attributePaths = {"metadata"})
    @org.springframework.lang.NonNull
    Page<StoredFile> findAll(@org.springframework.lang.Nullable org.springframework.data.jpa.domain.Specification<StoredFile> spec, @org.springframework.lang.NonNull Pageable pageable);

    @EntityGraph(attributePaths = {"metadata"})
    @org.springframework.lang.NonNull
    List<StoredFile> findAll(@org.springframework.lang.Nullable org.springframework.data.jpa.domain.Specification<StoredFile> spec);

    @EntityGraph(attributePaths = {"metadata"})
    List<StoredFile> findByUser(User user);

    @EntityGraph(attributePaths = {"metadata"})
    List<StoredFile> findByUserOrderByCreatedAtDesc(User user);

    @EntityGraph(attributePaths = {"metadata"})
    Page<StoredFile> findByUserOrderByCreatedAtDesc(
            User user,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"metadata"})
    List<StoredFile> findByUserAndOriginalFilenameContainingIgnoreCaseOrderByCreatedAtDesc(
            User user,
            String query
    );

    @Query("SELECT f.originalFilename, f.contentType, f.fileSize, f.encrypted FROM StoredFile f WHERE f.user = :user")
    List<Object[]> findFileStatsByUser(@Param("user") User user);

    @EntityGraph(attributePaths = {"metadata"})
    Optional<StoredFile> findByIdAndUser(Long id, User user);

    Optional<StoredFile> findByUserAndSha256Hash(
            User user,
            String sha256Hash
    );

    long countByUser(User user);
    
    @EntityGraph(attributePaths = {"metadata"})
    List<StoredFile> findByUserOrderByFileSizeDesc(User user, Pageable pageable);
}