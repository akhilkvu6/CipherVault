package com.ciphervault.ciphervault.activity;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for ActivityEvent to ensure clean Jackson serialization,
 * prevent Hibernate LazyInitializationException when open-in-view is disabled,
 * and protect user credentials/internals from exposure.
 */
public record ActivityEventDto(
        Long id,
        EventType eventType,
        LocalDateTime timestamp,
        String summary,
        Integer fileCount,
        Long totalBytes,
        String affectedFiles,
        String status,
        String errorMessage
) {
    public static ActivityEventDto fromEntity(ActivityEvent event) {
        if (event == null) {
            return null;
        }
        return new ActivityEventDto(
                event.getId(),
                event.getEventType(),
                event.getTimestamp(),
                event.getSummary(),
                event.getFileCount(),
                event.getTotalBytes(),
                event.getAffectedFiles(),
                event.getStatus(),
                event.getErrorMessage()
        );
    }

    // Standard getter aliases for JavaBeans compatibility
    public Long getId() { return id(); }
    public EventType getEventType() { return eventType(); }
    public LocalDateTime getTimestamp() { return timestamp(); }
    public String getSummary() { return summary(); }
    public Integer getFileCount() { return fileCount(); }
    public Long getTotalBytes() { return totalBytes(); }
    public String getAffectedFiles() { return affectedFiles(); }
    public String getStatus() { return status(); }
    public String getErrorMessage() { return errorMessage(); }
}
