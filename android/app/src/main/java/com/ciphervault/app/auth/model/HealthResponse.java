package com.ciphervault.app.auth.model;

public class HealthResponse {
    private String status;
    private String service;
    private String timestamp;
    private java.util.Map<String, String> components;

    public String getStatus() { return status; }
    public String getService() { return service; }
    public String getTimestamp() { return timestamp; }
    public java.util.Map<String, String> getComponents() { return components; }
}
