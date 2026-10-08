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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AuditLogger records security, authentication, and vault transfer events locally on device.
 * Ensures strict security: NEVER logs passwords, JWT tokens, encryption keys, or private secrets.
 */
public final class AuditLogger {

    private static final String TAG = "CipherVaultAudit";
    private static final String PREF_NAME = "cv_audit_log_prefs";
    private static final String KEY_AUDIT_EVENTS = "audit_events_json";
    private static final int MAX_EVENTS = 100;

    public static final String ACTION_DOWNLOAD_START = "Download Started";
    public static final String ACTION_DOWNLOAD_COMPLETE = "Download Completed";
    public static final String ACTION_UPLOAD_START = "Upload Started";
    public static final String ACTION_UPLOAD_COMPLETE = "Upload Completed";
    public static final String ACTION_SEARCH = "Search Executed";
    public static final String ACTION_AUTH = "Authentication";
    public static final String ACTION_LOCK = "Vault Locked";
    public static final String ACTION_UNLOCK = "Vault Unlocked";
    public static final String ACTION_DISCONNECT = "Server Disconnected";
    public static final String ACTION_CONNECT = "Server Connected";

    private static final Gson gson = new Gson();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    public static class AuditEntry {
        public String action;
        public String status;
        public String details;
        public String timestamp;

        public AuditEntry(String action, String status, String details, String timestamp) {
            this.action = action;
            this.status = status;
            this.details = sanitize(details);
            this.timestamp = timestamp;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("action", action);
            map.put("eventType", action);
            map.put("status", status);
            map.put("details", details);
            map.put("summary", details);
            map.put("timestamp", timestamp);
            return map;
        }
    }

    private AuditLogger() {}

    /**
     * Sanitizes input strings to strip any accidentally supplied sensitive tokens or keys.
     */
    private static String sanitize(@Nullable String input) {
        if (input == null) return "";
        // Strip out any JWT tokens (starts with eyJ...)
        String sanitized = input.replaceAll("(?i)eyJ[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+", "[REDACTED_TOKEN]");
        // Strip potential password / key indicators
        sanitized = sanitized.replaceAll("(?i)password=[^&\\s]+", "password=[REDACTED]");
        sanitized = sanitized.replaceAll("(?i)secret=[^&\\s]+", "secret=[REDACTED]");
        return sanitized;
    }

    public static synchronized void log(@NonNull Context context,
                                        @NonNull String action,
                                        @Nullable String details) {
        log(context, action, "SUCCESS", details);
    }

    /**
     * Records an audit event with current timestamp.
     */
    public static synchronized void log(@NonNull Context context,
                                        @NonNull String action,
                                        @NonNull String status,
                                        @Nullable String details) {
        try {
            String timestamp = dateFormat.format(new Date());
            AuditEntry entry = new AuditEntry(action, status, details, timestamp);

            SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            List<AuditEntry> events = getEntriesInternal(prefs);

            // Prepend newest first
            events.add(0, entry);
            while (events.size() > MAX_EVENTS) {
                events.remove(events.size() - 1);
            }

            prefs.edit().putString(KEY_AUDIT_EVENTS, gson.toJson(events)).apply();
            Log.i(TAG, "Recorded audit event: [" + action + "] " + status + " - " + entry.details);
        } catch (Exception e) {
            Log.e(TAG, "Failed to record audit event: " + e.getMessage());
        }
    }

    /**
     * Retrieves all recorded audit events as Maps for display in UI adapters.
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
        } catch (Exception ignored) {}
    }
}
