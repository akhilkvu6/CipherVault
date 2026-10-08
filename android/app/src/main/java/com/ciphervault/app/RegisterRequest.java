package com.ciphervault.app;

public class RegisterRequest {

    private String name;
    private String username;
    private String email;
    private String password;

    public RegisterRequest() {}

    public RegisterRequest(
            String name,
            String username,
            String email,
            String password) {
        this.name = name;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public RegisterRequest(
            String username,
            String email,
            String password) {
        this(username, username, email, password);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}