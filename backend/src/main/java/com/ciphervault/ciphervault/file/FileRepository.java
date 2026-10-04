package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<StoredFile, Long>, JpaSpecificationExecutor<StoredFile> {

    @Query("SELECT f FROM StoredFile f LEFT JOIN FETCH f.user WHERE f.metadata IS NULL")
    List<StoredFile> findByMetadataIsNull();

    List<StoredFile> findByUser(User user);

    List<StoredFile> findByUserOrderByCreatedAtDesc(User user);

    org.springframework.data.domain.Page<StoredFile> findByUserOrderByCreatedAtDesc(User user, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT DISTINCT f.originalFilename FROM StoredFile f WHERE f.user = :user AND LOWER(f.originalFilename) LIKE LOWER(CONCAT('%', :prefix, '%')) ORDER BY f.originalFilename ASC")
    List<String> findMatchingFilenamesByUser(@org.springframework.data.repository.query.Param("user") User user, @org.springframework.data.repository.query.Param("prefix") String prefix, org.springframework.data.domain.Pageable pageable);

    List<StoredFile> findByUserAndOriginalFilenameContainingIgnoreCaseOrderByCreatedAtDesc(User user, String query);

    Optional<StoredFile> findByIdAndUser(Long id, User user);

    Optional<StoredFile> findByUserAndSha256Hash(User user, String sha256Hash);
}