package com.ciphervault.app.transfer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import com.ciphervault.app.ApiClient;
import com.ciphervault.app.ApiService;
import com.ciphervault.app.FileUtils;
import com.ciphervault.app.StoredFile;
import com.ciphervault.app.StreamingProgressRequestBody;
import com.ciphervault.app.UploadResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class TransferManager {

    private static volatile TransferManager instance;

    private final Context appContext;
    private final ApiService apiService;
    private final ExecutorService transferExecutor;
    private final Handler mainHandler;

    private final List<TransferBatch> batches = new CopyOnWriteArrayList<>();
    private final List<TransferItem> allTransfers = new CopyOnWriteArrayList<>();
    private final List<TransferListener> listeners = new CopyOnWriteArrayList<>();

    private TransferManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.apiService = ApiClient.getApiService(appContext);
        this.transferExecutor = Executors.newFixedThreadPool(2);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static TransferManager getInstance(Context context) {
        if (instance == null) {
            synchronized (TransferManager.class) {
                if (instance == null) {
                    instance = new TransferManager(context);
                }
            }
        }
        return instance;
    }

    public void registerListener(TransferListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregisterListener(TransferListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void notifyStateChanged(TransferItem item) {
        mainHandler.post(() -> {
            for (TransferListener listener : listeners) {
                listener.onTransferStateChanged(item);
            }
        });
    }

    private void notifyProgress(TransferItem item) {
        mainHandler.post(() -> {
            for (TransferListener listener : listeners) {
                listener.onTransferProgress(item);
            }
        });
    }

    private void notifyBatchProgress(TransferBatch batch) {
        mainHandler.post(() -> {
            for (TransferListener listener : listeners) {
                listener.onBatchProgress(batch);
            }
        });
    }

    private void notifyBatchCompleted(TransferBatch batch) {
        mainHandler.post(() -> {
            for (TransferListener listener : listeners) {
                listener.onBatchCompleted(batch);
            }
        });
    }

    private void startTransferService() {
        Intent intent = new Intent(appContext, TransferService.class);
        intent.setAction(TransferService.ACTION_START);
        try {
            ContextCompat.startForegroundService(appContext, intent);
        } catch (Exception e) {
            // Background start restrictions fallback
        }
    }

    // --- ENQUEUE UPLOADS ---

    public TransferBatch enqueueUploadItems(String batchTitle, List<TransferItem> items) {
        if (items == null || items.isEmpty()) return null;

        TransferBatch batch = new TransferBatch(TransferType.UPLOAD, batchTitle, items);
        batches.add(0, batch);
        allTransfers.addAll(0, items);

        startTransferService();

        transferExecutor.execute(() -> processBatch(batch));
        return batch;
    }

    // --- ENQUEUE DOWNLOADS ---

    public TransferItem enqueueDownload(StoredFile file) {
        return enqueueDownload(file, true);
    }

    public TransferItem enqueueDownload(StoredFile file, boolean decrypt) {
        if (file == null || file.getId() == null) return null;

        String dlName = file.getOriginalFilename();
        if (!decrypt && !dlName.toLowerCase().endsWith(".enc")) {
            dlName = dlName + ".enc";
        }

        TransferItem item = new TransferItem(
                null,
                TransferType.DOWNLOAD,
                dlName,
                null,
                file.getId(),
                file.getFileSize(),
                decrypt
        );

        List<TransferItem> list = Collections.singletonList(item);
        TransferBatch batch = new TransferBatch(TransferType.DOWNLOAD, "Download: " + item.getFileName(), list);

        batches.add(0, batch);
        allTransfers.add(0, item);

        startTransferService();
        transferExecutor.execute(() -> processBatch(batch));
        return item;
    }

    public TransferBatch enqueueDownloadBatch(List<StoredFile> files) {
        return enqueueDownloadBatch(files, true);
    }

    public TransferBatch enqueueDownloadBatch(List<StoredFile> files, boolean decrypt) {
        if (files == null || files.isEmpty()) return null;

        List<TransferItem> items = new ArrayList<>();
        for (StoredFile file : files) {
            if (file != null && file.getId() != null) {
                String dlName = file.getOriginalFilename();
                if (!decrypt && !dlName.toLowerCase().endsWith(".enc")) {
                    dlName = dlName + ".enc";
                }
                items.add(new TransferItem(
                        null,
                        TransferType.DOWNLOAD,
                        dlName,
                        null,
                        file.getId(),
                        file.getFileSize(),
                        decrypt
                ));
            }
        }

        if (items.isEmpty()) return null;

        TransferBatch batch = new TransferBatch(
                TransferType.DOWNLOAD,
                "Batch Download (" + items.size() + " files)",
                items
        );

        batches.add(0, batch);
        allTransfers.addAll(0, items);

        startTransferService();
        transferExecutor.execute(() -> processBatch(batch));
        return batch;
    }

    // --- BATCH PROCESSOR ---

    private void processBatch(TransferBatch batch) {
        batch.setState(TransferState.STARTING);

        for (TransferItem item : batch.getItems()) {
            if (batch.getState() == TransferState.CANCELLED || item.isCancelled()) {
                item.setState(TransferState.CANCELLED);
                notifyStateChanged(item);
                continue;
            }

            if (item.getType() == TransferType.UPLOAD) {
                executeUploadItem(batch, item);
            } else {
                executeDownloadItem(batch, item);
            }
        }

        if (batch.getCancelledCount() == batch.getTotalCount()) {
            batch.setState(TransferState.CANCELLED);
        } else if (batch.getFailedCount() > 0) {
            batch.setState(TransferState.FAILED);
        } else {
            batch.setState(TransferState.COMPLETED);
        }

        notifyBatchCompleted(batch);
    }

    // --- UPLOAD EXECUTION ---

    private void executeUploadItem(TransferBatch batch, TransferItem item) {
        item.setState(TransferState.UPLOADING);
        notifyStateChanged(item);

        long[] lastSampleTime = {System.currentTimeMillis()};
        long[] lastSampleBytes = {0L};
        long[] smoothedSpeed = {0L};

        try {
            StreamingProgressRequestBody requestBody = new StreamingProgressRequestBody(
                    appContext.getContentResolver(),
                    item.getUri(),
                    appContext.getContentResolver().getType(item.getUri()),
                    item.getTotalBytes(),
                    (bytesWritten, totalBytes) -> {
                        if (item.isCancelled()) {
                            throw new RuntimeException("TRANSFER_CANCELLED_BY_USER");
                        }
                        item.setTransferredBytes(bytesWritten);

                        long now = System.currentTimeMillis();
                        long elapsed = now - lastSampleTime[0];
                        if (elapsed >= 400) {
                            long deltaBytes = bytesWritten - lastSampleBytes[0];
                            long currentSpeed = (deltaBytes * 1000) / elapsed;
                            smoothedSpeed[0] = (smoothedSpeed[0] == 0) ? currentSpeed : (long) (0.7 * smoothedSpeed[0] + 0.3 * currentSpeed);
                            item.setSpeedBytesPerSecond(smoothedSpeed[0]);

                            if (smoothedSpeed[0] > 0 && totalBytes > bytesWritten) {
                                item.setEtaSeconds((totalBytes - bytesWritten) / smoothedSpeed[0]);
                            }

                            lastSampleTime[0] = now;
                            lastSampleBytes[0] = bytesWritten;
                        }

                        notifyProgress(item);
                        notifyBatchProgress(batch);
                    }
            );

            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", item.getFileName(), requestBody);
            Call<UploadResponse> call = apiService.uploadFile(filePart);
            Response<UploadResponse> response = call.execute();

            if (response.isSuccessful()) {
                item.setTransferredBytes(item.getTotalBytes());
                item.setState(TransferState.COMPLETED);
                item.setCompletedAt(System.currentTimeMillis());
                notifyStateChanged(item);

                if (batch.getTotalCount() == 1) {
                    TransferNotificationHelper.postItemCompletedNotification(appContext, item);
                }
            } else {
                item.setState(TransferState.FAILED);
                item.setErrorMessage("Upload failed (HTTP " + response.code() + ")");
                notifyStateChanged(item);

                if (batch.getTotalCount() == 1) {
                    TransferNotificationHelper.postItemFailedNotification(appContext, item);
                }
            }
        } catch (Exception e) {
            if ("TRANSFER_CANCELLED_BY_USER".equals(e.getMessage()) || item.isCancelled()) {
                item.setState(TransferState.CANCELLED);
                notifyStateChanged(item);
                TransferNotificationHelper.postItemCancelledNotification(appContext, item);
            } else {
                item.setState(TransferState.FAILED);
                item.setErrorMessage(e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Network error");
                notifyStateChanged(item);

                if (batch.getTotalCount() == 1) {
                    TransferNotificationHelper.postItemFailedNotification(appContext, item);
                }
            }
        }
    }

    // --- DOWNLOAD EXECUTION ---

    private void executeDownloadItem(TransferBatch batch, TransferItem item) {
        item.setState(TransferState.DOWNLOADING);
        notifyStateChanged(item);

        long[] lastSampleTime = {System.currentTimeMillis()};
        long[] lastSampleBytes = {0L};
        long[] smoothedSpeed = {0L};

        File outputFile = null;
        try {
            Call<ResponseBody> call = apiService.downloadFile(item.getFileId(), item.isDecrypt());
            Response<ResponseBody> response = call.execute();

            if (!response.isSuccessful() || response.body() == null) {
                item.setState(TransferState.FAILED);
                item.setErrorMessage("Download error (HTTP " + response.code() + ")");
                notifyStateChanged(item);

                if (batch.getTotalCount() == 1) {
                    TransferNotificationHelper.postItemFailedNotification(appContext, item);
                }
                return;
            }

            ResponseBody body = response.body();
            long contentLength = body.contentLength();
            if (contentLength > 0 && item.getTotalBytes() <= 0) {
                item.setTotalBytes(contentLength);
            }

            // Create target file in standard Downloads directory
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }
            outputFile = new File(downloadsDir, item.getFileName());
            // Avoid clobbering existing file
            int counter = 1;
            String baseName = item.getFileName();
            String extension = "";
            int dotIdx = baseName.lastIndexOf('.');
            if (dotIdx > 0) {
                extension = baseName.substring(dotIdx);
                baseName = baseName.substring(0, dotIdx);
            }
            while (outputFile.exists()) {
                outputFile = new File(downloadsDir, baseName + " (" + counter + ")" + extension);
                counter++;
            }

            try (InputStream is = body.byteStream();
                 OutputStream os = new FileOutputStream(outputFile)) {

                byte[] buffer = new byte[16384];
                int read;
                long totalRead = 0;

                while ((read = is.read(buffer)) != -1) {
                    if (item.isCancelled()) {
                        throw new RuntimeException("TRANSFER_CANCELLED_BY_USER");
                    }
                    os.write(buffer, 0, read);
                    totalRead += read;
                    item.setTransferredBytes(totalRead);

                    long now = System.currentTimeMillis();
                    long elapsed = now - lastSampleTime[0];
                    if (elapsed >= 400) {
                        long deltaBytes = totalRead - lastSampleBytes[0];
                        long currentSpeed = (deltaBytes * 1000) / elapsed;
                        smoothedSpeed[0] = (smoothedSpeed[0] == 0) ? currentSpeed : (long) (0.7 * smoothedSpeed[0] + 0.3 * currentSpeed);
                        item.setSpeedBytesPerSecond(smoothedSpeed[0]);

                        if (smoothedSpeed[0] > 0 && item.getTotalBytes() > totalRead) {
                            item.setEtaSeconds((item.getTotalBytes() - totalRead) / smoothedSpeed[0]);
                        }

                        lastSampleTime[0] = now;
                        lastSampleBytes[0] = totalRead;
                    }

                    notifyProgress(item);
                    notifyBatchProgress(batch);
                }
                os.flush();
            }

            item.setDownloadedFile(outputFile);
            item.setTransferredBytes(item.getTotalBytes() > 0 ? item.getTotalBytes() : outputFile.length());
            item.setState(TransferState.COMPLETED);
            item.setCompletedAt(System.currentTimeMillis());
            notifyStateChanged(item);

            if (batch.getTotalCount() == 1) {
                TransferNotificationHelper.postItemCompletedNotification(appContext, item);
            }

        } catch (Exception e) {
            if (outputFile != null && outputFile.exists()) {
                outputFile.delete();
            }

            if ("TRANSFER_CANCELLED_BY_USER".equals(e.getMessage()) || item.isCancelled()) {
                item.setState(TransferState.CANCELLED);
                notifyStateChanged(item);
                TransferNotificationHelper.postItemCancelledNotification(appContext, item);
            } else {
                item.setState(TransferState.FAILED);
                item.setErrorMessage(e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Download failed");
                notifyStateChanged(item);

                if (batch.getTotalCount() == 1) {
                    TransferNotificationHelper.postItemFailedNotification(appContext, item);
                }
            }
        }
    }

    // --- CANCELLATION & CONTROL ---

    public void cancelTransfer(String transferId) {
        if (transferId == null) return;
        for (TransferItem item : allTransfers) {
            if (transferId.equals(item.getId())) {
                item.cancel();
                notifyStateChanged(item);
                break;
            }
        }
    }

    public void cancelBatch(String batchId) {
        if (batchId == null) return;
        for (TransferBatch batch : batches) {
            if (batchId.equals(batch.getId())) {
                batch.cancel();
                notifyBatchProgress(batch);
                break;
            }
        }
    }

    public TransferBatch getActiveBatch() {
        for (TransferBatch b : batches) {
            if (!b.isFinished()) {
                return b;
            }
        }
        return null;
    }

    public TransferItem getActiveItem() {
        for (TransferItem item : allTransfers) {
            if (item.getState() == TransferState.UPLOADING ||
                item.getState() == TransferState.DOWNLOADING ||
                item.getState() == TransferState.STARTING ||
                item.getState() == TransferState.QUEUED) {
                return item;
            }
        }
        return null;
    }

    public boolean hasActiveTransfers() {
        for (TransferItem item : allTransfers) {
            if (item.getState() == TransferState.UPLOADING ||
                item.getState() == TransferState.DOWNLOADING ||
                item.getState() == TransferState.STARTING ||
                item.getState() == TransferState.QUEUED) {
                return true;
            }
        }
        return false;
    }

    public List<TransferItem> getAllTransfers() {
        return Collections.unmodifiableList(allTransfers);
    }
}
