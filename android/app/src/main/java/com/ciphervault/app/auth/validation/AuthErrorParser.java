package com.ciphervault.app.auth.validation;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Parses and sanitizes backend error responses (Section 23).
 */
public final class AuthErrorParser {

    private static final String DEFAULT_GENERIC_ERROR = "An unexpected error occurred. Please try again.";
    private static final String NETWORK_ERROR = "Cannot connect to server. Check your connection URL and network.";

    private AuthErrorParser() {
    }

    @NonNull
    public static String parseError(@NonNull Response<?> response) {
        if (response.code() == 429) {
            int seconds = parseRetryAfter(response);
            return "Too many attempts. Please try again in " + seconds + " seconds.";
        }

        if (response.code() == 401) {
            String msg = extractMessage(response.errorBody());
            return msg != null ? msg : "Invalid email or password";
        }

        String message = extractMessage(response.errorBody());
        if (message != null && !message.trim().isEmpty()) {
            return message;
        }

        if (response.code() >= 500) {
            return "Server is currently unavailable. Please try again later.";
        }

        return DEFAULT_GENERIC_ERROR;
    }

    @NonNull
    public static String parseException(@NonNull Throwable throwable) {
        if (throwable instanceof IOException) {
            return NETWORK_ERROR;
        }
        return DEFAULT_GENERIC_ERROR;
    }

    public static int parseRetryAfter(@NonNull Response<?> response) {
        String header = response.headers().get("Retry-After");
        if (header != null) {
            try {
                int parsed = Integer.parseInt(header.trim());
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return 60; // Reasonable fallback lockout seconds when Retry-After header is omitted
    }

    private static String extractMessage(ResponseBody errorBody) {
        if (errorBody == null) {
            return null;
        }
        try {
            String json = errorBody.string();
            if (json == null || json.trim().isEmpty()) {
                return null;
            }
            JsonObject obj = new com.google.gson.Gson().fromJson(json, JsonObject.class);
            if (obj != null && obj.has("message")) {
                return obj.get("message").getAsString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
