package com.ciphervault.app;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class StreamingProgressRequestBody extends RequestBody {

    private static final int BUFFER_SIZE = 16384; // 16 KB fixed buffer size

    public interface ProgressListener {
        void onProgress(long bytesWritten, long totalBytes);
    }

    public interface InputStreamProvider {
        InputStream openStream() throws IOException;
    }

    private final InputStreamProvider streamProvider;
    private final String contentType;
    private final long contentLength;
    private final ProgressListener listener;
    private long lastProgressUpdate = 0;

    public StreamingProgressRequestBody(@NonNull ContentResolver contentResolver,
                                        @NonNull Uri uri,
                                        @Nullable String contentType,
                                        long contentLength,
                                        @Nullable ProgressListener listener) {
        this(() -> {
            InputStream is = contentResolver.openInputStream(uri);
            if (is == null) {
                throw new FileNotFoundException("Unable to open ContentResolver InputStream for URI: " + uri);
            }
            return is;
        }, contentType, contentLength, listener);
    }

    public StreamingProgressRequestBody(@NonNull InputStreamProvider streamProvider,
                                        @Nullable String contentType,
                                        long contentLength,
                                        @Nullable ProgressListener listener) {
        this.streamProvider = streamProvider;
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.listener = listener;
    }

    @Nullable
    @Override
    public MediaType contentType() {
        if (contentType != null && !contentType.trim().isEmpty()) {
            return MediaType.parse(contentType);
        }
        return MediaType.parse("application/octet-stream");
    }

    @Override
    public long contentLength() {
        return contentLength > 0 ? contentLength : -1L;
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        try (InputStream is = streamProvider.openStream()) {
            if (is == null) {
                throw new FileNotFoundException("InputStream provider returned null stream");
            }

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long bytesWritten = 0;
            lastProgressUpdate = 0;

            notifyProgress(0, contentLength);

            while ((bytesRead = is.read(buffer)) != -1) {
                sink.write(buffer, 0, bytesRead);
                bytesWritten += bytesRead;
                notifyProgress(bytesWritten, contentLength);
            }

            sink.flush();
            notifyProgress(bytesWritten, contentLength);
        }
    }

    private void notifyProgress(long bytesWritten, long totalBytes) {
        if (listener != null) {
            long now = System.currentTimeMillis();
            if (now - lastProgressUpdate >= 75 || bytesWritten == totalBytes || bytesWritten == 0) {
                lastProgressUpdate = now;
                listener.onProgress(bytesWritten, totalBytes);
            }
        }
    }
}
