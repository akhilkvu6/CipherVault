package com.ciphervault.app.transfer;

public enum TransferState {
    QUEUED,
    STARTING,
    UPLOADING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}
