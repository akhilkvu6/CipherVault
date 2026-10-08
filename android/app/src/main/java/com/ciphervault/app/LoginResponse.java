package com.ciphervault.app;

public class LoginResponse {

    private boolean success;
    private String message;
    private String username;
    private String name;
    private String token;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public String getToken() {
        return token;
    }
}