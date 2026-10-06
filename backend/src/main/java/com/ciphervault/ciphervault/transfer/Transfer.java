package com.ciphervault.ciphervault.transfer;

import com.ciphervault.ciphervault.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfers", indexes = {
    @Index(name = "idx_transfers_user", columnList = "user_id"),
    @Index(name = "idx_transfers_user_type", columnList = "user_id, transfer_type"),
    @Index(name = "idx_transfers_user_time", columnList = "user_id, started_at"),
    @Index(name = "idx_transfers_user_status", columnList = "user_id, status")
})
public class Transfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_type", nullable = false)
    private TransferType transferType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    @Column(name = "total_bytes")
    private Long totalBytes;

    @Column(name = "transferred_bytes")
    private Long transferredBytes;

    @Column(name = "file_ids")
    private String fileIds;

    @Column(name = "file_names", columnDefinition = "TEXT")
    private String fileNames;

    @Column(name = "batch_identifier", length = 100)
    private String batchIdentifier;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Transfer() {}

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) startedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public TransferType getTransferType() { return transferType; }
    public void setTransferType(TransferType transferType) { this.transferType = transferType; }
    public TransferStatus getStatus() { return status; }
    public void setStatus(TransferStatus status) { this.status = status; }
    public Long getTotalBytes() { return totalBytes; }
    public void setTotalBytes(Long totalBytes) { this.totalBytes = totalBytes; }
    public Long getTransferredBytes() { return transferredBytes; }
    public void setTransferredBytes(Long transferredBytes) { this.transferredBytes = transferredBytes; }
    public String getFileIds() { return fileIds; }
    public void setFileIds(String fileIds) { this.fileIds = fileIds; }
    public String getFileNames() { return fileNames; }
    public void setFileNames(String fileNames) { this.fileNames = fileNames; }
    public String getBatchIdentifier() { return batchIdentifier; }
    public void setBatchIdentifier(String batchIdentifier) { this.batchIdentifier = batchIdentifier; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

