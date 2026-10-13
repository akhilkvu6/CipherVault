package com.ciphervault.app.auth.model;

public class LoginRequest {
    private final String email;
    private final String username;
    private final String identifier;
    private final String password;

    public LoginRequest(String emailOrUsername, String password) {
        this.email = emailOrUsername;
        this.username = emailOrUsername;
        this.identifier = emailOrUsername;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getPassword() {
        return password;
    }
}
