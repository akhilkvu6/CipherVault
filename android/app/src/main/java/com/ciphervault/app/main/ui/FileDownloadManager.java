package com.ciphervault.app.main.ui;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.ciphervault.app.R;
import com.ciphervault.app.core.network.ApiClient;
import com.ciphervault.app.main.api.FileApi;
import com.ciphervault.app.main.model.FileResponse;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class FileDownloadManager {

    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface DownloadCallback {
        void onDownloadStarted(FileResponse file, boolean decrypted);
        void onDownloadFinished(FileResponse file, boolean success, File targetFile);
    }

    public static void showDownloadDialog(Context context, FileResponse file, DownloadCallback callback) {
        if (context == null || file == null) return;

        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(context, R.style.Theme_CipherVault_BottomSheetDialog);
        android.view.View sheetView = android.view.LayoutInflater.from(context).inflate(R.layout.bottom_sheet_download_mode, null);
        sheet.setContentView(sheetView);

        android.widget.TextView tvTitle = sheetView.findViewById(R.id.tvDownloadTitle);
        if (tvTitle != null && file.filename != null) {
            tvTitle.setText("Download " + file.filename);
        }

        android.view.View btnDecrypt = sheetView.findViewById(R.id.btnOptionDecrypt);
        if (btnDecrypt != null) {
            btnDecrypt.setOnClickListener(v -> {
                sheet.dismiss();
                startDownload(context, file, true, callback);
            });
        }

        android.view.View btnEncrypted = sheetView.findViewById(R.id.btnOptionEncrypted);
        if (btnEncrypted != null) {
            btnEncrypted.setOnClickListener(v -> {
                sheet.dismiss();
                startDownload(context, file, false, callback);
            });
        }

        android.view.View btnCancel = sheetView.findViewById(R.id.btnCancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> sheet.dismiss());
        }

        sheet.show();
    }

    public static void startDownload(Context context, FileResponse file, boolean decrypt, DownloadCallback callback) {
        if (context == null || file == null) return;

        int notificationId = (int) (file.id != null ? (file.id & 0x7FFFFFFF) : System.currentTimeMillis() % 100000);
        String filename = file.filename != null ? file.filename : "vault_file";
        if (!decrypt && !filename.endsWith(".encrypted")) {
            filename = filename + ".encrypted";
        }
        final String finalTargetFilename = filename;

        Toast.makeText(context, "Download started: " + finalTargetFilename, Toast.LENGTH_SHORT).show();
        TransferNotificationHelper.notifyProgress(context, notificationId, finalTargetFilename, false, 0);

        if (callback != null) {
            callback.onDownloadStarted(file, decrypt);
        }

        executor.execute(() -> {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists() || !downloadsDir.canWrite()) {
                downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (downloadsDir == null) {
                    downloadsDir = context.getFilesDir();
                }
            } else if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }
            File destFile = new File(downloadsDir, finalTargetFilename);

            try {
                FileApi api = ApiClient.getClient(context).create(FileApi.class);
                Response<ResponseBody> resp = api.downloadFile(file.id, decrypt).execute();

                if (!resp.isSuccessful() || resp.body() == null) {
                    TransferNotificationHelper.notifyFailed(context, notificationId, finalTargetFilename, false, "Server returned error " + resp.code());
                    mainHandler.post(() -> {
                        Toast.makeText(context, "Download failed for " + finalTargetFilename, Toast.LENGTH_SHORT).show();
                        if (callback != null) callback.onDownloadFinished(file, false, null);
                    });
                    return;
                }

                long totalBytes = resp.body().contentLength();
                if (totalBytes <= 0 && file.fileSize != null) {
                    totalBytes = file.fileSize;
                }

                try (InputStream is = resp.body().byteStream();
                     FileOutputStream fos = new FileOutputStream(destFile)) {

                    byte[] buffer = new byte[16384];
                    int read;
                    long bytesRead = 0;
                    int lastPercent = 0;

                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                        bytesRead += read;
                        if (totalBytes > 0) {
                            int pct = (int) ((bytesRead * 100) / totalBytes);
                            if (pct - lastPercent >= 5 || pct == 100) {
                                lastPercent = pct;
                                TransferNotificationHelper.notifyProgress(context, notificationId, finalTargetFilename, false, pct);
                            }
                        }
                    }
                    fos.flush();
                }

                TransferNotificationHelper.notifyComplete(context, notificationId, finalTargetFilename, false, destFile);
                mainHandler.post(() -> {
                    Toast.makeText(context, "Saved to Downloads: " + finalTargetFilename, Toast.LENGTH_LONG).show();
                    if (callback != null) callback.onDownloadFinished(file, true, destFile);
                });

            } catch (Exception e) {
                TransferNotificationHelper.notifyFailed(context, notificationId, finalTargetFilename, false, e.getMessage());
                mainHandler.post(() -> {
                    Toast.makeText(context, "Download error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (callback != null) callback.onDownloadFinished(file, false, null);
                });
            }
        });
    }
}
