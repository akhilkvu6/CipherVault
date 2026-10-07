package com.ciphervault.app.main.ui;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class ProgressRequestBody extends RequestBody {

    public interface ProgressListener {
        void onProgress(long bytesWritten, long totalBytes);
    }

    private final File file;
    private final String contentType;
    private final ProgressListener listener;

    public ProgressRequestBody(File file, String contentType, ProgressListener listener) {
        this.file = file;
        this.contentType = contentType != null ? contentType : "application/octet-stream";
        this.listener = listener;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse(contentType);
    }

    @Override
    public long contentLength() {
        return file.length();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        long fileLength = contentLength();
        byte[] buffer = new byte[32768]; // 32 KB buffer
        long uploaded = 0;

        try (Source source = Okio.source(file)) {
            Buffer bufferSink = new Buffer();
            long read;
            while ((read = source.read(bufferSink, buffer.length)) != -1) {
                sink.write(bufferSink, read);
                uploaded += read;
                if (listener != null) {
                    listener.onProgress(uploaded, fileLength);
                }
            }
        }
    }
}
