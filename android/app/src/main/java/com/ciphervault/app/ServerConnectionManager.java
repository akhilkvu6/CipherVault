package com.ciphervault.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Authoritative 5-Server Saved Connection & LRU Priority Manager.
 * Manages up to 5 verified server configurations in persistent LRU order,
 * executes bounded asynchronous health checks across all saved servers on startup,
 * selects the highest-priority reachable endpoint, and provides safe disconnect handling.
 */
public class ServerConnectionManager {

    private static final String TAG = "ConnectionManager";

    public static final int MAX_SAVED_SERVERS = 5;
    public static final long PROBE_TIMEOUT_MS = 2500L;
    private static final String PREF_NAME = "CipherVaultNetwork";
    private static final String KEY_SAVED_SERVERS_JSON = "saved_servers_lru_json";

    private static volatile ServerConnectionManager instance;

    private final SharedPreferences preferences;
    private final Gson gson = new Gson();
    private final Handler mainHandler;
    private final ExecutorService probeExecutor = Executors.newFixedThreadPool(MAX_SAVED_SERVERS);
    private final AtomicLong probeSessionId = new AtomicLong(0);

    public static class ProbeResult {
        public final SavedServer server;
        public final boolean isReachable;
        public final long latencyMs;
        public final int httpCode;
        public final String message;

        public ProbeResult(@NonNull SavedServer server, boolean isReachable, long latencyMs, int httpCode, @NonNull String message) {
            this.server = server;
            this.isReachable = isReachable;
            this.latencyMs = latencyMs;
            this.httpCode = httpCode;
            this.message = message;
        }

        @NonNull
        @Override
        public String toString() {
            return "[" + server.getDisplayAddress() + " -> " + (isReachable ? "UP (" + latencyMs + "ms)" : "DOWN (" + message + ")") + "]";
        }
    }

    public interface MultiProbeCallback {
        void onAllProbesCompleted(@NonNull List<ProbeResult> results, @Nullable SavedServer highestPriorityReachableServer);
    }

    public static ServerConnectionManager getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (ServerConnectionManager.class) {
                if (instance == null) {
                    instance = new ServerConnectionManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public ServerConnectionManager(@NonNull Context context) {
        this(context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE));
    }

    public ServerConnectionManager(@NonNull SharedPreferences preferences) {
        this.preferences = preferences;
        Handler h = null;
        try {
            if (Looper.getMainLooper() != null) {
                h = new Handler(Looper.getMainLooper());
            }
        } catch (Exception ignored) {}
        this.mainHandler = h;
    }

    /**
     * Retrieves the persistent list of saved servers, guaranteed to be sorted by LRU
     * (Index 0 = most recently used, Index 4 = least recently used, max 5 entries).
     */
    @NonNull
    public synchronized List<SavedServer> getSavedServers() {
        String json = preferences.getString(KEY_SAVED_SERVERS_JSON, null);
        List<SavedServer> list = new ArrayList<>();

        if (json != null && !json.trim().isEmpty()) {
            try {
                Type type = new TypeToken<List<SavedServer>>() {}.getType();
                List<SavedServer> loaded = gson.fromJson(json, type);
                if (loaded != null) {
                    list.addAll(loaded);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse saved servers JSON: " + e.getMessage());
            }
        }

        // Migrate legacy single server preference if saved list is empty
        if (list.isEmpty()) {
            String legacyUrl = preferences.getString("server_base_url", null);
            if (legacyUrl != null && !legacyUrl.trim().isEmpty()) {
                SavedServer legacyServer = SavedServer.fromUrl(legacyUrl, System.currentTimeMillis());
                list.add(legacyServer);
                saveServersList(list);
            }
        }

        // Enforce max 5 boundary and LRU sorting if corrupted or exceeded
        if (list.size() > MAX_SAVED_SERVERS) {
            Collections.sort(list, (a, b) -> Long.compare(b.getLastUsed(), a.getLastUsed()));
            while (list.size() > MAX_SAVED_SERVERS) {
                list.remove(list.size() - 1);
            }
            saveServersList(list);
        }

        return list;
    }

    private synchronized void saveServersList(@NonNull List<SavedServer> servers) {
        try {
            String json = gson.toJson(servers);
            preferences.edit().putString(KEY_SAVED_SERVERS_JSON, json).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to persist saved servers: " + e.getMessage());
        }
    }

    /**
     * Records a server as verified and used, moving it to #1 in LRU order.
     * If already present, updates lastUsed timestamp and moves to #1 without duplication.
     * If new and list is at capacity (5), evicts the least recently used server (#5).
     */
    @NonNull
    public synchronized SavedServer recordServerUsed(@NonNull String rawUrl) {
        SavedServer target = SavedServer.fromUrl(rawUrl, System.currentTimeMillis());
        List<SavedServer> current = new ArrayList<>(getSavedServers());

        int existingIndex = -1;
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).isSameEndpoint(target)) {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex >= 0) {
            // Move existing server to #1
            SavedServer existing = current.remove(existingIndex);
            existing.setLastUsed(System.currentTimeMillis());
            current.add(0, existing);
            Log.d(TAG, "Existing server moved to #1: " + existing.getCanonicalUrl());
            target = existing;
        } else {
            // New server: if at capacity 5, evict #5
            if (current.size() >= MAX_SAVED_SERVERS) {
                SavedServer evicted = current.remove(current.size() - 1);
                Log.d(TAG, "LRU server evicted: " + evicted.getCanonicalUrl());
            }
            target.setLastUsed(System.currentTimeMillis());
            current.add(0, target);
            Log.d(TAG, "New server saved at #1: " + target.getCanonicalUrl());
        }

        saveServersList(current);

        // Also update canonical base URL in legacy NetworkPreferences for global compatibility
        preferences.edit().putString("server_base_url", target.getCanonicalUrl()).apply();

        return target;
    }

    /**
     * Forgets a specific server from the saved list.
     */
    public synchronized boolean forgetServer(@NonNull String canonicalUrl) {
        List<SavedServer> current = new ArrayList<>(getSavedServers());
        boolean removed = false;

        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).getCanonicalUrl().equalsIgnoreCase(canonicalUrl)) {
                SavedServer s = current.remove(i);
                Log.d(TAG, "Server forgotten: " + s.getCanonicalUrl());
                removed = true;
                break;
            }
        }

        if (removed) {
            saveServersList(current);
        }
        return removed;
    }

    /**
     * Disconnects from the current server:
     * Clears active server endpoint, but preserves all saved servers AND authenticated user session.
     */
    public synchronized void disconnect(@Nullable Context context) {
        Log.i(TAG, "Disconnect action executed — preserving saved servers list and authentication session");
        if (context != null) {
            ApiClient.setBaseUrl(context, "");
            AuditLogger.log(context, "Disconnect Server", "SUCCESS", "Cleared active server connection; session preserved");
        }
    }

    public synchronized void disconnect() {
        disconnect(null);
    }

    /**
     * Checks ALL saved servers concurrently using bounded health probes.
     * NEVER terminates early upon first success — all candidates receive a ProbeResult.
     * Selects the highest-priority reachable server according to LRU order.
     */
    public void probeAllSavedServers(@NonNull Context context, @NonNull MultiProbeCallback callback) {
        final long sessionId = probeSessionId.incrementAndGet();
        final List<SavedServer> savedList = getSavedServers();

        Log.i(TAG, "Saved server check started (Session " + sessionId + ") with " + savedList.size() + " candidates");

        if (savedList.isEmpty()) {
            mainHandler.post(() -> callback.onAllProbesCompleted(Collections.emptyList(), null));
            return;
        }

        probeExecutor.execute(() -> {
            final Map<String, ProbeResult> resultMap = new HashMap<>();
            final CountDownLatch latch = new CountDownLatch(savedList.size());

            OkHttpClient probeClient = new OkHttpClient.Builder()
                    .connectTimeout(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .readTimeout(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .build();

            for (SavedServer server : savedList) {
                probeExecutor.execute(() -> {
                    long start = System.currentTimeMillis();
                    String healthUrl = server.getCanonicalUrl() + "api/health";
                    Request request = new Request.Builder()
                            .url(healthUrl)
                            .get()
                            .build();

                    ProbeResult result;
                    try (Response response = probeClient.newCall(request).execute()) {
                        long latency = System.currentTimeMillis() - start;
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Server check succeeded: " + server.getDisplayAddress() + " (" + latency + "ms)");
                            result = new ProbeResult(server, true, latency, response.code(), "HTTP 200 OK");
                        } else {
                            Log.d(TAG, "Server check returned error " + response.code() + ": " + server.getDisplayAddress());
                            result = new ProbeResult(server, false, latency, response.code(), "HTTP " + response.code());
                        }
                    } catch (Exception e) {
                        long latency = System.currentTimeMillis() - start;
                        Log.d(TAG, "Server check failed: " + server.getDisplayAddress() + " (" + e.getMessage() + ")");
                        result = new ProbeResult(server, false, latency, 0, e.getMessage() != null ? e.getMessage() : "Unreachable");
                    }

                    synchronized (resultMap) {
                        resultMap.put(server.getCanonicalUrl(), result);
                    }
                    latch.countDown();
                });
            }

            try {
                // Await all probes (bounded by max 4 seconds total)
                latch.await(4000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            // Stale check protection
            if (sessionId != probeSessionId.get()) {
                Log.w(TAG, "Discarding stale probe session results (Session " + sessionId + " superseded by " + probeSessionId.get() + ")");
                return;
            }

            // Compile results strictly in LRU order
            List<ProbeResult> orderedResults = new ArrayList<>();
            SavedServer highestPriorityReachable = null;

            for (SavedServer server : savedList) {
                ProbeResult r = resultMap.get(server.getCanonicalUrl());
                if (r == null) {
                    r = new ProbeResult(server, false, 0, 0, "Timed out");
                }
                orderedResults.add(r);

                // Highest priority reachable is the FIRST reachable server in LRU order
                if (highestPriorityReachable == null && r.isReachable) {
                    highestPriorityReachable = server;
                }
            }

            final SavedServer selected = highestPriorityReachable;
            Log.i(TAG, "All saved servers checked. Active candidate: " +
                    (selected != null ? selected.getDisplayAddress() : "None reachable"));

            if (mainHandler != null) {
                mainHandler.post(() -> callback.onAllProbesCompleted(orderedResults, selected));
            } else {
                callback.onAllProbesCompleted(orderedResults, selected);
            }
        });
    }

    /**
     * Evaluates LRU priority selection deterministically.
     * Selects the FIRST reachable server in LRU order.
     */
    @Nullable
    public static SavedServer selectHighestPriorityReachable(@NonNull List<SavedServer> lruOrderedServers,
                                                             @NonNull Map<String, Boolean> reachabilityMap) {
        for (SavedServer server : lruOrderedServers) {
            Boolean reachable = reachabilityMap.get(server.getCanonicalUrl());
            if (Boolean.TRUE.equals(reachable)) {
                return server;
            }
        }
        return null;
    }

    /**
     * Invalidate any in-flight asynchronous startup probes (e.g. when user manually scans QR or connects).
     */
    public void invalidateProbes() {
        probeSessionId.incrementAndGet();
    }
}
