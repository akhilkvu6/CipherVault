package com.ciphervault.app.transfer;

import android.net.Uri;

import java.io.File;
import java.util.Locale;
import java.util.UUID;

public class TransferItem {

    private final String id;
    private final String batchId;
    private final TransferType type;
    private final String fileName;
    private final Uri uri; // For uploads
    private final Long fileId; // For downloads
    private long totalBytes;
    private long transferredBytes;
    private TransferState state;
    private long speedBytesPerSecond;
    private long etaSeconds;
    private String errorMessage;
    private File downloadedFile;
    private volatile boolean isCancelled;
    private long startedAt;
    private long completedAt;
    private boolean decrypt = true;

    public TransferItem(String batchId, TransferType type, String fileName, Uri uri, Long fileId, long totalBytes) {
        this(batchId, type, fileName, uri, fileId, totalBytes, true);
    }

    public TransferItem(String batchId, TransferType type, String fileName, Uri uri, Long fileId, long totalBytes, boolean decrypt) {
        this.id = UUID.randomUUID().toString();
        this.batchId = batchId;
        this.type = type;
        this.fileName = fileName != null ? fileName : "Unknown File";
        this.uri = uri;
        this.fileId = fileId;
        this.totalBytes = Math.max(0, totalBytes);
        this.transferredBytes = 0;
        this.state = TransferState.QUEUED;
        this.speedBytesPerSecond = 0;
        this.etaSeconds = -1;
        this.errorMessage = null;
        this.downloadedFile = null;
        this.isCancelled = false;
        this.startedAt = System.currentTimeMillis();
        this.completedAt = 0;
        this.decrypt = decrypt;
    }

    public String getId() {
        return id;
    }

    public String getBatchId() {
        return batchId;
    }

    public TransferType getType() {
        return type;
    }

    public String getFileName() {
        return fileName;
    }

    public Uri getUri() {
        return uri;
    }

    public Long getFileId() {
        return fileId;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public long getTransferredBytes() {
        return transferredBytes;
    }

    public void setTransferredBytes(long transferredBytes) {
        this.transferredBytes = transferredBytes;
    }

    public TransferState getState() {
        return state;
    }

    public void setState(TransferState state) {
        this.state = state;
    }

    public long getSpeedBytesPerSecond() {
        return speedBytesPerSecond;
    }

    public void setSpeedBytesPerSecond(long speedBytesPerSecond) {
        this.speedBytesPerSecond = speedBytesPerSecond;
    }

    public long getEtaSeconds() {
        return etaSeconds;
    }

    public void setEtaSeconds(long etaSeconds) {
        this.etaSeconds = etaSeconds;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public File getDownloadedFile() {
        return downloadedFile;
    }

    public void setDownloadedFile(File downloadedFile) {
        this.downloadedFile = downloadedFile;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void cancel() {
        this.isCancelled = true;
        this.state = TransferState.CANCELLED;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    public int getProgressPercentage() {
        if (totalBytes <= 0) return 0;
        return (int) Math.min(100, Math.max(0, (transferredBytes * 100) / totalBytes));
    }

    public String getFormattedSpeed() {
        if (speedBytesPerSecond <= 0) {
            return "0 KB/s";
        }
        if (speedBytesPerSecond >= 1024 * 1024) {
            return String.format(Locale.US, "%.1f MB/s", speedBytesPerSecond / (1024.0 * 1024.0));
        }
        return String.format(Locale.US, "%d KB/s", speedBytesPerSecond / 1024);
    }

    public String getFormattedEta() {
        if (etaSeconds < 0 || speedBytesPerSecond <= 0) {
            return "Calculating...";
        }
        if (etaSeconds == 0) {
            return "Finishing...";
        }
        long minutes = etaSeconds / 60;
        long seconds = etaSeconds % 60;
        if (minutes >= 60) {
            long hours = minutes / 60;
            minutes = minutes % 60;
            return String.format(Locale.US, "ETA %02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.US, "ETA %02d:%02d", minutes, seconds);
    }

    public boolean isDecrypt() {
        return decrypt;
    }

    public void setDecrypt(boolean decrypt) {
        this.decrypt = decrypt;
    }
}
