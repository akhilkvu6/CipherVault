package com.ciphervault.app.main.model;

import java.util.Map;

public class UserProfileResponse {
    public Long id;
    public String username;
    public String email;
    public Long storageLimit;
    public Long usedStorage;
    public Long fileCount;
    public Long encryptedCount;
    public String createdAt;
    public Map<String, Long> categoryBytes;
    public Map<String, Long> categoryCounts;
}