package com.ciphervault.ciphervault.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    private static final long DEFAULT_STORAGE_LIMIT = 10L * 1024L * 1024L * 1024L;
    private static final long DEFAULT_USED_STORAGE = 0L;
    private static final int DEFAULT_TOKEN_VERSION = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private Long storageLimit = DEFAULT_STORAGE_LIMIT;

    @Column(nullable = false)
    private Long usedStorage = DEFAULT_USED_STORAGE;

    @Column(name = "user_key")
    private String userKey;

    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion = DEFAULT_TOKEN_VERSION;

    @Column(name = "profile_photo_path")
    private String profilePhotoPath;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (name == null && username != null) {
            name = username;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getStorageLimit() {
        return storageLimit;
    }

    public void setStorageLimit(Long storageLimit) {
        this.storageLimit = storageLimit;
    }

    public Long getUsedStorage() {
        return usedStorage;
    }

    public void setUsedStorage(Long usedStorage) {
        this.usedStorage = usedStorage;
    }

    public String getUserKey() {
        return userKey;
    }

    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }

    public Integer getTokenVersion() {
        return tokenVersion != null ? tokenVersion : DEFAULT_TOKEN_VERSION;
    }

    public void setTokenVersion(Integer tokenVersion) {
        this.tokenVersion = tokenVersion;
    }

    public String getProfilePhotoPath() {
        return profilePhotoPath;
    }

    public void setProfilePhotoPath(String profilePhotoPath) {
        this.profilePhotoPath = profilePhotoPath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}