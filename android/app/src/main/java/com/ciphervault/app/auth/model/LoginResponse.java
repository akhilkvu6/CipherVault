package com.ciphervault.app.auth.model;

public class LoginResponse {
    private boolean success;
    private String message;
    private String token;
    private String username;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getToken() { return token; }
    public String getUsername() { return username; }
}
