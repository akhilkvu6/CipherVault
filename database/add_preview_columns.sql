-- CipherVault Database Schema Update
-- Adds preview support columns to stored_files table

ALTER TABLE stored_files 
ADD COLUMN has_preview BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN preview_path VARCHAR(255) NULL,
ADD COLUMN preview_mime_type VARCHAR(100) NULL;
