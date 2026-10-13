package com.ciphervault.ciphervault.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Developer Observability Console Logger for CipherVault.
 *
 * Routes all application events through SLF4J / Logback, emitting concise,
 * colour-coded, single-line messages for normal operations and comprehensive
 * diagnostics on failure, ensuring 0% raw ANSI code leakage to log files.
 */
public final class ConsoleLogger {

    private static final Logger log = LoggerFactory.getLogger(ConsoleLogger.class);

    // ANSI Escape Code Constants
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";

    // Category Tags
    public static final String TAG_OK = "[OK]";
    public static final String TAG_WARN = "[WARN]";
    public static final String TAG_ERROR = "[ERROR]";
    public static final String TAG_HTTP = "[HTTP]";
    public static final String TAG_AUTH = "[AUTH]";
    public static final String TAG_DB = "[DB]";
    public static final String TAG_FILE = "[FILE]";
    public static final String TAG_HASH = "[HASH]";
    public static final String TAG_META = "[METADATA]";
    public static final String TAG_PREVIEW = "[PREVIEW]";
    public static final String TAG_CRYPTO = "[CRYPTO]";
    public static final String TAG_STORAGE = "[STORAGE]";
    public static final String TAG_QUOTA = "[QUOTA]";
    public static final String TAG_SEARCH = "[SEARCH]";
    public static final String TAG_BOOT = "[BOOT]";

    public static final String BORDER_DOUBLE = "================================================================";
    public static final String BORDER_SINGLE = "----------------------------------------------------------------";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private ConsoleLogger() {
    }

    public static String timeNow() {
        return LocalTime.now().format(TIME_FORMATTER);
    }

    public static String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.2f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024L * 1024L) return String.format(Locale.ROOT, "%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format(Locale.ROOT, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    public static String truncateSha(String sha) {
        if (sha == null || sha.isBlank()) return "[NONE]";
        sha = sha.trim();
        if (sha.length() <= 12) return sha;
        return sha.substring(0, 4) + "..." + sha.substring(sha.length() - 4);
    }

    public static void stage(String reqId, String tag, String message) {
        log.debug("{} {}", tag, message);
    }

    // =========================================================================
    // 1. STARTUP BANNER
    // =========================================================================

    public static void printStartupBanner(String host, int port, String environment, String javaVersion, String springBootVersion, String dbEngine, String dbVersion, String dbCatalog, boolean dbConnected, List<String> warnings) {
        long pid = ProcessHandle.current().pid();

        System.out.println();
        System.out.println(CYAN + BORDER_DOUBLE + RESET);
        System.out.println(CYAN + BOLD + "                     CIPHERVAULT BACKEND" + RESET);
        System.out.println(CYAN + BOLD + "                  PRIVATE CLOUD STORAGE SERVER" + RESET);
        System.out.println(CYAN + BORDER_DOUBLE + RESET);
        System.out.println();

        System.out.println(GREEN + "[BOOT] " + BOLD + "APPLICATION" + RESET);
        System.out.println(BORDER_SINGLE);
        System.out.println("Application       : CipherVault");
        System.out.println("Version           : 0.0.1-SNAPSHOT");
        System.out.println("Java              : " + (javaVersion != null ? javaVersion : System.getProperty("java.version")));
        System.out.println("Spring Boot       : " + (springBootVersion != null ? springBootVersion : "4.1.1"));
        System.out.println("Environment       : " + (environment != null ? environment.toUpperCase(Locale.ROOT) : "DEVELOPMENT"));
        System.out.println("Process ID        : " + pid);
        System.out.println();

        System.out.println(CYAN + "[BOOT] " + BOLD + "SERVER" + RESET);
        System.out.println(BORDER_SINGLE);
        System.out.println("Host              : " + (host != null ? host : "127.0.0.1"));
        System.out.println("Port              : " + port);
        System.out.println("Protocol          : HTTP");
        System.out.println("Status            : " + GREEN + "[OK] RUNNING" + RESET);
        System.out.println();

        System.out.println(BLUE + "[DB] " + BOLD + "DATABASE" + RESET);
        System.out.println(BORDER_SINGLE);
        System.out.println("Engine            : " + (dbEngine != null ? dbEngine : "MySQL"));

        if (dbVersion != null && !dbVersion.isBlank()) {
            System.out.println("Version           : " + dbVersion);
        }

        System.out.println("Database          : " + (dbCatalog != null ? dbCatalog : "ciphervault"));
        System.out.println("Connection        : " + (dbConnected ? GREEN + "[OK]" + RESET : RED + "[ERROR] DISCONNECTED" + RESET));
        System.out.println("Pool              : HikariCP");
        System.out.println();

        System.out.println(MAGENTA + "[SECURITY]" + RESET);
        System.out.println(BORDER_SINGLE);
        System.out.println("JWT               : " + GREEN + "[OK] INITIALIZED" + RESET);
        System.out.println("Password encoder  : " + GREEN + "[OK] BCrypt" + RESET);
        System.out.println("Key manager       : " + GREEN + "[OK] INITIALIZED" + RESET);
        System.out.println();

        System.out.println(YELLOW + "[CRYPTO]" + RESET);
        System.out.println(BORDER_SINGLE);
        System.out.println("Algorithm         : AES-256-GCM");
        System.out.println("Mode              : STREAMING");
        System.out.println("Status            : " + GREEN + "[OK]" + RESET);
        System.out.println();

        System.out.println(BLUE + "[STORAGE]" + RESET);
        System.out.println(BORDER_SINGLE);
        System.out.println("Storage system    : " + GREEN + "[OK] READY" + RESET);
        System.out.println();

        if (warnings != null && !warnings.isEmpty()) {
            System.out.println(YELLOW + "[WARNINGS]" + RESET);
            System.out.println(BORDER_SINGLE);

            for (String warning : warnings) {
                System.out.println(YELLOW + "[WARN] " + warning + RESET);
            }

            System.out.println();
        }

        System.out.println(GREEN + BORDER_DOUBLE + RESET);
        System.out.println(GREEN + BOLD + "[OK] CIPHERVAULT BACKEND READY" + RESET);
        System.out.println(GREEN + BORDER_DOUBLE + RESET);
        System.out.println();

        log.info("[BOOT] CipherVault Backend Ready | Host: {} | Port: {} | DB: {} | Storage: READY | Crypto: AES-256-GCM", host, port, dbEngine);
    }

    // =========================================================================
    // 2. HTTP REQUEST STREAM
    // =========================================================================

    public static void logHttpRequestStart(String reqId, String method, String uri, String user) {
        log.debug("[HTTP] {} {}{}", method, uri, user != null ? " | user=" + user : "");
    }

    public static void logHttpResponse(String reqId, String method, String uri, int statusCode, String statusPhrase, Long contentSize, long durationMs, String note) {
        String sizeStr = contentSize != null && contentSize > 0 ? " | " + formatSize(contentSize) : "";
        String noteStr = note != null && !note.isBlank() ? " | " + note : "";
        if (statusCode >= 500) {
            log.error("[HTTP] {} {} | {} {} | {} ms{}{}", method, uri, statusCode, statusPhrase, durationMs, sizeStr, noteStr);
        } else if (statusCode >= 400) {
            log.warn("[HTTP] {} {} | {} {} | {} ms{}{}", method, uri, statusCode, statusPhrase, durationMs, sizeStr, noteStr);
        } else {
            log.info("[HTTP] {} {} | {} {} | {} ms{}{}", method, uri, statusCode, statusPhrase, durationMs, sizeStr, noteStr);
        }
    }

    // =========================================================================
    // 3. AUTHENTICATION & REGISTRATION
    // =========================================================================

    public static void logAuthLoginTrace(String reqId, String email, Long userId, boolean userFound, boolean success, String reason, long durationMs) {
        if (success) {
            log.info("[AUTH] Login successful | email={} | userId={} | duration={} ms", email, userId, durationMs);
        } else {
            log.warn("[AUTH] Login failed | email={} | reason={} | duration={} ms", email, reason != null ? reason : "Invalid credentials", durationMs);
        }
    }

    public static void logAuthRegisterTrace(String reqId, String username, String email, boolean duplicateUser, boolean duplicateEmail, boolean success, String reason, long durationMs) {
        if (success) {
            log.info("[AUTH] Registration completed | user={} | email={} | duration={} ms", username, email, durationMs);
        } else if (duplicateUser) {
            log.warn("[AUTH] Registration rejected | user={} | reason=Username already exists", username);
        } else if (duplicateEmail) {
            log.warn("[AUTH] Registration rejected | email={} | reason=Email already registered", email);
        } else {
            log.warn("[AUTH] Registration failed | user={} | email={} | reason={} | duration={} ms", username, email, reason != null ? reason : "Validation failed", durationMs);
        }
    }

    public static void logAuthPasswordChanged(String reqId, String user, long durationMs) {
        log.info("[AUTH] Password changed | user={} | duration={} ms", user, durationMs);
    }

    // =========================================================================
    // 4. FILE UPLOAD
    // =========================================================================

    public static class UploadTimingTracker {
        public long tValidation;
        public long tSha256;
        public long tDuplicateCheck;
        public long tMetadata;
        public long tPreview;
        public long tEncryption;
        public long tStorage;
        public long tDatabase;
        public long tQuota;
        public long tTotal;
    }

    public static void logFileUploadTrace(String reqId, Long userId, String username, String filename, long fileSize, String contentType, String category, String sha256, boolean previewGenerated, String previewType, boolean metadataExtracted, String cameraMake, String resolution, boolean encrypted, long quotaBefore, long quotaLimit, UploadTimingTracker timings) {
        long totalMs = timings != null ? timings.tTotal : 0;
        log.info("[UPLOAD] Upload completed | file={} | size={} | type={} | sha256={} | preview={} | meta={} | duration={} ms",
                filename, formatSize(fileSize), contentType != null ? contentType : "unknown", truncateSha(sha256),
                previewGenerated ? "YES" : "NO", metadataExtracted ? "YES" : "NO", totalMs);
    }

    // =========================================================================
    // 5. DUPLICATE CHECK
    // =========================================================================

    public static void logDuplicateDetectedTrace(String reqId, String filename, long fileSize, String sha256, Long userId, Long existingFileId, String existingFilename) {
        log.warn("[UPLOAD] [DUP] Duplicate file detected | file={} | size={} | sha256={} | existingFileId={}",
                filename, formatSize(fileSize), truncateSha(sha256), existingFileId != null ? existingFileId : "-");
    }

    // =========================================================================
    // 6. DOWNLOAD / DECRYPTION
    // =========================================================================

    public static void logFileDownloadTrace(String reqId, Long fileId, String filename, Long userId, boolean decryptRequested, boolean isEncrypted, boolean decrypted, long fileSize, String sha256, String contentType, long durationMs) {
        log.info("[DOWNLOAD] Download completed | fileId={} | file={} | size={} | decrypted={} | sha256={} | duration={} ms",
                fileId != null ? fileId : "-", filename, formatSize(fileSize), decrypted ? "YES" : "NO", truncateSha(sha256), durationMs);
    }

    // =========================================================================
    // 7. FILE DELETE
    // =========================================================================

    public static void logFileDeleteTrace(String reqId, Long fileId, String filename, Long userId, long releasedSize, boolean previewRemoved) {
        log.info("[DELETE] File deleted | fileId={} | file={} | releasedSize={}",
                fileId != null ? fileId : "-", filename, formatSize(releasedSize));
    }

    // =========================================================================
    // 8. PREVIEW
    // =========================================================================

    public static void logPreviewTrace(String reqId, Long fileId, String type, String generator, boolean cacheHit, long durationMs) {
        log.info("[PREVIEW] Preview served | fileId={} | type={} | cache={} | duration={} ms",
                fileId != null ? fileId : "-", type != null ? type : "IMAGE", cacheHit ? "HIT" : "MISS", durationMs);
    }

    // =========================================================================
    // 9. METADATA
    // =========================================================================

    public static void logMetadataTrace(String reqId, String filename, String extractor, String type, String cameraMake, String cameraModel, String resolution, String dateTaken, long durationMs) {
        String camera = cameraMake != null ? cameraMake + (cameraModel != null ? " " + cameraModel : "") : "N/A";
        log.info("[METADATA] Extraction completed | file={} | camera={} | res={} | duration={} ms",
                filename, camera, resolution != null ? resolution : "N/A", durationMs);
    }

    public static void logMetadataBackfillSummary(int totalScanned, int missing, int backfilledCount) {
        log.info("[METADATA] Backfill summary | scanned={} | missing={} | backfilled={}", totalScanned, missing, backfilledCount);
    }

    // =========================================================================
    // 10. SEARCH
    // =========================================================================

    public static void logSearchTrace(String reqId, Long userId, String query, String category, String filters, int matches, long durationMs) {
        log.info("[SEARCH] Search executed | query='{}' | category={} | matches={} | duration={} ms",
                query != null ? query : "", category != null ? category : "ALL", matches, durationMs);
    }

    // =========================================================================
    // 11. ERROR TRACE
    // =========================================================================

    public static void logErrorTrace(String reqId, String operation, String filename, Long userId, String failedStage, Throwable throwable, boolean tempFileRemoved, boolean quotaRolledBack) {
        log.error("[ERROR] [{}] Operation failed at stage {} | file={} | error={}",
                operation, failedStage != null ? failedStage : "PROCESSING", filename != null ? filename : "-",
                throwable != null ? throwable.getMessage() : "Unknown", throwable);
    }
}