package com.ciphervault.app.transfer;

public interface TransferListener {
    void onTransferStateChanged(TransferItem item);
    void onTransferProgress(TransferItem item);
    void onBatchProgress(TransferBatch batch);
    void onBatchCompleted(TransferBatch batch);
}
