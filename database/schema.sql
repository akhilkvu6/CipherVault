-- ==============================================================================
-- CIPHERVAULT COMPLETE AUTHORITATIVE DATABASE SCHEMA (MySQL 8.0+)
-- ==============================================================================
-- Architecture: Private Cloud Storage with AES-256-GCM Streaming Encryption,
-- PBKDF2/HMAC-SHA256 Envelope Key Management, SHA-256 Deduplication,
-- Exif/Media Metadata Extraction, Atomic 10 GB Storage Quota Enforcement,
-- Live Transfer Auditing, and Sliding-Window Brute-Force Rate Limiting.
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS ciphervault 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE ciphervault;

-- ------------------------------------------------------------------------------
-- 1. USERS TABLE
-- ------------------------------------------------------------------------------
-- Manages user accounts, credentials, PBKDF2 envelope keys, and atomic storage quotas.
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    storage_limit BIGINT NOT NULL DEFAULT 10737418240, -- 10 GB Authoritative Quota in Bytes
    used_storage BIGINT NOT NULL DEFAULT 0,            -- Current Bytes Consumed
    user_key VARCHAR(512) NULL,                        -- AES Key Encrypted with Master Key
    token_version INT NOT NULL DEFAULT 1,              -- Incremented on Logout / Password Reset
    profile_photo_path VARCHAR(512) NULL,              -- Local path to custom profile avatar
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users_username (username),
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 2. STORED FILES TABLE
-- ------------------------------------------------------------------------------
-- Manages encrypted vault blobs, SHA-256 hash deduplication per user, and preview paths.
CREATE TABLE IF NOT EXISTS stored_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    sha256_hash VARCHAR(64) NOT NULL,
    storage_path VARCHAR(512) NOT NULL,
    encrypted BOOLEAN NOT NULL DEFAULT TRUE,
    has_preview BOOLEAN NOT NULL DEFAULT FALSE,
    preview_path VARCHAR(512) NULL,
    preview_mime_type VARCHAR(100) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stored_files_user 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_stored_files_user_sha256 
        UNIQUE (user_id, sha256_hash),
    INDEX idx_files_user (user_id),
    INDEX idx_files_user_created (user_id, created_at DESC),
    INDEX idx_files_user_size (user_id, file_size DESC),
    INDEX idx_files_user_sha256 (user_id, sha256_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 3. FILE METADATA TABLE
-- ------------------------------------------------------------------------------
-- Stores rich Exif, camera optics, video/audio codecs, and document metadata.
CREATE TABLE IF NOT EXISTS file_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id BIGINT NOT NULL UNIQUE,
    camera_make VARCHAR(255) NULL,
    camera_model VARCHAR(255) NULL,
    lens VARCHAR(255) NULL,
    focal_length VARCHAR(100) NULL,
    iso VARCHAR(50) NULL,
    exposure_time VARCHAR(100) NULL,
    f_number VARCHAR(50) NULL,
    date_taken VARCHAR(100) NULL,
    width INT NULL,
    height INT NULL,
    resolution VARCHAR(100) NULL,
    duration VARCHAR(100) NULL,
    video_codec VARCHAR(100) NULL,
    audio_codec VARCHAR(100) NULL,
    frame_rate VARCHAR(50) NULL,
    bitrate VARCHAR(100) NULL,
    title VARCHAR(255) NULL,
    artist VARCHAR(255) NULL,
    album VARCHAR(255) NULL,
    genre VARCHAR(100) NULL,
    release_year VARCHAR(50) NULL,
    author VARCHAR(255) NULL,
    creator TEXT NULL,
    subject TEXT NULL,
    keywords TEXT NULL,
    doc_created_date VARCHAR(100) NULL,
    doc_modified_date VARCHAR(100) NULL,
    raw_metadata_json TEXT NULL,
    CONSTRAINT fk_file_metadata_stored_file 
        FOREIGN KEY (file_id) REFERENCES stored_files(id) ON DELETE CASCADE,
    INDEX idx_meta_make (camera_make),
    INDEX idx_meta_model (camera_model),
    INDEX idx_meta_resolution (resolution),
    INDEX idx_meta_vcodec (video_codec),
    INDEX idx_meta_artist (artist),
    INDEX idx_meta_album (album),
    INDEX idx_meta_genre (genre),
    INDEX idx_meta_author (author),
    INDEX idx_meta_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 4. ACTIVITY EVENTS TABLE
-- ------------------------------------------------------------------------------
-- Audits security events, authentication, uploads, downloads, and account updates.
CREATE TABLE IF NOT EXISTS activity_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    summary VARCHAR(500) NULL,
    file_count INT NULL,
    total_bytes BIGINT NULL,
    affected_files TEXT NULL,
    status VARCHAR(50) NULL,
    error_message VARCHAR(500) NULL,
    CONSTRAINT fk_activity_events_user 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_activity_user (user_id),
    INDEX idx_activity_user_time (user_id, timestamp DESC),
    INDEX idx_activity_user_type (user_id, event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 5. TRANSFERS TABLE
-- ------------------------------------------------------------------------------
-- Records background streaming transfer batches and throughput history.
CREATE TABLE IF NOT EXISTS transfers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    transfer_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_bytes BIGINT NULL,
    transferred_bytes BIGINT NULL,
    file_ids VARCHAR(255) NULL,
    file_names TEXT NULL,
    batch_identifier VARCHAR(100) NULL,
    error_message VARCHAR(500) NULL,
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_transfers_user 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_transfers_user (user_id),
    INDEX idx_transfers_user_type (user_id, transfer_type),
    INDEX idx_transfers_user_time (user_id, started_at DESC),
    INDEX idx_transfers_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
