package com.ciphervault.app.auth.model;

public class RegisterResponse {
    private String message;
    private String username;
    private String email;

    public RegisterResponse() {
    }

    public RegisterResponse(String message, String username, String email) {
        this.message = message;
        this.username = username;
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }
}
