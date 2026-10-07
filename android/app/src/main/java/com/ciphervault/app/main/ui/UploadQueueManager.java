package com.ciphervault.app.main.ui;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;

import com.ciphervault.app.core.network.ApiClient;
import com.ciphervault.app.main.api.FileApi;
import com.ciphervault.app.main.model.FileUploadResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Response;

public class UploadQueueManager {

    public enum Stage {
        QUEUED("Queued"),
        PREPARING("Preparing"),
        HASHING("Hashing (SHA-256)"),
        DUPLICATE_CHECK("Checking duplicate"),
        ENCRYPTING("Encrypting"),
        UPLOADING("Uploading"),
        FINALIZING("Finalizing & Securing"),
        COMPLETED("Completed"),
        FAILED("Failed"),
        CANCELLED("Cancelled"),
        DUPLICATE("Duplicate file");

        private final String displayName;
        Stage(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public static class UploadItem {
        public final String id = UUID.randomUUID().toString();
        public final Uri uri;
        public String filename = "file";
        public long fileSize = 0;
        public String mimeType = "application/octet-stream";
        public Stage stage = Stage.QUEUED;
        public int progress = 0; // 0..100
        public long transferredBytes = 0;
        public long speedBytesPerSec = 0;
        public long etaSeconds = 0;
        public String sha256Hash = null;
        public String errorMessage = null;
        public Long duplicateFileId = null;
        public String duplicateFileName = null;
        public boolean cancelled = false;
        public Call<FileUploadResponse> activeCall = null;
        public File tempCacheFile = null;

        public UploadItem(Uri uri) {
            this.uri = uri;
        }

        public String getHumanReadableSize() {
            if (fileSize <= 0) return "0 B";
            double kb = fileSize / 1024.0;
            double mb = kb / 1024.0;
            double gb = mb / 1024.0;
            if (gb >= 1.0) return String.format(java.util.Locale.US, "%.1f GB", gb);
            if (mb >= 1.0) return String.format(java.util.Locale.US, "%.1f MB", mb);
            if (kb >= 1.0) return String.format(java.util.Locale.US, "%.1f KB", kb);
            return fileSize + " B";
        }
    }

    public interface QueueListener {
        void onQueueChanged();
        void onItemUpdated(UploadItem item);
    }

    private static volatile UploadQueueManager instance;
    private final List<UploadItem> items = new CopyOnWriteArrayList<>();
    private final List<QueueListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isBatchActive = false;

    public static UploadQueueManager getInstance() {
        if (instance == null) {
            synchronized (UploadQueueManager.class) {
                if (instance == null) {
                    instance = new UploadQueueManager();
                }
            }
        }
        return instance;
    }

    private UploadQueueManager() {}

    public void addListener(QueueListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(QueueListener listener) {
        listeners.remove(listener);
    }

    private void notifyQueueChanged() {
        mainHandler.post(() -> {
            for (QueueListener l : listeners) l.onQueueChanged();
        });
    }

    private void notifyItemUpdated(UploadItem item) {
        mainHandler.post(() -> {
            for (QueueListener l : listeners) l.onItemUpdated(item);
        });
    }

    public List<UploadItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addFiles(List<Uri> uris, Context context) {
        if (uris == null || uris.isEmpty()) return;
        for (Uri uri : uris) {
            if (uri == null) continue;
            boolean alreadyQueued = false;
            for (UploadItem existing : items) {
                if (existing.uri.equals(uri) && (existing.stage != Stage.FAILED && existing.stage != Stage.CANCELLED)) {
                    alreadyQueued = true;
                    break;
                }
            }
            if (alreadyQueued) continue;

            UploadItem item = new UploadItem(uri);
            resolveMetadata(item, context);
            items.add(item);
        }
        notifyQueueChanged();
    }

    private void resolveMetadata(UploadItem item, Context context) {
        try {
            if ("content".equals(item.uri.getScheme())) {
                try (Cursor cursor = context.getContentResolver().query(item.uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                        if (nameIndex != -1) item.filename = cursor.getString(nameIndex);
                        if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) item.fileSize = cursor.getLong(sizeIndex);
                    }
                }
            }
            if (item.filename == null || item.filename.isEmpty()) {
                String path = item.uri.getPath();
                if (path != null) {
                    int idx = path.lastIndexOf('/');
                    item.filename = idx != -1 ? path.substring(idx + 1) : path;
                }
            }
            item.mimeType = context.getContentResolver().getType(item.uri);
            if (item.mimeType == null) item.mimeType = "application/octet-stream";
        } catch (Exception ignored) {}
    }

    public void startUploadAll(Context context) {
        isBatchActive = true;
        for (UploadItem item : items) {
            if (item.stage == Stage.QUEUED || item.stage == Stage.FAILED || item.stage == Stage.CANCELLED) {
                startUpload(item, context);
            }
        }
    }

    public void startUpload(UploadItem item, Context context) {
        if (item == null || item.stage == Stage.UPLOADING || item.stage == Stage.FINALIZING) return;

        item.cancelled = false;
        item.errorMessage = null;
        item.stage = Stage.PREPARING;
        item.progress = 0;
        item.transferredBytes = 0;
        notifyItemUpdated(item);

        final Context appContext = context.getApplicationContext();

        executor.execute(() -> {
            try {
                // Stage 1: PREPARING (Cache to temp file)
                File tempFile = new File(appContext.getCacheDir(), "cv_up_" + item.id + "_" + item.filename);
                item.tempCacheFile = tempFile;

                try (InputStream is = appContext.getContentResolver().openInputStream(item.uri);
                     FileOutputStream fos = new FileOutputStream(tempFile)) {
                    if (is == null) throw new IllegalStateException("Cannot open input stream");
                    byte[] buf = new byte[65536];
                    int r;
                    long copied = 0;
                    while ((r = is.read(buf)) != -1) {
                        if (item.cancelled) {
                            cleanup(item);
                            item.stage = Stage.CANCELLED;
                            notifyItemUpdated(item);
                            return;
                        }
                        fos.write(buf, 0, r);
                        copied += r;
                    }
                    fos.flush();
                    item.fileSize = tempFile.length();
                }

                // Stage 2: HASHING (Calculate SHA-256 with real progress)
                item.stage = Stage.HASHING;
                notifyItemUpdated(item);

                MessageDigest md = MessageDigest.getInstance("SHA-256");
                try (InputStream is = new java.io.FileInputStream(tempFile)) {
                    byte[] buf = new byte[65536];
                    int r;
                    long hashed = 0;
                    long startTime = System.currentTimeMillis();
                    while ((r = is.read(buf)) != -1) {
                        if (item.cancelled) {
                            cleanup(item);
                            item.stage = Stage.CANCELLED;
                            notifyItemUpdated(item);
                            return;
                        }
                        md.update(buf, 0, r);
                        hashed += r;
                        if (item.fileSize > 0) {
                            item.progress = (int) ((hashed * 100) / item.fileSize);
                            long elapsed = Math.max(1, (System.currentTimeMillis() - startTime) / 1000);
                            item.speedBytesPerSec = hashed / elapsed;
                            if (item.speedBytesPerSec > 0) {
                                item.etaSeconds = (item.fileSize - hashed) / item.speedBytesPerSec;
                            }
                            notifyItemUpdated(item);
                        }
                    }
                }
                item.sha256Hash = HexFormat.of().formatHex(md.digest());

                // Stage 3: SHA-256 DUPLICATE CHECK
                item.stage = Stage.DUPLICATE_CHECK;
                notifyItemUpdated(item);

                FileApi api = ApiClient.getClient(appContext).create(FileApi.class);
                Response<Map<String, Object>> dupResp = api.checkDuplicate(item.sha256Hash).execute();
                if (dupResp.isSuccessful() && dupResp.body() != null) {
                    Map<String, Object> body = dupResp.body();
                    Boolean isDup = (Boolean) body.get("isDuplicate");
                    if (Boolean.TRUE.equals(isDup)) {
                        item.stage = Stage.DUPLICATE;
                        item.duplicateFileName = (String) body.get("existingFileName");
                        Object fId = body.get("fileId");
                        if (fId instanceof Number) item.duplicateFileId = ((Number) fId).longValue();
                        item.errorMessage = "Duplicate file already in vault: " + item.duplicateFileName;
                        cleanup(item);
                        notifyItemUpdated(item);
                        TransferNotificationHelper.notifyFailed(appContext, item.id.hashCode(), item.filename, true, "Already in vault");
                        return;
                    }
                }

                // Stage 4 & 5: ENCRYPTING & STREAM UPLOAD with real ProgressRequestBody
                item.stage = Stage.UPLOADING;
                item.progress = 0;
                item.transferredBytes = 0;
                notifyItemUpdated(item);

                final long uploadStartTime = System.currentTimeMillis();
                ProgressRequestBody reqBody = new ProgressRequestBody(tempFile, item.mimeType, (bytesWritten, totalBytes) -> {
                    if (item.cancelled) return;
                    item.transferredBytes = bytesWritten;
                    if (totalBytes > 0) {
                        item.progress = (int) ((bytesWritten * 100) / totalBytes);
                        long elapsed = Math.max(1, (System.currentTimeMillis() - uploadStartTime) / 1000);
                        item.speedBytesPerSec = bytesWritten / elapsed;
                        if (item.speedBytesPerSec > 0) {
                            item.etaSeconds = (totalBytes - bytesWritten) / item.speedBytesPerSec;
                        }
                        if (bytesWritten >= totalBytes) {
                            item.stage = Stage.FINALIZING;
                        }
                        notifyItemUpdated(item);
                        TransferNotificationHelper.notifyProgress(appContext, item.id.hashCode(), item.filename, true, item.progress);
                    }
                });

                MultipartBody.Part part = MultipartBody.Part.createFormData("file", item.filename, reqBody);
                item.activeCall = api.uploadFile(part);
                Response<FileUploadResponse> uploadResp = item.activeCall.execute();

                if (uploadResp.isSuccessful() && uploadResp.body() != null && uploadResp.body().success) {
                    item.stage = Stage.COMPLETED;
                    item.progress = 100;
                    item.transferredBytes = item.fileSize;
                    cleanup(item);
                    notifyItemUpdated(item);
                    TransferNotificationHelper.notifyComplete(appContext, item.id.hashCode(), item.filename, true, null);
                } else if (uploadResp.code() == 409) {
                    item.stage = Stage.DUPLICATE;
                    item.errorMessage = "Server duplicate detected";
                    cleanup(item);
                    notifyItemUpdated(item);
                    TransferNotificationHelper.notifyFailed(appContext, item.id.hashCode(), item.filename, true, "Duplicate detected");
                } else {
                    // VERIFY: Check if server actually completed the upload before reporting failure
                    if (verifyOnServer(api, item.sha256Hash)) {
                        item.stage = Stage.COMPLETED;
                        item.progress = 100;
                        cleanup(item);
                        notifyItemUpdated(item);
                        TransferNotificationHelper.notifyComplete(appContext, item.id.hashCode(), item.filename, true, null);
                    } else {
                        String errMsg = uploadResp.body() != null ? uploadResp.body().message : "Server error " + uploadResp.code();
                        item.stage = Stage.FAILED;
                        item.errorMessage = errMsg;
                        cleanup(item);
                        notifyItemUpdated(item);
                        TransferNotificationHelper.notifyFailed(appContext, item.id.hashCode(), item.filename, true, errMsg);
                    }
                }

            } catch (Exception e) {
                if (item.cancelled) {
                    item.stage = Stage.CANCELLED;
                } else {
                    // On socket timeout or lost response: verify with server before failing
                    try {
                        FileApi api = ApiClient.getClient(appContext).create(FileApi.class);
                        if (verifyOnServer(api, item.sha256Hash)) {
                            item.stage = Stage.COMPLETED;
                            item.progress = 100;
                            cleanup(item);
                            notifyItemUpdated(item);
                            TransferNotificationHelper.notifyComplete(appContext, item.id.hashCode(), item.filename, true, null);
                            return;
                        }
                    } catch (Exception ignored) {}

                    item.stage = Stage.FAILED;
                    item.errorMessage = e.getMessage() != null ? e.getMessage() : "Upload error";
                    TransferNotificationHelper.notifyFailed(appContext, item.id.hashCode(), item.filename, true, item.errorMessage);
                }
                cleanup(item);
                notifyItemUpdated(item);
            }
        });
    }

    private boolean verifyOnServer(FileApi api, String hash) {
        if (hash == null || api == null) return false;
        try {
            Response<Map<String, Object>> resp = api.checkDuplicate(hash).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                return Boolean.TRUE.equals(resp.body().get("isDuplicate"));
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void cleanup(UploadItem item) {
        if (item.tempCacheFile != null && item.tempCacheFile.exists()) {
            try { item.tempCacheFile.delete(); } catch (Exception ignored) {}
        }
    }

    public void cancelItem(String itemId) {
        for (UploadItem item : items) {
            if (item.id.equals(itemId)) {
                item.cancelled = true;
                if (item.activeCall != null && !item.activeCall.isCanceled()) {
                    item.activeCall.cancel();
                }
                cleanup(item);
                item.stage = Stage.CANCELLED;
                notifyItemUpdated(item);
                break;
            }
        }
    }

    public void cancelAll() {
        for (UploadItem item : items) {
            if (item.stage == Stage.PREPARING || item.stage == Stage.HASHING || item.stage == Stage.UPLOADING) {
                cancelItem(item.id);
            }
        }
    }

    public void retryItem(String itemId, Context context) {
        for (UploadItem item : items) {
            if (item.id.equals(itemId)) {
                startUpload(item, context);
                break;
            }
        }
    }

    public void retryAllFailed(Context context) {
        for (UploadItem item : items) {
            if (item.stage == Stage.FAILED || item.stage == Stage.CANCELLED) {
                startUpload(item, context);
            }
        }
    }

    public void removeCompleted() {
        List<UploadItem> toRemove = new ArrayList<>();
        for (UploadItem item : items) {
            if (item.stage == Stage.COMPLETED || item.stage == Stage.DUPLICATE) {
                toRemove.add(item);
            }
        }
        items.removeAll(toRemove);
        notifyQueueChanged();
    }

    public void clearAll() {
        cancelAll();
        items.clear();
        notifyQueueChanged();
    }

    public int getTotalCount() { return items.size(); }

    public int getCompletedCount() {
        int count = 0;
        for (UploadItem item : items) {
            if (item.stage == Stage.COMPLETED || item.stage == Stage.DUPLICATE) count++;
        }
        return count;
    }

    public long getTotalBatchBytes() {
        long total = 0;
        for (UploadItem item : items) total += item.fileSize;
        return total;
    }

    public long getTransferredBatchBytes() {
        long transferred = 0;
        for (UploadItem item : items) {
            if (item.stage == Stage.COMPLETED) {
                transferred += item.fileSize;
            } else {
                transferred += item.transferredBytes;
            }
        }
        return transferred;
    }

    public int getOverallBatchProgress() {
        long total = getTotalBatchBytes();
        if (total <= 0) return 0;
        return (int) Math.min(100, (getTransferredBatchBytes() * 100) / total);
    }
}
