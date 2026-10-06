package com.ciphervault.ciphervault.file;

import com.ciphervault.ciphervault.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    @Query("""
            SELECT DISTINCT m.cameraMake
            FROM FileMetadata m
            WHERE m.file.user = :user
              AND m.cameraMake IS NOT NULL
              AND TRIM(m.cameraMake) != ''
            """)
    List<String> findDistinctCameraMakesByUser(@Param("user") User user);

    @Query("""
            SELECT DISTINCT m.cameraModel
            FROM FileMetadata m
            WHERE m.file.user = :user
              AND m.cameraModel IS NOT NULL
              AND TRIM(m.cameraModel) != ''
            """)
    List<String> findDistinctCameraModelsByUser(@Param("user") User user);

    @Query("""
            SELECT DISTINCT m.resolution
            FROM FileMetadata m
            WHERE m.file.user = :user
              AND m.resolution IS NOT NULL
              AND TRIM(m.resolution) != ''
            """)
    List<String> findDistinctResolutionsByUser(@Param("user") User user);

    @Query("""
            SELECT DISTINCT m.videoCodec
            FROM FileMetadata m
            WHERE m.file.user = :user
              AND m.videoCodec IS NOT NULL
              AND TRIM(m.videoCodec) != ''
            """)
    List<String> findDistinctVideoCodecsByUser(@Param("user") User user);

    @Query("""
            SELECT DISTINCT m.artist
            FROM FileMetadata m
            WHERE m.file.user = :user
              AND m.artist IS NOT NULL
              AND TRIM(m.artist) != ''
            """)
    List<String> findDistinctArtistsByUser(@Param("user") User user);

    @Query("""
            SELECT DISTINCT m.album
            FROM FileMetadata m
            WHERE m.file.user = :user
              AND m.album IS NOT NULL
              AND TRIM(m.album) != ''
            """)
    List<String> findDistinctAlbumsByUser(@Param("user") User user);

    @Query("""
            SELECT DISTINCT m.author
            FROM FileMetadata m
            WHERE m.file.user = :user
              AND m.author IS NOT NULL
              AND TRIM(m.author) != ''
            """)
    List<String> findDistinctAuthorsByUser(@Param("user") User user);

    @Query("""
            SELECT DISTINCT m.genre
            FROM FileMetadata m
            WHERE m.file.user = :user
              AND m.genre IS NOT NULL
              AND TRIM(m.genre) != ''
            """)
    List<String> findDistinctGenresByUser(@Param("user") User user);

    @Query("""
            SELECT DISTINCT m.title
            FROM FileMetadata m
            WHERE m.file.user = :user
              AND m.title IS NOT NULL
              AND TRIM(m.title) != ''
            """)
    List<String> findDistinctTitlesByUser(@Param("user") User user);

    @Query("""
            SELECT m.cameraMake, m.cameraModel, m.resolution, m.videoCodec,
                   m.artist, m.album, m.author, m.genre, m.title
            FROM FileMetadata m
            WHERE m.file.user = :user
              AND (
                LOWER(m.cameraMake) LIKE LOWER(CONCAT(:prefix, '%')) OR
                LOWER(m.cameraModel) LIKE LOWER(CONCAT(:prefix, '%')) OR
                LOWER(m.resolution) LIKE LOWER(CONCAT(:prefix, '%')) OR
                LOWER(m.videoCodec) LIKE LOWER(CONCAT(:prefix, '%')) OR
                LOWER(m.artist) LIKE LOWER(CONCAT(:prefix, '%')) OR
                LOWER(m.album) LIKE LOWER(CONCAT(:prefix, '%')) OR
                LOWER(m.author) LIKE LOWER(CONCAT(:prefix, '%')) OR
                LOWER(m.genre) LIKE LOWER(CONCAT(:prefix, '%')) OR
                LOWER(m.title) LIKE LOWER(CONCAT(:prefix, '%'))
              )
            """)
    List<Object[]> findSuggestions(@Param("user") User user, @Param("prefix") String prefix);
}