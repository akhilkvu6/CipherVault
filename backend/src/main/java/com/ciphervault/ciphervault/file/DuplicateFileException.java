package com.ciphervault.ciphervault.file;

// Represent a duplicate file error and preserve its SHA-256 hash.
public class DuplicateFileException extends RuntimeException {
    private final String sha256Hash;

    public DuplicateFileException(String message, String sha256Hash) {
        super(message);
        this.sha256Hash = sha256Hash;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }
}