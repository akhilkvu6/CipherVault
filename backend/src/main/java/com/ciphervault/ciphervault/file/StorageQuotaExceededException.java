package com.ciphervault.ciphervault.file;

public class StorageQuotaExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StorageQuotaExceededException(String message) {
        super(message);
    }
}