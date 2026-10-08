package com.ciphervault.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.format.Formatter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;

public class FileUtils {

    public static String formatStorageSize(long bytes) {
        if (bytes < 0) bytes = 0;
        if (bytes == 1073741824L) {
            return "1 GB";
        }
        if (bytes >= 1073741824L) {
            double gb = (double) bytes / (1024.0 * 1024.0 * 1024.0);
            if (Math.abs(gb - Math.round(gb)) < 0.01) {
                return String.format(Locale.US, "%d GB", Math.round(gb));
            }
            return String.format(Locale.US, "%.2f GB", gb);
        } else if (bytes >= 1048576L) {
            double mb = (double) bytes / (1024.0 * 1024.0);
            if (Math.abs(mb - Math.round(mb)) < 0.05) {
                return String.format(Locale.US, "%d MB", Math.round(mb));
            }
            return String.format(Locale.US, "%.1f MB", mb);
        } else if (bytes >= 1024L) {
            double kb = (double) bytes / 1024.0;
            if (Math.abs(kb - Math.round(kb)) < 0.05) {
                return String.format(Locale.US, "%d KB", Math.round(kb));
            }
            return String.format(Locale.US, "%.1f KB", kb);
        } else {
            return bytes + " B";
        }
    }

    public static String formatStorageSize(Context context, long bytes) {
        return formatStorageSize(bytes);
    }

    public static String getFileName(Context context, Uri uri) {
        String result = "unknown_file";
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int colIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (colIndex != -1) {
                        result = cursor.getString(colIndex);
                    }
                }
            } catch (Exception ignored) {}
        }
        return result;
    }

    public static long getFileSize(Context context, Uri uri) {
        long size = 0;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int colIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (colIndex != -1) {
                        size = cursor.getLong(colIndex);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (size <= 0) {
            try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r")) {
                if (pfd != null) {
                    long statSize = pfd.getStatSize();
                    if (statSize > 0) {
                        size = statSize;
                    }
                }
            } catch (Exception ignored) {}
        }
        return size;
    }

    public static String calculateSha256(Context context, Uri uri) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public interface DownloadProgressListener {
        void onProgress(int percent);
    }

    public interface DownloadCallback {
        void onSuccess(File savedFile);
        void onError(Exception e);
    }

    private static final ExecutorService DOWNLOAD_EXECUTOR =
            Executors.newFixedThreadPool(2);

    public static void saveResponseBodyToDownloads(
            Context context,
            String filename,
            long totalBytesExpected,
            ResponseBody body,
            DownloadProgressListener progressListener,
            DownloadCallback callback) {

        DOWNLOAD_EXECUTOR.execute(() -> {
            try {
                File cipherVaultDir = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "CipherVault"
                );
                if (!cipherVaultDir.exists()) {
                    cipherVaultDir.mkdirs();
                }

                File targetFile = new File(cipherVaultDir, filename);
                OutputStream os = null;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CipherVault");
                    Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri != null) {
                        os = context.getContentResolver().openOutputStream(uri);
                    }
                }

                if (os == null) {
                    os = new FileOutputStream(targetFile);
                }

                long contentLength = body.contentLength();
                if (contentLength <= 0) contentLength = totalBytesExpected;

                try (InputStream is = body.byteStream();
                     OutputStream targetOs = os) {
                    byte[] buffer = new byte[8192];
                    int read;
                    long totalRead = 0;
                    while ((read = is.read(buffer)) != -1) {
                        targetOs.write(buffer, 0, read);
                        totalRead += read;
                        if (contentLength > 0 && progressListener != null) {
                            int percent = (int) Math.min(100, (totalRead * 100) / contentLength);
                            progressListener.onProgress(percent);
                        }
                    }
                    targetOs.flush();
                }

                if (callback != null) {
                    callback.onSuccess(targetFile);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    public static void saveResponseBodyToDownloads(
            Context context,
            String filename,
            ResponseBody body,
            DownloadCallback callback) {
        saveResponseBodyToDownloads(context, filename, -1L, body, null, callback);
    }
}