package com.ciphervault.app.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.InputStream;
import java.security.MessageDigest;

public class FileUtils {

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
}