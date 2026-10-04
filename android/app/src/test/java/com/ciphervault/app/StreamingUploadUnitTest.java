package com.ciphervault.app;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.MediaType;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying memory-safe streaming upload behavior,
 * progress listener dispatch, and upload size boundary validation.
 */
public class StreamingUploadUnitTest {

    @Test
    public void testMaxUploadSizeBytesConstant() {
        // Confirmed backend limit is 200MB in backend/src/main/resources/application.properties
        long expected200Mb = 200L * 1024L * 1024L;
        assertEquals(expected200Mb, UploadFragment.MAX_UPLOAD_SIZE_BYTES);
        assertEquals(209715200L, UploadFragment.MAX_UPLOAD_SIZE_BYTES);
    }

    @Test
    public void testSizeValidationBoundaries() {
        long exactLimit = 200L * 1024L * 1024L;

        // Valid sizes
        assertTrue(1024L <= UploadFragment.MAX_UPLOAD_SIZE_BYTES);
        assertTrue(10L * 1024L * 1024L <= UploadFragment.MAX_UPLOAD_SIZE_BYTES);
        assertTrue(100L * 1024L * 1024L <= UploadFragment.MAX_UPLOAD_SIZE_BYTES);
        assertTrue(150L * 1024L * 1024L <= UploadFragment.MAX_UPLOAD_SIZE_BYTES);
        assertTrue(exactLimit <= UploadFragment.MAX_UPLOAD_SIZE_BYTES);

        // Exceeding sizes
        assertTrue((exactLimit + 1L) > UploadFragment.MAX_UPLOAD_SIZE_BYTES);
        assertTrue((201L * 1024L * 1024L) > UploadFragment.MAX_UPLOAD_SIZE_BYTES);
        assertTrue((500L * 1024L * 1024L) > UploadFragment.MAX_UPLOAD_SIZE_BYTES);
    }

    @Test
    public void testContentTypeAndContentLength() {
        StreamingProgressRequestBody body = new StreamingProgressRequestBody(
                () -> new ByteArrayInputStream(new byte[0]),
                "application/pdf",
                524288L,
                null
        );

        MediaType mediaType = body.contentType();
        assertNotNull(mediaType);
        assertEquals("application/pdf", mediaType.toString());
        assertEquals(524288L, body.contentLength());

        // Default content type when null or empty
        StreamingProgressRequestBody defaultTypeBody = new StreamingProgressRequestBody(
                () -> new ByteArrayInputStream(new byte[0]),
                null,
                -1L,
                null
        );
        assertEquals("application/octet-stream", defaultTypeBody.contentType().toString());
        assertEquals(-1L, defaultTypeBody.contentLength());
    }

    @Test
    public void testStreamingSmallPayloadIntegrityAndProgress() throws IOException {
        byte[] payload = "Hello, CipherVault Secure Cloud!".getBytes();
        List<Long> progressPoints = new ArrayList<>();

        StreamingProgressRequestBody body = new StreamingProgressRequestBody(
                () -> new ByteArrayInputStream(payload),
                "text/plain",
                payload.length,
                (bytesWritten, totalBytes) -> progressPoints.add(bytesWritten)
        );

        Buffer sinkBuffer = new Buffer();
        body.writeTo(sinkBuffer);

        // Verify output integrity
        byte[] writtenBytes = sinkBuffer.readByteArray();
        assertArrayEquals(payload, writtenBytes);

        // Verify progress milestones
        assertFalse(progressPoints.isEmpty());
        assertEquals(Long.valueOf(0), progressPoints.get(0));
        assertEquals(Long.valueOf(payload.length), progressPoints.get(progressPoints.size() - 1));
    }

    @Test
    public void testStreamingMultiChunkPayloadIntegrity() throws IOException {
        // 50 KB exceeds the 16 KB buffer, testing chunked streaming
        int testSize = 50 * 1024;
        byte[] payload = new byte[testSize];
        for (int i = 0; i < testSize; i++) {
            payload[i] = (byte) (i % 127);
        }

        AtomicLong finalBytesWritten = new AtomicLong(0);

        StreamingProgressRequestBody body = new StreamingProgressRequestBody(
                () -> new ByteArrayInputStream(payload),
                "application/octet-stream",
                testSize,
                (bytesWritten, totalBytes) -> finalBytesWritten.set(bytesWritten)
        );

        Buffer sinkBuffer = new Buffer();
        body.writeTo(sinkBuffer);

        byte[] writtenBytes = sinkBuffer.readByteArray();
        assertArrayEquals(payload, writtenBytes);
        assertEquals(testSize, finalBytesWritten.get());
    }

    @Test
    public void testLargeStreamWithoutAllocatingLargeByteArrayInMemory() throws IOException {
        // Stream 200 MB synthetically without holding 200 MB in JVM heap
        final long totalStreamSize = 200L * 1024L * 1024L; // 200 MB

        InputStream syntheticLargeStream = new InputStream() {
            private long bytesRemaining = totalStreamSize;

            @Override
            public int read() {
                if (bytesRemaining <= 0) return -1;
                bytesRemaining--;
                return 0x5A;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                if (bytesRemaining <= 0) return -1;
                int toRead = (int) Math.min(len, bytesRemaining);
                Arrays.fill(b, off, off + toRead, (byte) 0x5A);
                bytesRemaining -= toRead;
                return toRead;
            }
        };

        AtomicLong reportedTotal = new AtomicLong(0);
        StreamingProgressRequestBody body = new StreamingProgressRequestBody(
                () -> syntheticLargeStream,
                "application/octet-stream",
                totalStreamSize,
                (bytesWritten, totalBytes) -> reportedTotal.set(bytesWritten)
        );

        // Discard sink using Okio blackhole sink
        BufferedSink discardingSink = Okio.buffer(Okio.blackhole());
        body.writeTo(discardingSink);

        // Verify the entire 200 MB was streamed and written successfully without OOM
        assertEquals(totalStreamSize, reportedTotal.get());
    }
}
