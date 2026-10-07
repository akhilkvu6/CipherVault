package com.ciphervault.app.main.model;

import com.google.gson.annotations.SerializedName;

public class StorageSummaryResponse {
    public Long totalBytes;
    public Long usedBytes;
    public Long availableBytes;

    @SerializedName(value = "percentage", alternate = {"usagePercentage"})
    public Double percentage;

    public Long fileCount;

    public long getSafeTotalBytes() {
        return totalBytes != null ? totalBytes : 0L;
    }

    public long getSafeUsedBytes() {
        return usedBytes != null ? usedBytes : 0L;
    }

    public long getSafeAvailableBytes() {
        return availableBytes != null ? availableBytes : 0L;
    }

    public double getEffectivePercentage() {
        if (percentage != null && percentage > 0) {
            return percentage;
        }
        long total = getSafeTotalBytes();
        long used = getSafeUsedBytes();
        if (total > 0 && used > 0) {
            return ((double) used / total) * 100.0;
        }
        return 0.0;
    }
}