package com.ciphervault.app.auth.model;

import java.util.Map;

public class HealthResponse {
    private String status;
    private String service;
    private String timestamp;
    private Map<String, Object> components;

    public HealthResponse() {
    }

    public HealthResponse(String status, String service, String timestamp, Map<String, Object> components) {
        this.status = status;
        this.service = service;
        this.timestamp = timestamp;
        this.components = components;
    }

    public String getStatus() {
        return status;
    }

    public String getService() {
        return service;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getComponents() {
        return components;
    }

    public boolean isUp() {
        return status != null && "UP".equalsIgnoreCase(status.trim());
    }
}
