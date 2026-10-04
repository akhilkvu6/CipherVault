# CipherVault Database Schema & Optimization

**Database Engine**: MySQL 8.0+ (InnoDB)  
**Collation**: `utf8mb4_unicode_ci`  
**Database Name**: `ciphervault`

---

## 1. Relational Schema Architecture

```
+--------------------------------------------------------+
|                         users                          |
+--------------------------------------------------------+
| id            BIGINT AUTO_INCREMENT PRIMARY KEY        |
| username      VARCHAR(255) NOT NULL                    |
| email         VARCHAR(255) NOT NULL UNIQUE             |
| password      VARCHAR(255) NOT NULL                    |
| user_key      VARCHAR(255) NULL                        | <-- Encrypted UDEK (Base64)
| storage_limit BIGINT NOT NULL DEFAULT 1073741824       | <-- 1 GB Default Quota
| used_storage  BIGINT NOT NULL DEFAULT 0                |
+--------------------------------------------------------+
                           | 1
                           |
                           | N
+--------------------------------------------------------+
|                      stored_files                      |
+--------------------------------------------------------+
| id                BIGINT AUTO_INCREMENT PRIMARY KEY    |
| user_id           BIGINT NOT NULL                      | <-- FK to users(id) ON DELETE CASCADE
| filename          VARCHAR(255) NOT NULL                | <-- Random UUID storage filename
| original_filename VARCHAR(255) NOT NULL                | <-- Original uploaded display name
| file_size         BIGINT NOT NULL                      |
| content_type      VARCHAR(255) NULL                    |
| encrypted         BOOLEAN NOT NULL DEFAULT FALSE       |
| sha256_hash       VARCHAR(64) NULL                     | <-- Hexadecimal SHA-256 Checksum
| storage_path      VARCHAR(512) NULL                    |
| preview_path      VARCHAR(255) NULL                    |
| has_preview       BOOLEAN NOT NULL DEFAULT FALSE       |
| preview_mime_type VARCHAR(64) NULL                     |
| created_at        TIMESTAMP NOT NULL DEFAULT NOW()     |
+--------------------------------------------------------+
                           | 1
                           |
                           | 0..1
+--------------------------------------------------------+
|                     file_metadata                      |
+--------------------------------------------------------+
| id                BIGINT AUTO_INCREMENT PRIMARY KEY    |
| file_id           BIGINT NOT NULL UNIQUE               | <-- FK to stored_files(id) ON DELETE CASCADE
| camera_make       VARCHAR(100) NULL                    |
| camera_model      VARCHAR(100) NULL                    |
| lens              VARCHAR(150) NULL                    |
| focal_length      VARCHAR(50) NULL                     |
| iso               VARCHAR(50) NULL                     |
| exposure_time     VARCHAR(50) NULL                     |
| f_number          VARCHAR(50) NULL                     |
| date_taken        VARCHAR(50) NULL                     |
| width             INT NULL                             |
| height            INT NULL                             |
| resolution        VARCHAR(50) NULL                     |
| duration          VARCHAR(50) NULL                     |
| video_codec       VARCHAR(50) NULL                     |
| audio_codec       VARCHAR(50) NULL                     |
| frame_rate        VARCHAR(50) NULL                     |
| bitrate           VARCHAR(50) NULL                     |
| title             VARCHAR(500) NULL                    |
| artist            VARCHAR(255) NULL                    |
| album             VARCHAR(255) NULL                    |
| genre             VARCHAR(100) NULL                    |
| release_year      VARCHAR(20) NULL                     |
| author            VARCHAR(500) NULL                    |
| creator           VARCHAR(255) NULL                    |
| subject           VARCHAR(255) NULL                    |
| keywords          TEXT NULL                            |
| doc_created_date  VARCHAR(50) NULL                     |
| doc_modified_date VARCHAR(50) NULL                     |
| extracted_at      TIMESTAMP NOT NULL DEFAULT NOW()     |
+--------------------------------------------------------+
```

---

## 2. DDL Schema Definition

```sql
CREATE DATABASE IF NOT EXISTS ciphervault
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE ciphervault;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    user_key VARCHAR(255) NULL,
    storage_limit BIGINT NOT NULL DEFAULT 1073741824,
    used_storage BIGINT NOT NULL DEFAULT 0,
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Stored files table
CREATE TABLE IF NOT EXISTS stored_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(255) NULL,
    encrypted BOOLEAN NOT NULL DEFAULT FALSE,
    sha256_hash VARCHAR(64) NULL,
    storage_path VARCHAR(512) NULL,
    preview_path VARCHAR(255) NULL,
    has_preview BOOLEAN NOT NULL DEFAULT FALSE,
    preview_mime_type VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stored_files_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_stored_files_user_sha256 UNIQUE (user_id, sha256_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- File metadata table
CREATE TABLE IF NOT EXISTS file_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id BIGINT NOT NULL UNIQUE,
    camera_make VARCHAR(100) NULL,
    camera_model VARCHAR(100) NULL,
    lens VARCHAR(150) NULL,
    focal_length VARCHAR(50) NULL,
    iso VARCHAR(50) NULL,
    exposure_time VARCHAR(50) NULL,
    f_number VARCHAR(50) NULL,
    date_taken VARCHAR(50) NULL,
    width INT NULL,
    height INT NULL,
    resolution VARCHAR(50) NULL,
    duration VARCHAR(50) NULL,
    video_codec VARCHAR(50) NULL,
    audio_codec VARCHAR(50) NULL,
    frame_rate VARCHAR(50) NULL,
    bitrate VARCHAR(50) NULL,
    title VARCHAR(500) NULL,
    artist VARCHAR(255) NULL,
    album VARCHAR(255) NULL,
    genre VARCHAR(100) NULL,
    release_year VARCHAR(20) NULL,
    author VARCHAR(500) NULL,
    creator VARCHAR(255) NULL,
    subject VARCHAR(255) NULL,
    keywords TEXT NULL,
    doc_created_date VARCHAR(50) NULL,
    doc_modified_date VARCHAR(50) NULL,
    extracted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_file_metadata_file FOREIGN KEY (file_id) REFERENCES stored_files(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 3. High-Performance Indexing Strategy

| Index Name | Table | Columns | Purpose |
|------------|-------|---------|---------|
| `PRIMARY` | `users` | `id` | Clustered index primary key |
| `idx_users_email` | `users` | `email` | Instant $O(1)$ JWT auth lookup |
| `PRIMARY` | `stored_files` | `id` | Clustered index primary key |
| `uq_stored_files_user_sha256` | `stored_files` | `(user_id, sha256_hash)` | Composite uniqueness constraint; zero-race deduplication |
| `idx_files_user_created` | `stored_files` | `(user_id, created_at DESC)` | Avoids filesort for home/files timeline |
| `idx_files_user_orig_name`| `stored_files` | `(user_id, original_filename)` | Server-side autocomplete & search |
| `PRIMARY` | `file_metadata` | `id` | Clustered index primary key |
| `file_id` (UNIQUE) | `file_metadata` | `file_id` | $1:1$ StoredFile relationship mapping |
| `idx_metadata_camera` | `file_metadata` | `(camera_make, camera_model)` | Camera filter lookups |
| `idx_metadata_media` | `file_metadata` | `(video_codec, resolution)` | Media player filter lookups |
| `idx_metadata_artist_genre` | `file_metadata` | `(artist, genre)` | Audio tagging search |
