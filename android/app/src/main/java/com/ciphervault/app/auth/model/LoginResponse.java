package com.ciphervault.app.auth.model;

public class LoginResponse {
    private boolean success;
    private String message;
    private String token;
    private String username;
    private String email;
    private String name;

    public LoginResponse() {
    }

    public LoginResponse(boolean success, String message, String token, String username, String name) {
        this(success, message, token, username, null, name);
    }

    public LoginResponse(boolean success, String message, String token, String username, String email, String name) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.username = username;
        this.email = email;
        this.name = name;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }
}
