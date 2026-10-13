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

/**
 * Centralized exception handling ensuring consistent JSON error bodies,
 * single-point logging, preservation of causal chains in diagnostic logs,
 * and zero internal stack trace leakage to clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException exc) {
        if (exc.getStatus().is5xxServerError()) {
            log.error("[ERROR] [API] Server error [{}]: {}", exc.getCode(), exc.getMessage(), exc);
        } else {
            log.warn("[WARN] [API] Client error [{}]: {}", exc.getCode(), exc.getMessage());
        }
        return errorResponse(
                exc.getStatus(),
                exc.getCode(),
                exc.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        log.warn("[WARN] [UPLOAD] Upload rejected: File exceeds configured limit of 2GB");
        return errorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "MAX_UPLOAD_SIZE_EXCEEDED",
                "File exceeds the maximum allowable upload limit of 2GB");
    }

    @ExceptionHandler(DuplicateFileException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateFileException(DuplicateFileException exc) {
        log.warn("[WARN] [UPLOAD] [DUP] Upload conflict: Duplicate SHA-256 {}", exc.getSha256Hash());

        Map<String, Object> body = errorBody(
                "DUPLICATE_FILE",
                "Duplicate file already exists in your vault");
        body.put("sha256", exc.getSha256Hash());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(StorageQuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleQuotaException(StorageQuotaExceededException exc) {
        log.warn("[WARN] [QUOTA] Storage quota exceeded: {}", exc.getMessage());
        return errorResponse(
                HttpStatus.INSUFFICIENT_STORAGE,
                "STORAGE_QUOTA_EXCEEDED",
                "Storage quota exceeded. Your maximum capacity is 10 GB.");
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleFileNotFoundException(FileNotFoundException exc) {
        log.warn("[WARN] [FILE] File not found: {}", exc.getMessage());
        return errorResponse(
                HttpStatus.NOT_FOUND,
                "FILE_NOT_FOUND",
                exc.getMessage() != null ? exc.getMessage() : "File not found");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException exc) {
        log.warn("[WARN] [API] Bad request argument: {}", exc.getMessage());
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                exc.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception exc) {
        log.error("[ERROR] [API] Unhandled exception: {}", exc.getMessage(), exc);
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