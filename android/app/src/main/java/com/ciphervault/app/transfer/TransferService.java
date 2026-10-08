package com.ciphervault.app.transfer;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;

public class TransferService extends Service implements TransferListener {

    public static final String ACTION_START = "com.ciphervault.app.transfer.START";
    public static final String ACTION_CANCEL_TRANSFER = "com.ciphervault.app.transfer.CANCEL_TRANSFER";
    public static final String ACTION_CANCEL_BATCH = "com.ciphervault.app.transfer.CANCEL_BATCH";

    public static final String EXTRA_TRANSFER_ID = "extra_transfer_id";
    public static final String EXTRA_BATCH_ID = "extra_batch_id";

    private boolean isForeground = false;

    @Override
    public void onCreate() {
        super.onCreate();
        TransferNotificationHelper.ensureNotificationChannels(this);
        TransferManager.getInstance(this).registerListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_CANCEL_TRANSFER.equals(action)) {
                String transferId = intent.getStringExtra(EXTRA_TRANSFER_ID);
                if (transferId != null) {
                    TransferManager.getInstance(this).cancelTransfer(transferId);
                }
            } else if (ACTION_CANCEL_BATCH.equals(action)) {
                String batchId = intent.getStringExtra(EXTRA_BATCH_ID);
                if (batchId != null) {
                    TransferManager.getInstance(this).cancelBatch(batchId);
                }
            }
        }

        updateForegroundNotification();

        return START_NOT_STICKY;
    }

    private synchronized void updateForegroundNotification() {
        TransferManager manager = TransferManager.getInstance(this);
        TransferBatch activeBatch = manager.getActiveBatch();
        TransferItem activeItem = manager.getActiveItem();

        if (activeBatch != null || activeItem != null) {
            Notification notification = TransferNotificationHelper.buildForegroundNotification(
                    this, activeBatch, activeItem
            );

            if (!isForeground) {
                try {
                    ServiceCompat.startForeground(
                            this,
                            TransferNotificationHelper.NOTIFICATION_ID_FOREGROUND,
                            notification,
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                                    ? android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                                    : 0
                    );
                    isForeground = true;
                } catch (Exception e) {
                    // In case of foreground restrictions on newer Android versions
                }
            } else {
                android.app.NotificationManager nm =
                        (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (nm != null) {
                    try {
                        nm.notify(TransferNotificationHelper.NOTIFICATION_ID_FOREGROUND, notification);
                    } catch (SecurityException ignored) {}
                }
            }
        } else {
            // No active transfers remain
            stopTransferForeground();
        }
    }

    private synchronized void stopTransferForeground() {
        if (isForeground) {
            try {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
            } catch (Exception ignored) {}
            isForeground = false;
        }
        stopSelf();
    }

    @Override
    public void onTransferStateChanged(TransferItem item) {
        updateForegroundNotification();
    }

    @Override
    public void onTransferProgress(TransferItem item) {
        if (TransferNotificationHelper.shouldUpdateProgressNotification(item.getProgressPercentage())) {
            updateForegroundNotification();
        }
    }

    @Override
    public void onBatchProgress(TransferBatch batch) {
        if (TransferNotificationHelper.shouldUpdateProgressNotification(batch.getOverallProgress())) {
            updateForegroundNotification();
        }
    }

    @Override
    public void onBatchCompleted(TransferBatch batch) {
        TransferNotificationHelper.postBatchCompletedNotification(this, batch);
        updateForegroundNotification();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        TransferManager.getInstance(this).unregisterListener(this);
        stopTransferForeground();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
