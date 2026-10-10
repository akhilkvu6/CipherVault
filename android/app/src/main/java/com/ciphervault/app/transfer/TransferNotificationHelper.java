package com.ciphervault.app.transfer;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.ciphervault.app.FileViewerActivity;
import com.ciphervault.app.MainActivity;
import com.ciphervault.app.R;

import java.io.File;
import java.util.Locale;

public class TransferNotificationHelper {

    public static final String CHANNEL_ID_PROGRESS = "ciphervault_transfers_progress";
    public static final String CHANNEL_NAME_PROGRESS = "Transfers Progress";

    public static final String CHANNEL_ID_COMPLETED = "ciphervault_transfers_complete";
    public static final String CHANNEL_NAME_COMPLETED = "Transfer Notifications";

    public static final int NOTIFICATION_ID_FOREGROUND = 10001;

    private static final long THROTTLE_INTERVAL_MS = 350;
    private static long lastProgressUpdateTimeMs = 0;
    private static int lastReportedProgress = -1;

    private static boolean channelsCreated = false;

    public static synchronized void ensureNotificationChannels(Context context) {
        if (channelsCreated || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        // 1. Progress channel (LOW importance = silent progress updates without buzzing)
        NotificationChannel progressChannel = new NotificationChannel(
                CHANNEL_ID_PROGRESS,
                CHANNEL_NAME_PROGRESS,
                NotificationManager.IMPORTANCE_LOW
        );
        progressChannel.setDescription("Live progress notifications for file uploads and downloads");
        progressChannel.setShowBadge(false);
        progressChannel.enableVibration(false);
        progressChannel.setSound(null, null);
        manager.createNotificationChannel(progressChannel);

        // 2. Completed / Failure channel (DEFAULT importance = normal alert on completion)
        NotificationChannel completeChannel = new NotificationChannel(
                CHANNEL_ID_COMPLETED,
                CHANNEL_NAME_COMPLETED,
                NotificationManager.IMPORTANCE_DEFAULT
        );
        completeChannel.setDescription("Notifications for completed and failed file transfers");
        completeChannel.setShowBadge(true);
        manager.createNotificationChannel(completeChannel);

        channelsCreated = true;
    }

    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static Notification buildForegroundNotification(Context context, TransferBatch activeBatch, TransferItem activeItem) {
        ensureNotificationChannels(context);

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.putExtra("EXTRA_OPEN_TRANSFERS_TAB", true);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_PROGRESS)
                .setSmallIcon(R.drawable.ic_notification_transfer)
                .setContentIntent(contentPendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS);

        if (activeBatch != null && activeBatch.getTotalCount() > 1) {
            // Multi-file batch notification
            int total = activeBatch.getTotalCount();
            int completed = activeBatch.getCompletedCount();
            int overallProgress = activeBatch.getOverallProgress();
            TransferType type = activeBatch.getType();
            String actionName = (type == TransferType.UPLOAD) ? "Uploading" : "Downloading";

            builder.setContentTitle(String.format(Locale.US, "%s %d files", actionName, total));

            String contentText;
            if (activeItem != null) {
                int itemIndex = activeBatch.getCurrentItemIndex();
                contentText = String.format(Locale.US, "File %d of %d: %s (%d%%)",
                        itemIndex, total, activeItem.getFileName(), activeItem.getProgressPercentage());
                if (activeItem.getSpeedBytesPerSecond() > 0) {
                    contentText += " • " + activeItem.getFormattedSpeed();
                }
            } else {
                contentText = String.format(Locale.US, "%d of %d completed • Overall: %d%%",
                        completed, total, overallProgress);
            }

            builder.setContentText(contentText);
            builder.setProgress(100, overallProgress, false);

            // Add Cancel action for the batch
            Intent cancelIntent = new Intent(context, TransferService.class);
            cancelIntent.setAction(TransferService.ACTION_CANCEL_BATCH);
            cancelIntent.putExtra(TransferService.EXTRA_BATCH_ID, activeBatch.getId());
            PendingIntent cancelPendingIntent = PendingIntent.getService(
                    context,
                    1,
                    cancelIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            builder.addAction(R.drawable.ic_lucide_x, "Cancel Batch", cancelPendingIntent);

        } else if (activeItem != null) {
            // Single-file transfer notification
            TransferType type = activeItem.getType();
            String title = (type == TransferType.UPLOAD) ? "Uploading" : "Downloading";
            builder.setContentTitle(title);

            int progress = activeItem.getProgressPercentage();
            StringBuilder sb = new StringBuilder();
            sb.append(activeItem.getFileName());
            sb.append(" • ").append(progress).append("%");

            if (activeItem.getSpeedBytesPerSecond() > 0) {
                sb.append(" • ").append(activeItem.getFormattedSpeed());
            }

            if (activeItem.getEtaSeconds() >= 0 && activeItem.getSpeedBytesPerSecond() > 0) {
                sb.append(" • ").append(activeItem.getFormattedEta());
            }

            builder.setContentText(sb.toString());
            builder.setProgress(100, progress, false);

            // Add Cancel action for this item
            Intent cancelIntent = new Intent(context, TransferService.class);
            cancelIntent.setAction(TransferService.ACTION_CANCEL_TRANSFER);
            cancelIntent.putExtra(TransferService.EXTRA_TRANSFER_ID, activeItem.getId());
            PendingIntent cancelPendingIntent = PendingIntent.getService(
                    context,
                    2,
                    cancelIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            builder.addAction(R.drawable.ic_lucide_x, "Cancel", cancelPendingIntent);

        } else {
            builder.setContentTitle("CipherVault Transfer");
            builder.setContentText("Transfer in progress...");
            builder.setProgress(0, 0, true);
        }

        return builder.build();
    }

    public static boolean shouldUpdateProgressNotification(int currentProgress) {
        long now = System.currentTimeMillis();
        if (now - lastProgressUpdateTimeMs >= THROTTLE_INTERVAL_MS ||
                Math.abs(currentProgress - lastReportedProgress) >= 3 ||
                currentProgress >= 100) {
            lastProgressUpdateTimeMs = now;
            lastReportedProgress = currentProgress;
            return true;
        }
        return false;
    }

    public static void postItemCompletedNotification(Context context, TransferItem item) {
        if (!hasNotificationPermission(context)) return;
        ensureNotificationChannels(context);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        int notificationId = (int) (item.getId().hashCode() & 0x7FFFFFFF);

        Intent intent;
        if (item.getType() == TransferType.DOWNLOAD && item.getDownloadedFile() != null && item.getDownloadedFile().exists()) {
            // Open in FileViewerActivity
            intent = new Intent(context, FileViewerActivity.class);
            intent.putExtra(FileViewerActivity.EXTRA_FILE_ID, item.getFileId());
            intent.putExtra(FileViewerActivity.EXTRA_FILE_NAME, item.getFileName());
            intent.putExtra(FileViewerActivity.EXTRA_FILE_SIZE, item.getTotalBytes());
        } else {
            intent = new Intent(context, MainActivity.class);
            intent.putExtra("EXTRA_OPEN_TRANSFERS_TAB", true);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = (item.getType() == TransferType.UPLOAD) ? "Upload complete" : "Download complete";
        String content = (item.getType() == TransferType.UPLOAD)
                ? item.getFileName() + " encrypted & saved to vault"
                : item.getFileName() + " saved to Downloads";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_COMPLETED)
                .setSmallIcon(R.drawable.ic_notification_transfer)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            manager.notify(notificationId, builder.build());
        } catch (SecurityException ignored) {}
    }

    public static void postBatchCompletedNotification(Context context, TransferBatch batch) {
        if (!hasNotificationPermission(context)) return;
        ensureNotificationChannels(context);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        int notificationId = (int) (batch.getId().hashCode() & 0x7FFFFFFF);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("EXTRA_OPEN_TRANSFERS_TAB", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        int total = batch.getTotalCount();
        int completed = batch.getCompletedCount();
        int failed = batch.getFailedCount();

        String typeStr = (batch.getType() == TransferType.UPLOAD) ? "Upload" : "Download";
        String title;
        String content;

        if (failed == 0) {
            title = typeStr + " complete";
            content = String.format(Locale.US, "%d files %s successfully", total,
                    batch.getType() == TransferType.UPLOAD ? "uploaded" : "downloaded");
        } else {
            title = typeStr + " finished";
            content = String.format(Locale.US, "%d succeeded, %d failed", completed, failed);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_COMPLETED)
                .setSmallIcon(R.drawable.ic_notification_transfer)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            manager.notify(notificationId, builder.build());
        } catch (SecurityException ignored) {}
    }

    public static void postItemFailedNotification(Context context, TransferItem item) {
        if (!hasNotificationPermission(context)) return;
        ensureNotificationChannels(context);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        int notificationId = (int) (item.getId().hashCode() & 0x7FFFFFFF);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("EXTRA_OPEN_TRANSFERS_TAB", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String typeStr = (item.getType() == TransferType.UPLOAD) ? "Upload" : "Download";
        String reason = item.getErrorMessage() != null ? item.getErrorMessage() : "Transfer failed";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_COMPLETED)
                .setSmallIcon(R.drawable.ic_notification_transfer)
                .setContentTitle(typeStr + " failed: " + item.getFileName())
                .setContentText(reason)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            manager.notify(notificationId, builder.build());
        } catch (SecurityException ignored) {}
    }

    public static void postItemCancelledNotification(Context context, TransferItem item) {
        if (!hasNotificationPermission(context)) return;
        ensureNotificationChannels(context);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        int notificationId = (int) (item.getId().hashCode() & 0x7FFFFFFF);

        String typeStr = (item.getType() == TransferType.UPLOAD) ? "Upload" : "Download";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_COMPLETED)
                .setSmallIcon(R.drawable.ic_notification_transfer)
                .setContentTitle(typeStr + " cancelled")
                .setContentText(item.getFileName())
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        try {
            manager.notify(notificationId, builder.build());
        } catch (SecurityException ignored) {}
    }

    public static void cancelNotification(Context context, int notificationId) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            try {
                manager.cancel(notificationId);
            } catch (Exception ignored) {}
        }
    }
}
