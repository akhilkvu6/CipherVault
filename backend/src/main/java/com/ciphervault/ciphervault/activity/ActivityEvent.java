package com.ciphervault.ciphervault.activity;

import com.ciphervault.ciphervault.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_events", indexes = {
    @Index(name = "idx_activity_user", columnList = "user_id"),
    @Index(name = "idx_activity_user_time", columnList = "user_id, timestamp"),
    @Index(name = "idx_activity_user_type", columnList = "user_id, event_type")
})
public class ActivityEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 500)
    private String summary;

    @Column(name = "file_count")
    private Integer fileCount;

    @Column(name = "total_bytes")
    private Long totalBytes;

    @Column(name = "affected_files", columnDefinition = "TEXT")
    private String affectedFiles;

    @Column(length = 50)
    private String status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    public ActivityEvent() {}

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Integer getFileCount() { return fileCount; }
    public void setFileCount(Integer fileCount) { this.fileCount = fileCount; }
    public Long getTotalBytes() { return totalBytes; }
    public void setTotalBytes(Long totalBytes) { this.totalBytes = totalBytes; }
    public String getAffectedFiles() { return affectedFiles; }
    public void setAffectedFiles(String affectedFiles) { this.affectedFiles = affectedFiles; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

