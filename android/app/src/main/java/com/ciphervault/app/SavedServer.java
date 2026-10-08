package com.ciphervault.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.net.URI;
import java.util.Objects;

import okhttp3.HttpUrl;

/**
 * Represents a saved backend server candidate.
 * Contains only non-sensitive network configuration: scheme, host, port, and lastUsed timestamp.
 * Strictly prohibits storage of user credentials, JWTs, encryption keys, or secrets.
 */
public class SavedServer implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String scheme;
    private String host;
    private int port;
    private long lastUsed;

    public SavedServer() {
        // Default constructor for Gson serialization
    }

    public SavedServer(@NonNull String scheme, @NonNull String host, int port, long lastUsed) {
        this.scheme = normalizeScheme(scheme, host);
        this.host = host.trim().toLowerCase();
        this.port = port > 0 && port <= 65535 ? port : 8080;
        this.lastUsed = lastUsed;
        this.id = buildCanonicalUrl(this.scheme, this.host, this.port);
    }

    @NonNull
    public static SavedServer fromUrl(@Nullable String rawUrl, long lastUsed) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return new SavedServer("http", "127.0.0.1", 8080, lastUsed);
        }

        String cleaned = rawUrl.trim();
        if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
            cleaned = "http://" + cleaned;
        }

        HttpUrl parsed = HttpUrl.parse(cleaned);
        if (parsed != null) {
            String host = parsed.host();
            int port = parsed.port();
            String scheme = parsed.scheme();
            return new SavedServer(scheme, host, port, lastUsed);
        }

        // Fallback manual parsing
        try {
            URI uri = URI.create(cleaned);
            String scheme = uri.getScheme() != null ? uri.getScheme() : "http";
            String host = uri.getHost() != null ? uri.getHost() : "127.0.0.1";
            int port = uri.getPort() > 0 ? uri.getPort() : 8080;
            return new SavedServer(scheme, host, port, lastUsed);
        } catch (Exception e) {
            return new SavedServer("http", "127.0.0.1", 8080, lastUsed);
        }
    }

    private static String normalizeScheme(String scheme, String host) {
        String s = (scheme != null ? scheme.trim().toLowerCase() : "http");
        // Force HTTP for private IP ranges and loopback
        if (host != null) {
            String h = host.toLowerCase().trim();
            if (h.equals("localhost") || h.equals("127.0.0.1") || h.startsWith("192.168.")
                    || h.startsWith("10.") || h.startsWith("172.")) {
                return "http";
            }
        }
        return s.equals("https") ? "https" : "http";
    }

    private static String buildCanonicalUrl(String scheme, String host, int port) {
        return scheme + "://" + host + ":" + port + "/";
    }

    @NonNull
    public String getId() {
        if (id == null || id.isEmpty()) {
            id = buildCanonicalUrl(scheme, host, port);
        }
        return id;
    }

    @NonNull
    public String getScheme() {
        return scheme != null ? scheme : "http";
    }

    @NonNull
    public String getHost() {
        return host != null ? host : "127.0.0.1";
    }

    public int getPort() {
        return port > 0 && port <= 65535 ? port : 8080;
    }

    public long getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }

    @NonNull
    public String getCanonicalUrl() {
        return getId();
    }

    @NonNull
    public String getDisplayAddress() {
        return getHost() + ":" + getPort();
    }

    public boolean isSameEndpoint(@Nullable SavedServer other) {
        if (other == null) return false;
        return Objects.equals(this.getHost(), other.getHost()) &&
                this.getPort() == other.getPort() &&
                Objects.equals(this.getScheme(), other.getScheme());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SavedServer that = (SavedServer) o;
        return isSameEndpoint(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getScheme(), getHost(), getPort());
    }

    @NonNull
    @Override
    public String toString() {
        return "SavedServer{" + getCanonicalUrl() + ", lastUsed=" + lastUsed + "}";
    }
}
