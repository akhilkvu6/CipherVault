package com.ciphervault.app.main.ui;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.ciphervault.app.R;
import java.io.File;

public class TransferNotificationHelper {

    public static final String CHANNEL_ID = "ciphervault_transfers";
    private static final String CHANNEL_NAME = "CipherVault Transfers";

    public static void initChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Progress and completion status of vault uploads and downloads");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void notifyProgress(Context context, int id, String filename, boolean isUpload, int progress) {
        initChannel(context);
        String title = (isUpload ? "Uploading: " : "Downloading: ") + filename;
        String content = progress > 0 ? progress + "% completed" : "Transfer in progress...";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(isUpload ? R.drawable.ic_lucide_upload : R.drawable.ic_lucide_download)
                .setContentTitle(title)
                .setContentText(content)
                .setProgress(100, Math.max(0, Math.min(100, progress)), progress <= 0)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        try {
            NotificationManagerCompat.from(context).notify(id, builder.build());
        } catch (SecurityException ignored) {}
    }

    public static void notifyComplete(Context context, int id, String filename, boolean isUpload, File localFile) {
        initChannel(context);
        String title = (isUpload ? "Upload complete: " : "Download complete: ") + filename;
        String content = isUpload ? "Secured safely in CipherVault" : "Saved to Downloads folder";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_lucide_shield_check)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setOngoing(false)
                .setProgress(0, 0, false)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            NotificationManagerCompat.from(context).notify(id, builder.build());
        } catch (SecurityException ignored) {}
    }

    public static void notifyFailed(Context context, int id, String filename, boolean isUpload, String error) {
        initChannel(context);
        String title = (isUpload ? "Upload failed: " : "Download failed: ") + filename;
        String content = error != null && !error.isEmpty() ? error : "Operation could not be completed";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(isUpload ? R.drawable.ic_lucide_upload : R.drawable.ic_lucide_download)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setOngoing(false)
                .setProgress(0, 0, false)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            NotificationManagerCompat.from(context).notify(id, builder.build());
        } catch (SecurityException ignored) {}
    }
}
