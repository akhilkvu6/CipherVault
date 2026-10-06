package com.ciphervault.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Locale;
import java.util.Map;

public final class StorageDetailsBottomSheet {

    private StorageDetailsBottomSheet() {}

    public static void show(@NonNull Context context, @NonNull UserProfileResponse profile) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(
                R.layout.bottom_sheet_storage_details, null, false);
        dialog.setContentView(view);

        TextView tvTotalUsed = view.findViewById(R.id.tvStorageDetailsTotalUsed);
        TextView tvTotalFiles = view.findViewById(R.id.tvStorageDetailsTotalFiles);
        TextView tvAvailable = view.findViewById(R.id.tvStorageDetailsAvailable);
        TextView tvUsagePercent = view.findViewById(R.id.tvStorageDetailsPercentage);

        TextView tvImages = view.findViewById(R.id.tvStorageImages);
        TextView tvVideos = view.findViewById(R.id.tvStorageVideos);
        TextView tvDocuments = view.findViewById(R.id.tvStorageDocuments);
        TextView tvAudio = view.findViewById(R.id.tvStorageAudio);
        TextView tvOther = view.findViewById(R.id.tvStorageOther);

        TextView tvImagesCount = view.findViewById(R.id.tvStorageImagesCount);
        TextView tvVideosCount = view.findViewById(R.id.tvStorageVideosCount);
        TextView tvDocumentsCount = view.findViewById(R.id.tvStorageDocumentsCount);
        TextView tvAudioCount = view.findViewById(R.id.tvStorageAudioCount);
        TextView tvOtherCount = view.findViewById(R.id.tvStorageOtherCount);

        TextView btnClose = view.findViewById(R.id.btnStorageDetailsClose);

        long totalUsed = Math.max(0L, profile.getUsedStorage());
        long storageLimit = profile.getStorageLimit() > 0
                ? profile.getStorageLimit()
                : SessionManager.DEFAULT_LIMIT;
        long available = Math.max(0L, storageLimit - totalUsed);
        double percentage = storageLimit > 0 ? (totalUsed * 100.0) / storageLimit : 0.0;

        if (tvTotalUsed != null) {
            tvTotalUsed.setText(FileUtils.formatStorageSize(context, totalUsed)
                    + " used of " + FileUtils.formatStorageSize(context, storageLimit));
        }

        if (tvTotalFiles != null) {
            tvTotalFiles.setText(profile.getFileCount() + " files");
        }

        if (tvAvailable != null) {
            tvAvailable.setText(FileUtils.formatStorageSize(context, available) + " available");
        }

        if (tvUsagePercent != null) {
            tvUsagePercent.setText(String.format(Locale.US, "%.1f%% used", percentage));
        }

        Map<String, Long> categoryBytes = profile.getCategoryBytes();
        Map<String, Long> categoryCounts = profile.getCategoryCounts();

        long images = getCategoryValue(categoryBytes, "IMAGES", "IMAGE", "images", "image");
        long videos = getCategoryValue(categoryBytes, "VIDEOS", "VIDEO", "videos", "video");
        long documents = getCategoryValue(categoryBytes,
                "DOCUMENTS", "DOCUMENT", "PDFS", "PDF", "documents", "pdfs");
        long audio = getCategoryValue(categoryBytes, "AUDIO", "AUDIOS", "audio", "audios");

        long knownBytes = images + videos + documents + audio;
        long other = Math.max(0L, totalUsed - knownBytes);

        setStorageValue(tvImages, images, context);
        setStorageValue(tvVideos, videos, context);
        setStorageValue(tvDocuments, documents, context);
        setStorageValue(tvAudio, audio, context);
        setStorageValue(tvOther, other, context);

        long imageCount = getCategoryValue(categoryCounts, "IMAGES", "IMAGE", "images", "image");
        long videoCount = getCategoryValue(categoryCounts, "VIDEOS", "VIDEO", "videos", "video");
        long documentCount = getCategoryValue(categoryCounts,
                "DOCUMENTS", "DOCUMENT", "PDFS", "PDF", "documents", "pdfs");
        long audioCount = getCategoryValue(categoryCounts, "AUDIO", "AUDIOS", "audio", "audios");

        setCount(tvImagesCount, imageCount);
        setCount(tvVideosCount, videoCount);
        setCount(tvDocumentsCount, documentCount);
        setCount(tvAudioCount, audioCount);

        long knownCount = imageCount + videoCount + documentCount + audioCount;
        long otherCount = Math.max(0L, profile.getFileCount() - knownCount);

        setCount(tvOtherCount, otherCount);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private static long getCategoryValue(Map<String, Long> values, String... keys) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }

        for (String key : keys) {
            if (key == null) {
                continue;
            }

            Long directValue = values.get(key);
            if (directValue != null) {
                return Math.max(0L, directValue);
            }

            for (Map.Entry<String, Long> entry : values.entrySet()) {
                String entryKey = entry.getKey();
                Long entryValue = entry.getValue();

                if (entryKey != null && entryKey.equalsIgnoreCase(key) && entryValue != null) {
                    return Math.max(0L, entryValue);
                }
            }
        }

        return 0L;
    }

    private static void setStorageValue(TextView textView, long bytes, Context context) {
        if (textView == null) {
            return;
        }

        textView.setText(FileUtils.formatStorageSize(context, bytes));
    }

    private static void setCount(TextView textView, long count) {
        if (textView == null) {
            return;
        }

        textView.setText(String.valueOf(Math.max(0L, count)));
    }
}