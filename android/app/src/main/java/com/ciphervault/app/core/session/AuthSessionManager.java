package com.ciphervault.app.core.session;

import android.content.Context;

public class AuthSessionManager {
    private final SecureTokenStorage tokenStorage;

    public AuthSessionManager(Context context) {
        this.tokenStorage = new SecureTokenStorage(context);
    }

    public boolean hasValidSession() {
        // For Batch 2, presence of a token is treated as an active session locally.
        // Server will ultimately validate it via 401 response if expired.
        String token = tokenStorage.getToken();
        return token != null && !token.trim().isEmpty();
    }

    public void createSession(String token, String username) {
        tokenStorage.saveSession(token, username);
    }

    public String getAuthToken() {
        return tokenStorage.getToken();
    }

    public void logout() {
        tokenStorage.clearSession();
    }
}
