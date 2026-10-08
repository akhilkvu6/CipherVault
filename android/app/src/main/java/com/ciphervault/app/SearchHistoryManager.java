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

public class SearchHistoryManager {

    private static final String PREF_NAME = "ciphervault_search_history";
    private static final String KEY_LATEST = "latest_searches";
    private static final String KEY_COUNTS = "search_counts";
    private static final int MAX_LATEST = 10;

    public static class SearchCountItem implements Comparable<SearchCountItem> {
        public final String query;
        public final int count;

        public SearchCountItem(String query, int count) {
            this.query = query;
            this.count = count;
        }

        @Override
        public int compareTo(SearchCountItem other) {
            int diff = Integer.compare(other.count, this.count); // Descending
            if (diff != 0) return diff;
            return this.query.compareToIgnoreCase(other.query); // Deterministic tie-breaker
        }
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized void recordSearch(@NonNull Context context, String rawQuery) {
        if (rawQuery == null) return;
        String query = rawQuery.trim();
        if (query.isEmpty()) return;

        SharedPreferences prefs = getPrefs(context);

        // 1. Update Latest Searches (Max 10, deduplicated, latest first)
        List<String> latest = getLatestSearches(context);
        latest.remove(query);
        latest.add(0, query);
        while (latest.size() > MAX_LATEST) {
            latest.remove(latest.size() - 1);
        }

        JSONArray latestArray = new JSONArray(latest);

        // 2. Update Search Counts
        JSONObject countsObj;
        try {
            String countsJson = prefs.getString(KEY_COUNTS, "{}");
            countsObj = new JSONObject(countsJson);
        } catch (Exception e) {
            countsObj = new JSONObject();
        }

        int currentCount = countsObj.optInt(query, 0);
        try {
            countsObj.put(query, currentCount + 1);
        } catch (Exception ignored) {}

        prefs.edit()
                .putString(KEY_LATEST, latestArray.toString())
                .putString(KEY_COUNTS, countsObj.toString())
                .apply();
    }

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

    @NonNull
    public static synchronized List<SearchCountItem> getTopMostSeenSearches(@NonNull Context context, int limit) {
        List<SearchCountItem> items = new ArrayList<>();
        SharedPreferences prefs = getPrefs(context);
        String json = prefs.getString(KEY_COUNTS, "{}");
        try {
            JSONObject obj = new JSONObject(json);
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                int cnt = obj.optInt(key, 0);
                if (cnt > 0) {
                    items.add(new SearchCountItem(key, cnt));
                }
            }
        } catch (Exception ignored) {}

        Collections.sort(items);
        if (items.size() > limit) {
            return new ArrayList<>(items.subList(0, limit));
        }
        return items;
    }

    public static synchronized void clearSearchHistory(@NonNull Context context) {
        getPrefs(context).edit().clear().apply();
    }
}
