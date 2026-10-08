package com.ciphervault.app.transfer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransferBatch {

    private final String id;
    private final TransferType type;
    private final String title;
    private final List<TransferItem> items;
    private TransferState state;
    private final long startedAt;

    public TransferBatch(TransferType type, String title, List<TransferItem> items) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.title = title != null ? title : (type == TransferType.UPLOAD ? "Batch Upload" : "Batch Download");
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.state = TransferState.QUEUED;
        this.startedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public TransferType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public List<TransferItem> getItems() {
        return items;
    }

    public TransferState getState() {
        return state;
    }

    public void setState(TransferState state) {
        this.state = state;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public int getTotalCount() {
        return items.size();
    }

    public int getCompletedCount() {
        int count = 0;
        for (TransferItem item : items) {
            if (item.getState() == TransferState.COMPLETED) {
                count++;
            }
        }
        return count;
    }

    public int getFailedCount() {
        int count = 0;
        for (TransferItem item : items) {
            if (item.getState() == TransferState.FAILED) {
                count++;
            }
        }
        return count;
    }

    public int getCancelledCount() {
        int count = 0;
        for (TransferItem item : items) {
            if (item.getState() == TransferState.CANCELLED) {
                count++;
            }
        }
        return count;
    }

    public long getTotalBytes() {
        long total = 0;
        for (TransferItem item : items) {
            total += item.getTotalBytes();
        }
        return total;
    }

    public long getTransferredBytes() {
        long transferred = 0;
        for (TransferItem item : items) {
            transferred += item.getTransferredBytes();
        }
        return transferred;
    }

    public TransferItem getCurrentItem() {
        for (TransferItem item : items) {
            if (item.getState() == TransferState.UPLOADING ||
                item.getState() == TransferState.DOWNLOADING ||
                item.getState() == TransferState.STARTING ||
                item.getState() == TransferState.QUEUED) {
                return item;
            }
        }
        return items.isEmpty() ? null : items.get(items.size() - 1);
    }

    public int getCurrentItemIndex() {
        for (int i = 0; i < items.size(); i++) {
            TransferItem item = items.get(i);
            if (item.getState() == TransferState.UPLOADING ||
                item.getState() == TransferState.DOWNLOADING ||
                item.getState() == TransferState.STARTING ||
                item.getState() == TransferState.QUEUED) {
                return i + 1;
            }
        }
        return items.size();
    }

    public int getOverallProgress() {
        long total = getTotalBytes();
        if (total > 0) {
            return (int) Math.min(100, Math.max(0, (getTransferredBytes() * 100) / total));
        }
        if (items.isEmpty()) return 100;
        return (getCompletedCount() * 100) / items.size();
    }

    public boolean isFinished() {
        for (TransferItem item : items) {
            if (item.getState() == TransferState.QUEUED ||
                item.getState() == TransferState.STARTING ||
                item.getState() == TransferState.UPLOADING ||
                item.getState() == TransferState.DOWNLOADING) {
                return false;
            }
        }
        return true;
    }

    public void cancel() {
        this.state = TransferState.CANCELLED;
        for (TransferItem item : items) {
            if (item.getState() != TransferState.COMPLETED && item.getState() != TransferState.FAILED) {
                item.cancel();
            }
        }
    }
}
