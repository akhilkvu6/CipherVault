package com.ciphervault.ciphervault.health;

public class HealthResponse {

    private String status;
    private String service;
    private String timestamp;
    private java.util.Map<String, String> components;

    public HealthResponse() {
    }

    public HealthResponse(String status, String service, String timestamp) {
        this.status = status;
        this.service = service;
        this.timestamp = timestamp;
    }

    public HealthResponse(String status, String service, String timestamp, java.util.Map<String, String> components) {
        this.status = status;
        this.service = service;
        this.timestamp = timestamp;
        this.components = components;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public java.util.Map<String, String> getComponents() {
        return components;
    }

    public void setComponents(java.util.Map<String, String> components) {
        this.components = components;
    }
}