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

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        log.warn("Upload rejected: File exceeds configured limit of 200MB");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", "MAX_UPLOAD_SIZE_EXCEEDED");
        body.put("message", "File exceeds the maximum allowable upload limit of 200MB");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    @ExceptionHandler(DuplicateFileException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateFileException(DuplicateFileException exc) {
        log.warn("Upload conflict: Duplicate SHA-256 {}", exc.getSha256Hash());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", "DUPLICATE_FILE");
        body.put("message", "Duplicate file already exists in your vault");
        body.put("sha256", exc.getSha256Hash());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(StorageQuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleQuotaException(StorageQuotaExceededException exc) {
        log.warn("Storage quota exceeded: {}", exc.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", "STORAGE_QUOTA_EXCEEDED");
        body.put("message", "Storage quota exceeded. Your maximum capacity is 1 GB.");
        return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE).body(body);
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleFileNotFoundException(FileNotFoundException exc) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", "FILE_NOT_FOUND");
        body.put("message", exc.getMessage() != null ? exc.getMessage() : "File not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException exc) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", "BAD_REQUEST");
        body.put("message", exc.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception exc) {
        log.error("Unhandled exception: {}", exc.getMessage(), exc);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", "INTERNAL_SERVER_ERROR");
        body.put("message", "An unexpected error occurred processing your request");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
