package com.ciphervault.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * AuditLogger records security, authentication, and vault activity events locally on device.
 * Ensures strict security: NEVER logs passwords, JWT tokens, encryption keys, or private credentials.
 * Persists events to private encrypted/local SharedPreferences across application restarts.
 * Provides deduplication and structured expandable event details.
 */
public final class AuditLogger {

    private static final String TAG = "CipherVaultAudit";
    private static final String PREF_NAME = "cv_audit_log_prefs";
    private static final String KEY_AUDIT_EVENTS = "audit_events_json";
    private static final int MAX_EVENTS = 200;

    // Standardized Action Names
    public static final String ACTION_ACCOUNT_CREATED = "Account Created";
    public static final String ACTION_LOGIN_SUCCESS = "Login Successful";
    public static final String ACTION_LOGIN_FAILED = "Login Failed";
    public static final String ACTION_LOGOUT = "Logout";
    public static final String ACTION_SESSION_EXPIRED = "Session Expired";
    public static final String ACTION_VAULT_LOCKED = "Vault Locked";
    public static final String ACTION_VAULT_UNLOCKED = "Vault Unlocked";
    public static final String ACTION_BIOMETRIC_ENABLED = "Biometric Auth Enabled";
    public static final String ACTION_BIOMETRIC_DISABLED = "Biometric Auth Disabled";
    public static final String ACTION_PASSWORD_CHANGED = "Password Changed";

    public static final String ACTION_UPLOAD_START = "Upload Started";
    public static final String ACTION_UPLOAD_COMPLETE = "File Uploaded";
    public static final String ACTION_UPLOAD_FAILED = "Upload Failed";
    public static final String ACTION_DOWNLOAD_START = "Download Started";
    public static final String ACTION_DOWNLOAD_COMPLETE = "File Downloaded";
    public static final String ACTION_DOWNLOAD_FAILED = "Download Failed";
    public static final String ACTION_FILE_STAGED = "File Staged";
    public static final String ACTION_FILE_PREVIEW = "File Previewed";
    public static final String ACTION_FILE_DECRYPTED = "File Decrypted";
    public static final String ACTION_FILE_DELETED = "File Deleted";
    public static final String ACTION_DUPLICATE_DETECTED = "Duplicate Detected";
    public static final String ACTION_VAULT_PURGED = "Vault Data Deleted";
    public static final String ACTION_ACCOUNT_DELETED = "Account Deleted";

    public static final String ACTION_PROFILE_UPDATED = "Profile Updated";
    public static final String ACTION_PROFILE_PHOTO_UPDATED = "Profile Photo Updated";
    public static final String ACTION_PROFILE_PHOTO_REMOVED = "Profile Photo Removed";
    public static final String ACTION_THEME_CHANGED = "Theme Changed";
    public static final String ACTION_NOTIFICATIONS_CHANGED = "Notification Settings Changed";
    public static final String ACTION_SECURITY_CHANGED = "Security Settings Changed";

    public static final String ACTION_SERVER_CONNECTED = "Server Connected";
    public static final String ACTION_SERVER_DISCONNECTED = "Server Disconnected";
    public static final String ACTION_SERVER_CONFIG_CHANGED = "Server Address Changed";
    public static final String ACTION_CONNECTION_TEST_SUCCESS = "Connection Test Succeeded";
    public static final String ACTION_CONNECTION_TEST_FAILED = "Connection Test Failed";
    public static final String ACTION_ERROR = "Application Error";

    private static final Gson gson = new Gson();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    // Debounce tracker for duplicate prevention (key -> timestampMillis)
    private static final Map<String, Long> lastLoggedTimeMap = new HashMap<>();
    private static final long DEBOUNCE_WINDOW_MS = 1200L;

    public static class BatchItemDetail {
        public String filename;
        public long sizeBytes;
        public String status; // "SUCCESS" or "FAILED"
        public String errorReason;

        public BatchItemDetail() {}

        public BatchItemDetail(String filename, long sizeBytes, String status, String errorReason) {
            this.filename = filename;
            this.sizeBytes = sizeBytes;
            this.status = status;
            this.errorReason = errorReason;
        }
    }

    public static class AuditEntry {
        public String id;
        public String action;
        public String status; // "SUCCESS", "FAILED", "WARNING", "IN_PROGRESS"
        public String details;
        public String timestamp;
        public long timestampMillis;

        // Structured optional details for expandable log cards
        public String primaryEntity;
        public Long fileSize;
        public String fileCategory;
        public String transferDirection; // "UPLOAD", "DOWNLOAD", or null
        public String serverTarget;
        public String settingKey;
        public String oldValue;
        public String newValue;
        public String authMethod; // "PASSWORD", "BIOMETRIC", etc.
        public String errorMessage;
        public String batchId;
        public Integer batchCount;
        public List<BatchItemDetail> batchItems;

        public AuditEntry() {
            this.id = UUID.randomUUID().toString();
            this.timestampMillis = System.currentTimeMillis();
            this.timestamp = dateFormat.format(new Date(this.timestampMillis));
        }

        public AuditEntry(String action, String status, String details, String timestamp) {
            this.id = UUID.randomUUID().toString();
            this.action = action;
            this.status = status != null ? status : "SUCCESS";
            this.details = sanitize(details);
            this.timestamp = timestamp != null ? timestamp : dateFormat.format(new Date());
            this.timestampMillis = System.currentTimeMillis();
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("id", id != null ? id : UUID.randomUUID().toString());
            map.put("action", action);
            map.put("eventType", action);
            map.put("status", status);
            map.put("details", details);
            map.put("summary", details);
            map.put("timestamp", timestamp);
            map.put("timestampMillis", timestampMillis);
            if (primaryEntity != null) map.put("primaryEntity", primaryEntity);
            if (fileSize != null) map.put("fileSize", fileSize);
            if (fileCategory != null) map.put("fileCategory", fileCategory);
            if (transferDirection != null) map.put("transferDirection", transferDirection);
            if (serverTarget != null) map.put("serverTarget", serverTarget);
            if (settingKey != null) map.put("settingKey", settingKey);
            if (oldValue != null) map.put("oldValue", oldValue);
            if (newValue != null) map.put("newValue", newValue);
            if (authMethod != null) map.put("authMethod", authMethod);
            if (errorMessage != null) map.put("errorMessage", errorMessage);
            if (batchId != null) map.put("batchId", batchId);
            if (batchCount != null) map.put("batchCount", batchCount);
            if (batchItems != null && !batchItems.isEmpty()) map.put("batchItems", batchItems);
            return map;
        }
    }

    private AuditLogger() {}

    /**
     * Sanitizes input strings to strip any accidentally supplied sensitive tokens, passwords, or keys.
     */
    public static String sanitize(@Nullable String input) {
        if (input == null) return "";
        // Strip out any JWT tokens (starts with eyJ...)
        String sanitized = input.replaceAll("(?i)eyJ[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+", "[REDACTED_TOKEN]");
        // Strip potential password / key indicators
        sanitized = sanitized.replaceAll("(?i)password=[^&\\s]+", "password=[REDACTED]");
        sanitized = sanitized.replaceAll("(?i)secret=[^&\\s]+", "secret=[REDACTED]");
        sanitized = sanitized.replaceAll("(?i)token=[^&\\s]+", "token=[REDACTED]");
        sanitized = sanitized.replaceAll("(?i)api[_-]?key=[^&\\s]+", "key=[REDACTED]");
        return sanitized;
    }

    public static synchronized void log(@NonNull Context context,
                                        @NonNull String action,
                                        @Nullable String details) {
        log(context, action, "SUCCESS", details);
    }

    public static synchronized void log(@NonNull Context context,
                                        @NonNull String action,
                                        @NonNull String status,
                                        @Nullable String details) {
        AuditEntry entry = new AuditEntry(action, status, details, dateFormat.format(new Date()));
        recordEntry(context, entry);
    }

    public static synchronized void logEvent(@NonNull Context context, @NonNull AuditEntry entry) {
        recordEntry(context, entry);
    }

    /**
     * Records a grouped batch upload or download event with individual item breakdown.
     */
    public static synchronized void logBatch(@NonNull Context context,
                                            @NonNull String action,
                                            @NonNull String status,
                                            @NonNull String batchTitle,
                                            @Nullable List<BatchItemDetail> items) {
        AuditEntry entry = new AuditEntry();
        entry.action = action;
        entry.status = status;
        entry.details = batchTitle;
        if (items != null) {
            entry.batchCount = items.size();
            entry.batchItems = new ArrayList<>(items);
            long totalBytes = 0;
            for (BatchItemDetail item : items) {
                totalBytes += item.sizeBytes;
            }
            if (totalBytes > 0) {
                entry.fileSize = totalBytes;
            }
        }
        recordEntry(context, entry);
    }

    private static synchronized void recordEntry(@NonNull Context context, @NonNull AuditEntry entry) {
        try {
            long now = System.currentTimeMillis();
            String dedupeKey = (entry.action != null ? entry.action : "") + "||" + (entry.details != null ? entry.details : "");

            // Deduplication check: ignore if identical action and details logged within debounce window
            Long lastTime = lastLoggedTimeMap.get(dedupeKey);
            if (lastTime != null && (now - lastTime) < DEBOUNCE_WINDOW_MS) {
                Log.d(TAG, "Suppressed duplicate audit event within debounce window: " + dedupeKey);
                return;
            }
            lastLoggedTimeMap.put(dedupeKey, now);

            entry.details = sanitize(entry.details);
            if (entry.errorMessage != null) entry.errorMessage = sanitize(entry.errorMessage);
            if (entry.timestampMillis <= 0) entry.timestampMillis = now;
            if (entry.timestamp == null) entry.timestamp = dateFormat.format(new Date(now));
            if (entry.id == null) entry.id = UUID.randomUUID().toString();

            SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            List<AuditEntry> events = getEntriesInternal(prefs);

            // Prepend newest first
            events.add(0, entry);
            while (events.size() > MAX_EVENTS) {
                events.remove(events.size() - 1);
            }

            prefs.edit().putString(KEY_AUDIT_EVENTS, gson.toJson(events)).apply();
            Log.i(TAG, "Recorded audit event: [" + entry.action + "] " + entry.status + " - " + entry.details);
        } catch (Exception e) {
            Log.e(TAG, "Failed to record audit event: " + e.getMessage());
        }
    }

    /**
     * Retrieves all recorded audit events as Maps for display in UI adapters.
     * Ordered newest first.
     */
    @NonNull
    public static synchronized List<Map<String, Object>> getAuditEvents(@NonNull Context context) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            List<AuditEntry> events = getEntriesInternal(prefs);
            for (AuditEntry entry : events) {
                result.add(entry.toMap());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load audit events: " + e.getMessage());
        }
        return result;
    }

    @NonNull
    public static synchronized List<AuditEntry> getAuditEntries(@NonNull Context context) {
        try {
            SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            return getEntriesInternal(prefs);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load audit entries: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private static List<AuditEntry> getEntriesInternal(SharedPreferences prefs) {
        String json = prefs.getString(KEY_AUDIT_EVENTS, null);
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type type = new TypeToken<List<AuditEntry>>() {}.getType();
            List<AuditEntry> list = gson.fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Clears local audit history.
     */
    public static synchronized void clearAuditEvents(@NonNull Context context) {
        try {
            SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().remove(KEY_AUDIT_EVENTS).apply();
            lastLoggedTimeMap.clear();
        } catch (Exception ignored) {}
    }
}
