package com.ciphervault.ciphervault.exception;

import com.ciphervault.ciphervault.file.DuplicateFileException;
import com.ciphervault.ciphervault.file.StorageQuotaExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;

// Centralize exception handling and keep API error responses consistent.
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        log.warn("Upload rejected: File exceeds configured limit of 500MB");
        return errorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "MAX_UPLOAD_SIZE_EXCEEDED",
                "File exceeds the maximum allowable upload limit of 500MB");
    }

    @ExceptionHandler(DuplicateFileException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateFileException(DuplicateFileException exc) {
        log.warn("Upload conflict: Duplicate SHA-256 {}", exc.getSha256Hash());

        Map<String, Object> body = errorBody(
                "DUPLICATE_FILE",
                "Duplicate file already exists in your vault");
        body.put("sha256", exc.getSha256Hash());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(StorageQuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleQuotaException(StorageQuotaExceededException exc) {
        log.warn("Storage quota exceeded: {}", exc.getMessage());
        return errorResponse(
                HttpStatus.INSUFFICIENT_STORAGE,
                "STORAGE_QUOTA_EXCEEDED",
                "Storage quota exceeded. Your maximum capacity is 1 GB.");
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleFileNotFoundException(FileNotFoundException exc) {
        return errorResponse(
                HttpStatus.NOT_FOUND,
                "FILE_NOT_FOUND",
                exc.getMessage() != null ? exc.getMessage() : "File not found");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException exc) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                exc.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception exc) {
        log.error("Unhandled exception: {}", exc.getMessage(), exc);
        return errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred processing your request");
    }

    private ResponseEntity<Map<String, Object>> errorResponse(
            HttpStatus status,
            String error,
            String message) {
        return ResponseEntity.status(status).body(errorBody(error, message));
    }

    private Map<String, Object> errorBody(String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", error);
        body.put("message", message);
        return body;
    }
}