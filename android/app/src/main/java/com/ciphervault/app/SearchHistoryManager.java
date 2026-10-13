package com.ciphervault.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class SearchHistoryManager {

    private static final String BASE_PREF_NAME = "ciphervault_search_history";
    private static final String KEY_LATEST = "latest_searches";
    private static final String KEY_COUNTS = "search_counts";
    private static final String KEY_DISPLAY_NAMES = "search_display_names";
    private static final int MAX_LATEST = 10;

    // In-memory debounce to prevent duplicate counting on screen recreation / rapid retry
    private static String sLastRecordedQuery = null;
    private static long sLastRecordedTime = 0L;
    private static final long DEBOUNCE_WINDOW_MS = 1500L;

    public static class SearchCountItem implements Comparable<SearchCountItem> {
        public final String query;
        public final int count;

        public SearchCountItem(String query, int count) {
            this.query = query;
            this.count = count;
        }

        @Override
        public int compareTo(SearchCountItem other) {
            int diff = Integer.compare(other.count, this.count); // Descending count
            if (diff != 0) return diff;
            return this.query.compareToIgnoreCase(other.query); // Deterministic tie-breaker
        }
    }

    /**
     * Resolves user-scoped SharedPreferences to ensure one user's searches are never
     * exposed to another authenticated user.
     */
    private static SharedPreferences getPrefs(Context context) {
        String userScope = "guest";
        try {
            SessionManager sm = SessionManager.getInstance(context);
            String username = sm.getUsername();
            if (username != null && !username.trim().isEmpty()) {
                userScope = username.trim().toLowerCase(Locale.ROOT);
            } else {
                String email = sm.getEmail();
                if (email != null && !email.trim().isEmpty()) {
                    userScope = email.trim().toLowerCase(Locale.ROOT);
                }
            }
        } catch (Exception ignored) {}
        // Sanitize preference filename
        userScope = userScope.replaceAll("[^a-zA-Z0-9_.-]", "_");
        return context.getApplicationContext().getSharedPreferences(BASE_PREF_NAME + "_" + userScope, Context.MODE_PRIVATE);
    }

    /**
     * Records an executed search query with case-normalization deduplication,
     * newest-first ordering, and occurrence count aggregation.
     */
    public static synchronized void recordSearch(@NonNull Context context, String rawQuery) {
        if (rawQuery == null) return;
        String query = rawQuery.trim();
        if (query.isEmpty()) return;

        long now = System.currentTimeMillis();
        // Prevent accidental duplicate counting on screen rotation / fast retries
        if (sLastRecordedQuery != null && sLastRecordedQuery.equalsIgnoreCase(query) && (now - sLastRecordedTime < DEBOUNCE_WINDOW_MS)) {
            return;
        }
        sLastRecordedQuery = query;
        sLastRecordedTime = now;

        SharedPreferences prefs = getPrefs(context);

        // 1. Update Latest Searches (Max 10, case-insensitive deduplication, preserving typed display casing)
        List<String> latest = getLatestSearches(context);
        Iterator<String> it = latest.iterator();
        while (it.hasNext()) {
            if (it.next().equalsIgnoreCase(query)) {
                it.remove();
            }
        }
        latest.add(0, query);
        while (latest.size() > MAX_LATEST) {
            latest.remove(latest.size() - 1);
        }

        JSONArray latestArray = new JSONArray(latest);

        // 2. Update Search Counts (Case-normalized key, preserving sensible display casing)
        JSONObject countsObj;
        JSONObject displayObj;
        try {
            countsObj = new JSONObject(prefs.getString(KEY_COUNTS, "{}"));
        } catch (Exception e) {
            countsObj = new JSONObject();
        }
        try {
            displayObj = new JSONObject(prefs.getString(KEY_DISPLAY_NAMES, "{}"));
        } catch (Exception e) {
            displayObj = new JSONObject();
        }

        String normKey = query.toLowerCase(Locale.ROOT);
        int currentCount = countsObj.optInt(normKey, 0);
        try {
            countsObj.put(normKey, currentCount + 1);
            displayObj.put(normKey, query);
        } catch (Exception ignored) {}

        prefs.edit()
                .putString(KEY_LATEST, latestArray.toString())
                .putString(KEY_COUNTS, countsObj.toString())
                .putString(KEY_DISPLAY_NAMES, displayObj.toString())
                .apply();
    }

    /**
     * Retrieves up to 10 unique recent search queries for the currently authenticated user, newest first.
     */
    @NonNull
    public static synchronized List<String> getLatestSearches(@NonNull Context context) {
        List<String> result = new ArrayList<>();
        SharedPreferences prefs = getPrefs(context);
        String json = prefs.getString(KEY_LATEST, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                result.add(arr.getString(i));
            }
        } catch (Exception ignored) {}
        return result;
    }

    /**
     * Retrieves up to limit most frequently executed metadata search queries, sorted descending by count
     * with deterministic tie-breaking.
     */
    @NonNull
    public static synchronized List<SearchCountItem> getTopMostSeenSearches(@NonNull Context context, int limit) {
        List<SearchCountItem> items = new ArrayList<>();
        SharedPreferences prefs = getPrefs(context);
        String countsJson = prefs.getString(KEY_COUNTS, "{}");
        String displayJson = prefs.getString(KEY_DISPLAY_NAMES, "{}");
        try {
            JSONObject countsObj = new JSONObject(countsJson);
            JSONObject displayObj = new JSONObject(displayJson);
            Iterator<String> keys = countsObj.keys();
            while (keys.hasNext()) {
                String normKey = keys.next();
                int cnt = countsObj.optInt(normKey, 0);
                if (cnt > 0) {
                    String display = displayObj.optString(normKey, normKey);
                    items.add(new SearchCountItem(display, cnt));
                }
            }
        } catch (Exception ignored) {}

        Collections.sort(items);
        if (items.size() > limit) {
            return new ArrayList<>(items.subList(0, limit));
        }
        return items;
    }

    /**
     * Clears recent searches only for the current authenticated user.
     */
    public static synchronized void clearRecentSearches(@NonNull Context context) {
        getPrefs(context).edit().remove(KEY_LATEST).apply();
    }

    /**
     * Clears all search history and counts for the current authenticated user.
     */
    public static synchronized void clearSearchHistory(@NonNull Context context) {
        getPrefs(context).edit().clear().apply();
        sLastRecordedQuery = null;
        sLastRecordedTime = 0L;
    }

    public static class SavedSearchItem {
        public final String id;
        public final String name;
        public final String query;
        public final String category;
        public final String cameraModel;
        public final String resolution;
        public final long savedAt;

        public SavedSearchItem(String id, String name, String query, String category, String cameraModel, String resolution, long savedAt) {
            this.id = id;
            this.name = name;
            this.query = query != null ? query : "";
            this.category = category != null ? category : "ALL";
            this.cameraModel = cameraModel != null ? cameraModel : "";
            this.resolution = resolution != null ? resolution : "";
            this.savedAt = savedAt;
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", id);
                obj.put("name", name);
                obj.put("query", query);
                obj.put("category", category);
                obj.put("cameraModel", cameraModel);
                obj.put("resolution", resolution);
                obj.put("savedAt", savedAt);
            } catch (Exception ignored) {}
            return obj;
        }

        public static SavedSearchItem fromJson(JSONObject obj) {
            if (obj == null) return null;
            return new SavedSearchItem(
                    obj.optString("id", String.valueOf(System.currentTimeMillis())),
                    obj.optString("name", "Saved Search"),
                    obj.optString("query", ""),
                    obj.optString("category", "ALL"),
                    obj.optString("cameraModel", ""),
                    obj.optString("resolution", ""),
                    obj.optLong("savedAt", System.currentTimeMillis())
            );
        }
    }

    private static final String KEY_SAVED_SEARCHES = "saved_searches";

    public static synchronized void saveSearch(
            @NonNull Context context,
            String name,
            String query,
            String category,
            String cameraModel,
            String resolution) {
        if (context == null) return;
        List<SavedSearchItem> current = getSavedSearches(context);
        String id = String.valueOf(System.currentTimeMillis());
        String displayName = (name != null && !name.trim().isEmpty())
                ? name.trim()
                : ((query != null && !query.trim().isEmpty()) ? query.trim() : "Custom Filter");

        Iterator<SavedSearchItem> it = current.iterator();
        while (it.hasNext()) {
            SavedSearchItem item = it.next();
            if (item.query.equalsIgnoreCase(query != null ? query : "")
                    && item.category.equalsIgnoreCase(category != null ? category : "ALL")) {
                it.remove();
            }
        }

        current.add(0, new SavedSearchItem(id, displayName, query, category, cameraModel, resolution, System.currentTimeMillis()));

        JSONArray arr = new JSONArray();
        for (SavedSearchItem item : current) {
            arr.put(item.toJson());
        }

        getPrefs(context).edit().putString(KEY_SAVED_SEARCHES, arr.toString()).apply();
    }

    @NonNull
    public static synchronized List<SavedSearchItem> getSavedSearches(@NonNull Context context) {
        List<SavedSearchItem> list = new ArrayList<>();
        if (context == null) return list;
        String json = getPrefs(context).getString(KEY_SAVED_SEARCHES, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                SavedSearchItem item = SavedSearchItem.fromJson(arr.getJSONObject(i));
                if (item != null) list.add(item);
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static synchronized void deleteSavedSearch(@NonNull Context context, String id) {
        if (context == null || id == null) return;
        List<SavedSearchItem> current = getSavedSearches(context);
        Iterator<SavedSearchItem> it = current.iterator();
        boolean changed = false;
        while (it.hasNext()) {
            if (id.equals(it.next().id)) {
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            JSONArray arr = new JSONArray();
            for (SavedSearchItem item : current) {
                arr.put(item.toJson());
            }
            getPrefs(context).edit().putString(KEY_SAVED_SEARCHES, arr.toString()).apply();
        }
    }

    public static synchronized boolean isSearchSaved(@NonNull Context context, String query, String category) {
        if (context == null) return false;
        String q = query != null ? query.trim() : "";
        String cat = category != null ? category.trim() : "ALL";
        for (SavedSearchItem item : getSavedSearches(context)) {
            if (item.query.equalsIgnoreCase(q) && item.category.equalsIgnoreCase(cat)) {
                return true;
            }
        }
        return false;
    }
}
