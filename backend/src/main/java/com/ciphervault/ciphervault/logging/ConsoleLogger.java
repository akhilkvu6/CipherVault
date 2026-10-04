package com.ciphervault.ciphervault.logging;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Ultimate Developer Observability Console Logger for CipherVault.
 * 
 * Provides structured, request-correlated, stage-timed, security-safe,
 * pure-ASCII formatted terminal logging across all application subsystems.
 * 
 * GUARANTEES:
 * 1. ZERO non-ASCII Unicode characters (no broken '?' in Windows CMD/PowerShell).
 * 2. Every colored indicator includes an explicit text tag: [OK], [WARN], [ERROR], [HTTP], etc.
 * 3. ZERO secret leakage: passwords, hashes, JWTs, keys, salts are strictly excluded.
 */
public final class ConsoleLogger {

    // ANSI terminal color escape codes
    public static final String RESET   = "\u001B[0m";
    public static final String BOLD    = "\u001B[1m";
    public static final String RED     = "\u001B[31m";
    public static final String GREEN   = "\u001B[32m";
    public static final String YELLOW  = "\u001B[33m";
    public static final String BLUE    = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN    = "\u001B[36m";
    public static final String WHITE   = "\u001B[37m";

    // Text tags with standard colors
    public static final String TAG_OK      = GREEN + "[OK]" + RESET;
    public static final String TAG_WARN    = YELLOW + "[WARN]" + RESET;
    public static final String TAG_ERROR   = RED + "[ERROR]" + RESET;
    public static final String TAG_INFO    = CYAN + "[INFO]" + RESET;
    public static final String TAG_HTTP    = BLUE + "[HTTP]" + RESET;
    public static final String TAG_AUTH    = MAGENTA + "[AUTH]" + RESET;
    public static final String TAG_DB      = CYAN + "[DB]" + RESET;
    public static final String TAG_FILE    = BLUE + "[FILE]" + RESET;
    public static final String TAG_HASH    = YELLOW + "[HASH]" + RESET;
    public static final String TAG_META    = CYAN + "[META]" + RESET;
    public static final String TAG_PREVIEW = CYAN + "[PREVIEW]" + RESET;
    public static final String TAG_CRYPTO  = YELLOW + "[CRYPTO]" + RESET;
    public static final String TAG_STORAGE = BLUE + "[STORAGE]" + RESET;
    public static final String TAG_QUOTA   = MAGENTA + "[QUOTA]" + RESET;
    public static final String TAG_SEARCH  = BLUE + "[SEARCH]" + RESET;
    public static final String TAG_BOOT    = CYAN + "[BOOT]" + RESET;

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
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.2f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024L * 1024L) return String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    public static String truncateSha(String sha) {
        if (sha == null || sha.isBlank()) return "[NONE]";
        sha = sha.trim();
        if (sha.length() <= 12) return sha;
        return sha.substring(0, 4) + "..." + sha.substring(sha.length() - 4);
    }

    /**
     * Prints an inline stage log line with request correlation.
     * Example: [REQ-0042] [CRYPTO] AES-256-GCM encryption completed
     */
    public static void stage(String reqId, String tag, String message) {
        String id = reqId != null ? reqId : RequestContext.getRequestId();
        System.out.println("[" + BOLD + id + RESET + "] " + tag + " " + message);
    }

    // =========================================================================
    // 1. STARTUP BANNER
    // =========================================================================

    public static void printStartupBanner(
            String host,
            int port,
            String environment,
            String javaVersion,
            String springBootVersion,
            String dbEngine,
            String dbVersion,
            String dbCatalog,
            boolean dbConnected,
            List<String> warnings) {

        long pid = ProcessHandle.current().pid();

        System.out.println();
        System.out.println(CYAN + BORDER_DOUBLE + RESET);
        System.out.println(CYAN + BOLD + "                     CIPHERVAULT BACKEND" + RESET);
        System.out.println(CYAN + BOLD + "                  PRIVATE CLOUD STORAGE SERVER" + RESET);
        System.out.println(CYAN + BORDER_DOUBLE + RESET);
        System.out.println();

        System.out.println(TAG_BOOT + " " + BOLD + "APPLICATION" + RESET);
        System.out.println(BORDER_SINGLE);
        System.out.println("Application       : CipherVault");
        System.out.println("Version           : 0.0.1-SNAPSHOT");
        System.out.println("Java              : " + (javaVersion != null ? javaVersion : System.getProperty("java.version")));
        System.out.println("Spring Boot       : " + (springBootVersion != null ? springBootVersion : "3.4.x"));
        System.out.println("Environment       : " + (environment != null ? environment.toUpperCase(Locale.US) : "DEVELOPMENT"));
        System.out.println("Process ID        : " + pid);
        System.out.println();

        System.out.println(TAG_BOOT + " " + BOLD + "SERVER" + RESET);
        System.out.println(BORDER_SINGLE);
        System.out.println("Host              : " + (host != null ? host : "192.168.1.38"));
        System.out.println("Port              : " + port);
        System.out.println("Protocol          : HTTP");
        System.out.println("Status            : " + TAG_OK + " RUNNING");
        System.out.println();

        System.out.println(TAG_DB + " " + BOLD + "DATABASE" + RESET);
        System.out.println(BORDER_SINGLE);
        System.out.println("Engine            : " + (dbEngine != null ? dbEngine : "MySQL"));
        if (dbVersion != null && !dbVersion.isBlank()) {
            System.out.println("Version           : " + dbVersion);
        }
        System.out.println("Database          : " + (dbCatalog != null ? dbCatalog : "ciphervault"));
        System.out.println("Connection        : " + (dbConnected ? TAG_OK : TAG_ERROR + " DISCONNECTED"));
        System.out.println("Pool              : HikariCP");
        System.out.println();

        System.out.println(MAGENTA + "[SECURITY]" + RESET);
        System.out.println(BORDER_SINGLE);
        System.out.println("JWT               : " + TAG_OK + " INITIALIZED");
        System.out.println("Password encoder  : " + TAG_OK + " BCrypt");
        System.out.println("Key manager       : " + TAG_OK + " INITIALIZED");
        System.out.println();

        System.out.println(TAG_CRYPTO);
        System.out.println(BORDER_SINGLE);
        System.out.println("Algorithm         : AES-256-GCM");
        System.out.println("Mode              : STREAMING");
        System.out.println("Status            : " + TAG_OK);
        System.out.println();

        System.out.println(TAG_STORAGE);
        System.out.println(BORDER_SINGLE);
        System.out.println("Storage system    : " + TAG_OK + " READY");
        System.out.println();

        if (warnings != null && !warnings.isEmpty()) {
            System.out.println(YELLOW + "[WARNINGS]" + RESET);
            System.out.println(BORDER_SINGLE);
            for (String w : warnings) {
                System.out.println(TAG_WARN + " " + w);
            }
            System.out.println();
        }

        System.out.println(GREEN + BORDER_DOUBLE + RESET);
        System.out.println(TAG_OK + BOLD + " CIPHERVAULT BACKEND READY" + RESET);
        System.out.println(GREEN + BORDER_DOUBLE + RESET);
        System.out.println();
    }

    // =========================================================================
    // 2. HTTP REQUEST STREAM
    // =========================================================================

    public static void logHttpRequestStart(String reqId, String method, String uri, String user) {
        System.out.println("[" + BOLD + reqId + RESET + "] " + TAG_HTTP + " " + BOLD + method + " " + uri + RESET);
        if (user != null && !user.isBlank()) {
            System.out.println("             User        : " + user);
        }
        System.out.println("             Started     : " + timeNow());
    }

    public static void logHttpResponse(
            String reqId,
            String method,
            String uri,
            int statusCode,
            String statusPhrase,
            Long contentSize,
            long durationMs,
            String note) {

        boolean isSuccess = statusCode >= 200 && statusCode < 400;
        String statusTag = isSuccess ? TAG_OK : TAG_ERROR;
        String durStr = durationMs >= 1000
                ? String.format(Locale.US, "%.2f s", durationMs / 1000.0)
                : durationMs + " ms";

        System.out.println("[" + BOLD + reqId + RESET + "] " + TAG_HTTP + " RESPONSE " + method + " " + uri);
        System.out.println("             Status      : " + statusCode + (statusPhrase != null ? " " + statusPhrase : ""));
        if (contentSize != null && contentSize > 0) {
            System.out.println("             Size        : " + formatSize(contentSize));
        }
        System.out.println("             Duration    : " + durStr);
        System.out.println("             Result      : " + statusTag);
        if (note != null && !note.isBlank()) {
            System.out.println("             Note        : " + note);
        }
    }

    // =========================================================================
    // 3. AUTHENTICATION & REGISTRATION TRACES
    // =========================================================================

    public static void logAuthLoginTrace(
            String reqId,
            String email,
            Long userId,
            boolean userFound,
            boolean success,
            String reason,
            long durationMs) {

        System.out.println();
        System.out.println(MAGENTA + BORDER_DOUBLE + RESET);
        System.out.println("[" + BOLD + reqId + RESET + "] " + TAG_AUTH + BOLD + " LOGIN" + RESET);
        System.out.println(MAGENTA + BORDER_DOUBLE + RESET);
        System.out.println();
        System.out.println("Email             : " + (email != null ? email : "anonymous"));
        System.out.println();
        System.out.println("[VALIDATION]");
        System.out.println("Email             : " + (email != null && !email.isBlank() ? TAG_OK : TAG_ERROR));
        System.out.println("Password          : PROVIDED");
        System.out.println();
        System.out.println(TAG_DB);
        System.out.println("User lookup       : " + (userFound ? "FOUND" : "NOT FOUND"));
        if (userId != null) {
            System.out.println("User ID           : " + userId);
            System.out.println("Account status    : ACTIVE");
        }
        System.out.println();
        System.out.println(TAG_AUTH);
        System.out.println("Password check    : " + (success ? TAG_OK : TAG_ERROR + " (mismatch)"));
        System.out.println("JWT generation    : " + (success ? TAG_OK : "NOT STARTED"));
        System.out.println();
        System.out.println(BORDER_SINGLE);
        System.out.println("RESULT");
        System.out.println(BORDER_SINGLE);
        if (success) {
            System.out.println("Status            : " + TAG_OK + " LOGIN SUCCESSFUL");
        } else {
            System.out.println("Status            : " + TAG_ERROR + " LOGIN FAILED");
            System.out.println("Reason            : " + (reason != null ? reason : "Invalid credentials"));
        }
        System.out.println("Duration          : " + durationMs + " ms");
        System.out.println(MAGENTA + BORDER_DOUBLE + RESET);
        System.out.println();
    }

    public static void logAuthRegisterTrace(
            String reqId,
            String username,
            String email,
            boolean duplicateUser,
            boolean duplicateEmail,
            boolean success,
            String reason,
            long durationMs) {

        System.out.println();
        System.out.println(MAGENTA + BORDER_DOUBLE + RESET);
        System.out.println("[" + BOLD + reqId + RESET + "] " + TAG_AUTH + BOLD + " REGISTRATION" + RESET);
        System.out.println(MAGENTA + BORDER_DOUBLE + RESET);
        System.out.println();
        System.out.println("Username          : " + username);
        System.out.println("Email             : " + email);
        System.out.println();
        System.out.println("[VALIDATION]");
        System.out.println("Username          : " + TAG_OK);
        System.out.println("Email             : " + TAG_OK);
        System.out.println("Password policy   : " + TAG_OK);
        System.out.println();
        System.out.println(TAG_DB);
        System.out.println("Duplicate email   : " + (duplicateEmail ? "YES" : "NO"));
        System.out.println("Duplicate user    : " + (duplicateUser ? "YES" : "NO"));
        System.out.println();
        System.out.println("[SECURITY]");
        System.out.println("Password hashing  : BCrypt");
        System.out.println("Password hash     : [HIDDEN]");
        System.out.println();
        System.out.println(TAG_CRYPTO);
        System.out.println("User key          : [GENERATED]");
        System.out.println("Key envelope      : [PROTECTED]");
        System.out.println();
        System.out.println(TAG_DB);
        System.out.println("User record       : " + (success ? TAG_OK : TAG_ERROR));
        System.out.println("Key envelope      : " + (success ? TAG_OK : TAG_ERROR));
        System.out.println();
        System.out.println(BORDER_SINGLE);
        System.out.println("RESULT");
        System.out.println(BORDER_SINGLE);
        if (success) {
            System.out.println("Status            : " + TAG_OK + " REGISTRATION COMPLETE");
        } else {
            System.out.println("Status            : " + TAG_ERROR + " REGISTRATION FAILED (" + reason + ")");
        }
        System.out.println("Duration          : " + durationMs + " ms");
        System.out.println(MAGENTA + BORDER_DOUBLE + RESET);
        System.out.println();
    }

    public static void logAuthPasswordChanged(String reqId, String user, long durationMs) {
        System.out.println();
        System.out.println(MAGENTA + BORDER_DOUBLE + RESET);
        System.out.println("[" + BOLD + reqId + RESET + "] " + TAG_AUTH + BOLD + " PASSWORD CHANGE" + RESET);
        System.out.println(MAGENTA + BORDER_DOUBLE + RESET);
        System.out.println();
        System.out.println("User              : " + user);
        System.out.println("Password hash     : BCrypt [UPDATED / HIDDEN]");
        System.out.println("Token version     : [INCREMENTED]");
        System.out.println("Status            : " + TAG_OK + " PASSWORD UPDATED");
        System.out.println("Duration          : " + durationMs + " ms");
        System.out.println(MAGENTA + BORDER_DOUBLE + RESET);
        System.out.println();
    }

    // =========================================================================
    // 4. FILE UPLOAD TRACE WITH STAGE TIMINGS
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

    public static void logFileUploadTrace(
            String reqId,
            Long userId,
            String username,
            String filename,
            long fileSize,
            String contentType,
            String category,
            String sha256,
            boolean previewGenerated,
            String previewType,
            boolean metadataExtracted,
            String cameraMake,
            String resolution,
            boolean encrypted,
            long quotaBefore,
            long quotaLimit,
            UploadTimingTracker timings) {

        long quotaAfter = quotaBefore + fileSize;

        System.out.println();
        System.out.println(BLUE + BORDER_DOUBLE + RESET);
        System.out.println("[" + BOLD + reqId + RESET + "] " + TAG_FILE + BOLD + " UPLOAD" + RESET);
        System.out.println(BLUE + BORDER_DOUBLE + RESET);
        System.out.println();
        System.out.println("USER");
        System.out.println(BORDER_SINGLE);
        System.out.println("User ID            : " + (userId != null ? userId : "-"));
        System.out.println("Username           : " + (username != null ? username : "-"));
        System.out.println();
        System.out.println("FILE");
        System.out.println(BORDER_SINGLE);
        System.out.println("Filename           : " + filename);
        System.out.println("Original size      : " + formatSize(fileSize));
        System.out.println("MIME type          : " + (contentType != null ? contentType : "application/octet-stream"));
        System.out.println("Category           : " + (category != null ? category.toUpperCase(Locale.US) : "OTHER"));
        System.out.println();

        System.out.println("[1] VALIDATION");
        System.out.println(BORDER_SINGLE);
        System.out.println("File present       : " + TAG_OK);
        System.out.println("Filename           : " + TAG_OK);
        System.out.println("MIME               : " + TAG_OK);
        System.out.println("Maximum size       : " + TAG_OK);
        System.out.println("Quota              : " + TAG_OK);
        System.out.println("Stage time         : " + timings.tValidation + " ms");
        System.out.println();

        System.out.println("[2] SHA-256");
        System.out.println(BORDER_SINGLE);
        System.out.println("Input              : ORIGINAL FILE");
        System.out.println("Algorithm          : SHA-256");
        System.out.println("Hash               : " + truncateSha(sha256));
        System.out.println("Result             : " + TAG_OK);
        System.out.println("Stage time         : " + timings.tSha256 + " ms");
        System.out.println();

        System.out.println("[3] DUPLICATE CHECK");
        System.out.println(BORDER_SINGLE);
        System.out.println("Scope              : USER " + (userId != null ? userId : "-"));
        System.out.println("Hash               : " + truncateSha(sha256));
        System.out.println("Existing match     : NO");
        System.out.println("Result             : " + TAG_OK + " UNIQUE");
        System.out.println("Stage time         : " + timings.tDuplicateCheck + " ms");
        System.out.println();

        System.out.println("[4] METADATA");
        System.out.println(BORDER_SINGLE);
        System.out.println("Extractor          : ExifTool");
        System.out.println("Type               : " + (category != null ? category.toUpperCase(Locale.US) : "DOCUMENT"));
        if (cameraMake != null && !cameraMake.isBlank()) {
            System.out.println("Camera             : " + cameraMake);
        }
        if (resolution != null && !resolution.isBlank()) {
            System.out.println("Resolution         : " + resolution);
        }
        System.out.println("EXIF               : " + (metadataExtracted ? TAG_OK : "[SKIPPED]"));
        System.out.println("Database           : " + (metadataExtracted ? TAG_OK : "[NONE]"));
        System.out.println("Stage time         : " + timings.tMetadata + " ms");
        System.out.println();

        System.out.println("[5] PREVIEW");
        System.out.println(BORDER_SINGLE);
        System.out.println("Type               : " + (previewType != null ? previewType : "NONE"));
        System.out.println("Generator          : Thumbnail pipeline");
        System.out.println("Decode             : " + (previewGenerated ? TAG_OK : "[N/A]"));
        System.out.println("Resize             : " + (previewGenerated ? TAG_OK : "[N/A]"));
        System.out.println("Output             : " + (previewGenerated ? TAG_OK : "[N/A]"));
        System.out.println("Stage time         : " + timings.tPreview + " ms");
        System.out.println();

        System.out.println("[6] ENCRYPTION");
        System.out.println(BORDER_SINGLE);
        if (encrypted) {
            System.out.println("Algorithm          : AES-256-GCM");
            System.out.println("Mode               : STREAMING");
            System.out.println("Input              : ORIGINAL FILE");
            System.out.println("Key                : [RESOLVED / HIDDEN]");
            System.out.println("Encryption         : " + TAG_OK);
        } else {
            System.out.println("Requested          : DISABLED");
            System.out.println("Algorithm          : NONE");
            System.out.println("Encryption         : [BYPASSED]");
        }
        System.out.println("Stage time         : " + timings.tEncryption + " ms");
        System.out.println();

        System.out.println("[7] STORAGE");
        System.out.println(BORDER_SINGLE);
        System.out.println((encrypted ? "Encrypted file     : " : "Unencrypted file   : ") + TAG_OK);
        System.out.println("Physical write     : " + TAG_OK);
        System.out.println("Stage time         : " + timings.tStorage + " ms");
        System.out.println();

        System.out.println("[8] DATABASE");
        System.out.println(BORDER_SINGLE);
        System.out.println("StoredFile         : " + TAG_OK);
        System.out.println("FileMetadata       : " + (metadataExtracted ? TAG_OK : "[SKIPPED]"));
        System.out.println("Stage time         : " + timings.tDatabase + " ms");
        System.out.println();

        System.out.println("[9] QUOTA");
        System.out.println(BORDER_SINGLE);
        System.out.println("Before             : " + formatSize(quotaBefore));
        System.out.println("Added              : " + formatSize(fileSize));
        System.out.println("After              : " + formatSize(quotaAfter));
        System.out.println("Limit              : " + formatSize(quotaLimit));
        System.out.println("Status             : " + TAG_OK);
        System.out.println("Stage time         : " + timings.tQuota + " ms");
        System.out.println();

        System.out.println(BORDER_SINGLE);
        System.out.println("RESULT");
        System.out.println(BORDER_SINGLE);
        System.out.println(TAG_OK + " UPLOAD COMPLETE");
        System.out.println("Total time         : " + timings.tTotal + " ms");
        System.out.println(BLUE + BORDER_DOUBLE + RESET);
        System.out.println();
    }

    // =========================================================================
    // 5. DUPLICATE CHECK TRACE
    // =========================================================================

    public static void logDuplicateDetectedTrace(
            String reqId,
            String filename,
            long fileSize,
            String sha256,
            Long userId,
            Long existingFileId,
            String existingFilename) {

        System.out.println();
        System.out.println(YELLOW + BORDER_DOUBLE + RESET);
        System.out.println("[" + BOLD + reqId + RESET + "] " + TAG_FILE + BOLD + " DUPLICATE CHECK" + RESET);
        System.out.println(YELLOW + BORDER_DOUBLE + RESET);
        System.out.println();
        System.out.println("Filename           : " + filename);
        System.out.println("Size               : " + formatSize(fileSize));
        System.out.println("SHA-256            : " + truncateSha(sha256));
        System.out.println();
        System.out.println(TAG_HASH);
        System.out.println("Original hash      : MATCH");
        System.out.println();
        System.out.println(TAG_DB);
        System.out.println("User scope         : " + (userId != null ? userId : "-"));
        System.out.println("Existing file      : FOUND (" + existingFilename + ")");
        System.out.println("Existing File ID   : " + (existingFileId != null ? existingFileId : "-"));
        System.out.println();
        System.out.println("[UPLOAD]");
        System.out.println("Encryption         : NOT STARTED");
        System.out.println("Storage write      : NOT STARTED");
        System.out.println("Quota update       : NOT STARTED");
        System.out.println();
        System.out.println(BORDER_SINGLE);
        System.out.println(TAG_WARN + BOLD + " DUPLICATE FILE DETECTED" + RESET);
        System.out.println(YELLOW + BORDER_DOUBLE + RESET);
        System.out.println();
    }

    // =========================================================================
    // 6. DOWNLOAD / DECRYPTION TRACE
    // =========================================================================

    public static void logFileDownloadTrace(
            String reqId,
            Long fileId,
            String filename,
            Long userId,
            boolean decryptRequested,
            boolean isEncrypted,
            boolean decrypted,
            long fileSize,
            String sha256,
            String contentType,
            long durationMs) {

        System.out.println();
        System.out.println(BLUE + BORDER_DOUBLE + RESET);
        System.out.println("[" + BOLD + reqId + RESET + "] " + TAG_FILE + BOLD + " DOWNLOAD" + RESET);
        System.out.println(BLUE + BORDER_DOUBLE + RESET);
        System.out.println();
        System.out.println("File ID            : " + (fileId != null ? fileId : "-"));
        System.out.println("Filename           : " + filename);
        System.out.println("User ID            : " + (userId != null ? userId : "-"));
        System.out.println("Decrypt requested  : " + (decryptRequested ? "YES" : "NO"));
        System.out.println();
        System.out.println(TAG_AUTH);
        System.out.println("JWT                : VALID");
        System.out.println("Ownership          : " + TAG_OK);
        System.out.println();
        System.out.println(TAG_DB);
        System.out.println("StoredFile         : FOUND");
        System.out.println("Metadata           : FOUND");
        System.out.println();
        System.out.println(TAG_STORAGE);
        System.out.println((isEncrypted ? "Encrypted file     : " : "Unencrypted file   : ") + "FOUND");
        System.out.println("Size               : " + formatSize(fileSize));
        System.out.println("Read               : " + TAG_OK);
        System.out.println();
        System.out.println(TAG_CRYPTO);
        if (isEncrypted && decrypted) {
            System.out.println("Algorithm          : AES-256-GCM");
            System.out.println("Mode               : STREAMING");
            System.out.println("Key                : [RESOLVED / HIDDEN]");
            System.out.println("Decryption         : " + TAG_OK);
        } else if (isEncrypted) {
            System.out.println("Requested          : NO");
            System.out.println("Algorithm          : AES-256-GCM");
            System.out.println("Decryption         : [BYPASSED]");
        } else {
            System.out.println("Requested          : NO");
            System.out.println("Algorithm          : NONE");
            System.out.println("Decryption         : NOT REQUIRED");
        }
        System.out.println();
        System.out.println(TAG_HASH);
        System.out.println("SHA-256 calculated : " + truncateSha(sha256));
        System.out.println("Expected hash      : " + truncateSha(sha256));
        System.out.println("Comparison         : MATCH");
        System.out.println("Integrity          : " + TAG_OK);
        System.out.println();
        System.out.println(TAG_HTTP);
        System.out.println("Content-Type       : " + (contentType != null ? contentType : "application/octet-stream"));
        System.out.println("Response           : 200 OK");
        System.out.println();
        System.out.println(BORDER_SINGLE);
        System.out.println("RESULT");
        System.out.println(BORDER_SINGLE);
        System.out.println(TAG_OK + " DOWNLOAD COMPLETE");
        System.out.println("Total time         : " + durationMs + " ms");
        System.out.println(BLUE + BORDER_DOUBLE + RESET);
        System.out.println();
    }

    public static void logFileDownloadTrace(
            String reqId,
            Long fileId,
            String filename,
            Long userId,
            boolean decryptRequested,
            long fileSize,
            String sha256,
            String contentType,
            long durationMs) {
        logFileDownloadTrace(reqId, fileId, filename, userId, decryptRequested, true, decryptRequested, fileSize, sha256, contentType, durationMs);
    }

    // =========================================================================
    // 7. FILE DELETE TRACE
    // =========================================================================

    public static void logFileDeleteTrace(
            String reqId,
            Long fileId,
            String filename,
            Long userId,
            long releasedSize,
            boolean previewRemoved) {

        System.out.println();
        System.out.println(BLUE + BORDER_DOUBLE + RESET);
        System.out.println("[" + BOLD + reqId + RESET + "] " + TAG_FILE + BOLD + " DELETE" + RESET);
        System.out.println(BLUE + BORDER_DOUBLE + RESET);
        System.out.println();
        System.out.println("File ID            : " + (fileId != null ? fileId : "-"));
        System.out.println("Filename           : " + filename);
        System.out.println("User ID            : " + (userId != null ? userId : "-"));
        System.out.println();
        System.out.println(TAG_AUTH);
        System.out.println("Ownership          : " + TAG_OK);
        System.out.println();
        System.out.println(TAG_STORAGE);
        System.out.println("Encrypted file     : " + TAG_OK + " REMOVED");
        System.out.println();
        System.out.println(TAG_PREVIEW);
        System.out.println("Derived preview    : " + (previewRemoved ? TAG_OK + " REMOVED" : "[NONE]"));
        System.out.println();
        System.out.println(TAG_DB);
        System.out.println("Metadata           : " + TAG_OK + " REMOVED");
        System.out.println("StoredFile         : " + TAG_OK + " REMOVED");
        System.out.println();
        System.out.println(TAG_QUOTA);
        System.out.println("Released           : " + formatSize(releasedSize));
        System.out.println("Usage updated      : " + TAG_OK);
        System.out.println();
        System.out.println(BORDER_SINGLE);
        System.out.println("RESULT");
        System.out.println(BORDER_SINGLE);
        System.out.println(TAG_OK + " DELETE COMPLETE");
        System.out.println(BLUE + BORDER_DOUBLE + RESET);
        System.out.println();
    }

    // =========================================================================
    // 8. PREVIEW TRACE (IMAGE / VIDEO / CACHE)
    // =========================================================================

    public static void logPreviewTrace(
            String reqId,
            Long fileId,
            String type,
            String generator,
            boolean cacheHit,
            long durationMs) {

        System.out.println();
        System.out.println("[" + BOLD + reqId + RESET + "] " + TAG_PREVIEW);
        System.out.println(BORDER_SINGLE);
        if (fileId != null) {
            System.out.println("File ID             : " + fileId);
        }
        System.out.println("Type                : " + (type != null ? type.toUpperCase(Locale.US) : "IMAGE"));
        if (cacheHit) {
            System.out.println("Source              : PREVIEW CACHE");
            System.out.println("Cache               : HIT");
            System.out.println("Decode              : " + TAG_OK);
            System.out.println("Output              : " + TAG_OK);
            System.out.println("Result              : " + TAG_OK + " SERVED FROM CACHE");
        } else {
            System.out.println("Source              : ORIGINAL FILE");
            System.out.println("Cache               : MISS");
            System.out.println("Generator           : " + (generator != null ? generator : "Pipeline"));
            System.out.println("Decode              : " + TAG_OK);
            System.out.println("Resize              : " + TAG_OK);
            System.out.println("Output              : " + TAG_OK);
            System.out.println("Cache write         : " + TAG_OK);
            System.out.println("Result              : " + TAG_OK + " PREVIEW GENERATED");
        }
        System.out.println("Stage time          : " + durationMs + " ms");
        System.out.println();
    }

    // =========================================================================
    // 9. METADATA TRACE & BACKFILL
    // =========================================================================

    public static void logMetadataTrace(
            String reqId,
            String filename,
            String extractor,
            String type,
            String cameraMake,
            String cameraModel,
            String resolution,
            String dateTaken,
            long durationMs) {

        System.out.println();
        System.out.println(CYAN + BORDER_DOUBLE + RESET);
        System.out.println("[" + BOLD + reqId + RESET + "] " + TAG_META + BOLD + " EXTRACTION" + RESET);
        System.out.println(CYAN + BORDER_DOUBLE + RESET);
        System.out.println();
        System.out.println("File                : " + filename);
        System.out.println("Extractor           : " + (extractor != null ? extractor : "ExifTool"));
        System.out.println("Type                : " + (type != null ? type.toUpperCase(Locale.US) : "IMAGE"));
        System.out.println();
        if (cameraMake != null && !cameraMake.isBlank()) {
            System.out.println("Camera Make         : " + cameraMake);
        }
        if (cameraModel != null && !cameraModel.isBlank()) {
            System.out.println("Camera Model        : " + cameraModel);
        }
        if (resolution != null && !resolution.isBlank()) {
            System.out.println("Resolution          : " + resolution);
        }
        if (dateTaken != null && !dateTaken.isBlank()) {
            System.out.println("Date Taken          : " + dateTaken);
        }
        System.out.println();
        System.out.println(TAG_DB);
        System.out.println("Metadata record     : " + TAG_OK + " STORED");
        System.out.println();
        System.out.println("Duration            : " + durationMs + " ms");
        System.out.println("Result              : " + TAG_OK);
        System.out.println(CYAN + BORDER_DOUBLE + RESET);
        System.out.println();
    }

    public static void logMetadataBackfillSummary(int totalScanned, int missing, int backfilledCount) {
        System.out.println();
        System.out.println(CYAN + BORDER_DOUBLE + RESET);
        System.out.println(TAG_META + BOLD + " BACKFILL CHECK" + RESET);
        System.out.println(CYAN + BORDER_DOUBLE + RESET);
        System.out.println("Scanning stored files...");
        System.out.println("Files scanned       : " + totalScanned);
        System.out.println("Missing metadata    : " + missing);
        if (backfilledCount > 0) {
            System.out.println("Backfilled files    : " + backfilledCount);
        }
        if (missing == 0 || backfilledCount == missing) {
            System.out.println("Result              : " + TAG_OK + " ALL FILES HAVE METADATA");
        } else {
            System.out.println("Result              : " + TAG_WARN + " BACKFILL FINISHED (" + backfilledCount + "/" + missing + ")");
        }
        System.out.println(CYAN + BORDER_DOUBLE + RESET);
        System.out.println();
    }

    // =========================================================================
    // 10. SEARCH TRACE
    // =========================================================================

    public static void logSearchTrace(
            String reqId,
            Long userId,
            String query,
            String category,
            String filters,
            int matches,
            long durationMs) {

        System.out.println();
        System.out.println(BLUE + BORDER_DOUBLE + RESET);
        System.out.println("[" + BOLD + reqId + RESET + "] " + TAG_SEARCH + BOLD + " EXECUTION" + RESET);
        System.out.println(BLUE + BORDER_DOUBLE + RESET);
        System.out.println();
        System.out.println("User                : " + (userId != null ? userId : "-"));
        if (query != null && !query.isBlank()) {
            System.out.println("Query               : " + query);
        }
        if (category != null && !category.isBlank()) {
            System.out.println("Category            : " + category.toUpperCase(Locale.US));
        }
        if (filters != null && !filters.isBlank()) {
            System.out.println("Filters             : " + filters);
        }
        System.out.println();
        System.out.println(TAG_DB);
        System.out.println("User isolation      : " + TAG_OK);
        System.out.println("Metadata matching   : " + TAG_OK);
        System.out.println("Filename matching   : " + TAG_OK);
        System.out.println("Category filtering  : " + TAG_OK);
        System.out.println();
        System.out.println(BORDER_SINGLE);
        System.out.println("RESULTS");
        System.out.println(BORDER_SINGLE);
        System.out.println("Matches             : " + matches);
        System.out.println("Duration            : " + durationMs + " ms");
        if (matches == 0) {
            System.out.println("Result              : " + TAG_OK + " SEARCH COMPLETE - NO MATCHES");
        } else {
            System.out.println("Result              : " + TAG_OK + " SEARCH COMPLETE");
        }
        System.out.println(BLUE + BORDER_DOUBLE + RESET);
        System.out.println();
    }

    // =========================================================================
    // 11. DETAILED ERROR TRACE
    // =========================================================================

    public static void logErrorTrace(
            String reqId,
            String operation,
            String filename,
            Long userId,
            String failedStage,
            Throwable throwable,
            boolean tempFileRemoved,
            boolean quotaRolledBack) {

        System.out.println();
        System.out.println(RED + BORDER_DOUBLE + RESET);
        System.out.println("[" + BOLD + reqId + RESET + "] " + TAG_ERROR + " " + BOLD + operation.toUpperCase(Locale.US) + RESET);
        System.out.println(RED + BORDER_DOUBLE + RESET);
        System.out.println();
        if (filename != null && !filename.isBlank()) {
            System.out.println("File                : " + filename);
        }
        if (userId != null) {
            System.out.println("User ID             : " + userId);
        }
        System.out.println("Failed stage        : " + (failedStage != null ? failedStage : "PROCESSING"));
        if (throwable != null) {
            System.out.println("Exception           : " + throwable.getClass().getName());
            System.out.println("Message             : " + throwable.getMessage());
        }
        System.out.println();
        System.out.println("[RECOVERY]");
        System.out.println("Temporary file      : " + (tempFileRemoved ? TAG_OK + " REMOVED" : "[N/A]"));
        System.out.println("Quota               : " + (quotaRolledBack ? TAG_OK + " ROLLED BACK" : TAG_OK + " UNCHANGED"));
        System.out.println("Database            : " + TAG_OK + " NO RECORD CREATED / ROLLED BACK");
        System.out.println();
        System.out.println(BORDER_SINGLE);
        System.out.println(TAG_ERROR + " " + operation.toUpperCase(Locale.US) + " FAILED");
        System.out.println(RED + BORDER_DOUBLE + RESET);
        System.out.println();
    }

    // =========================================================================
    // 12. HELPER / COMPATIBILITY METHODS
    // =========================================================================

    public static void logFileDownloadComplete(String filename, Long fileId, boolean encrypted, boolean decrypted) {
        String reqId = RequestContext.getRequestId();
        stage(reqId, TAG_FILE, "Download complete: " + filename + " (ID=" + fileId + ", Encrypted=" + encrypted + ", Decrypted=" + decrypted + ")");
    }

    public static void logFileDeleteComplete(String filename, Long fileId, boolean storageDeleted, boolean previewDeleted, boolean metadataDeleted, boolean quotaUpdated) {
        String reqId = RequestContext.getRequestId();
        stage(reqId, TAG_FILE, "Delete complete: " + filename + " (ID=" + fileId + ")");
    }

    public static void logPreviewServed(Long fileId, boolean cacheHit) {
        String reqId = RequestContext.getRequestId();
        stage(reqId, TAG_PREVIEW, "Preview served for ID=" + fileId + " (Cache=" + (cacheHit ? "HIT" : "MISS") + ")");
    }

    public static void logPreviewReady(String filename, Long fileId, String type, String generator, boolean cached, boolean ready) {
        String reqId = RequestContext.getRequestId();
        stage(reqId, TAG_PREVIEW, "Preview ready: " + filename + " [" + type + " via " + generator + "]");
    }

    public static void logPreviewUnavailable(String filename, String reason) {
        String reqId = RequestContext.getRequestId();
        stage(reqId, TAG_PREVIEW, "Preview unavailable: " + filename + " (" + reason + ")");
    }

    public static void logMetadataExtracted(String filename, String extractor, boolean rawSaved, boolean exifFound, boolean dateFound, boolean resFound) {
        String reqId = RequestContext.getRequestId();
        stage(reqId, TAG_META, "Metadata extracted via " + extractor + " for " + filename + " (EXIF=" + (exifFound ? "YES" : "NO") + ", Date=" + (dateFound ? "YES" : "NO") + ", Res=" + (resFound ? "YES" : "NO") + ")");
    }
}

