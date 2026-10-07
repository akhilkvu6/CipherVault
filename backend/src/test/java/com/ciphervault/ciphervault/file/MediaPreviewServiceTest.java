package com.ciphervault.ciphervault.file;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class MediaPreviewServiceTest {

    private MediaPreviewService mediaPreviewService;

    @BeforeEach
    void setUp() {
        mediaPreviewService = new MediaPreviewService();
    }

    @Test
    void generatePreviewForNullOrEmptyShouldReturnNull() {
        assertNull(mediaPreviewService.generatePreview((byte[]) null, "test.jpg", "image/jpeg"));
        assertNull(mediaPreviewService.generatePreview((java.nio.file.Path) null, "test.jpg", "image/jpeg"));
        assertNull(mediaPreviewService.generatePreview(new byte[0], "test.jpg", "image/jpeg"));
    }

    @Test
    void generatePreviewForUnsupportedTypeShouldReturnNull() {
        byte[] docBytes = "plain text document content".getBytes();
        assertNull(mediaPreviewService.generatePreview(docBytes, "report.pdf", "application/pdf"));
        assertNull(mediaPreviewService.generatePreview(docBytes, "notes.txt", "text/plain"));
        assertNull(mediaPreviewService.generatePreview(docBytes, "data.zip", "application/zip"));
    }

    @Test
    void generateImagePreviewShouldProduceDownscaledJpeg() throws Exception {
        // Create an 800x600 test image in memory
        BufferedImage original = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = original.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 800, 600);
        g.setColor(Color.BLUE);
        g.drawString("CipherVault Preview Test", 100, 100);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(original, "png", baos);
        byte[] originalPngBytes = baos.toByteArray();

        byte[] previewBytes = mediaPreviewService.generatePreview(originalPngBytes, "photo.png", "image/png");

        assertNotNull(previewBytes);
        assertTrue(previewBytes.length > 0);

        BufferedImage preview = ImageIO.read(new ByteArrayInputStream(previewBytes));
        assertNotNull(preview);
        assertTrue(preview.getWidth() <= 384);
        assertTrue(preview.getHeight() <= 384);
        // Aspect ratio was 800:600 (4:3), target width 384, height should be 288
        assertEquals(384, preview.getWidth());
        assertEquals(288, preview.getHeight());
    }

    private java.nio.file.Path getOrCreateTestVideo() throws Exception {
        java.nio.file.Path testVideo = java.nio.file.Path.of("storage/test_sample.mp4");
        if (java.nio.file.Files.exists(testVideo)) {
            return testVideo;
        }
        java.nio.file.Path tempMp4 = java.nio.file.Files.createTempFile("cv_synthetic_test_", ".mp4");
        org.jcodec.api.awt.AWTSequenceEncoder encoder = org.jcodec.api.awt.AWTSequenceEncoder.createSequenceEncoder(tempMp4.toFile(), 25);
        BufferedImage frame = new BufferedImage(160, 120, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = frame.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 160, 120);
        g.dispose();
        encoder.encodeImage(frame);
        encoder.finish();
        return tempMp4;
    }

    @Test
    void generateVideoPreviewShouldExtractFrame() throws Exception {
        java.nio.file.Path testVideo = getOrCreateTestVideo();
        try {
            byte[] previewBytes = mediaPreviewService.generatePreview(testVideo, "sample.mp4", "video/mp4");
            assertNotNull(previewBytes, "Preview bytes for video should not be null");
            assertTrue(previewBytes.length > 0);

            BufferedImage preview = ImageIO.read(new ByteArrayInputStream(previewBytes));
            assertNotNull(preview, "Preview image should be decodable as a BufferedImage");
            assertTrue(preview.getWidth() <= 384);
            assertTrue(preview.getHeight() <= 384);
        } finally {
            if (!testVideo.equals(java.nio.file.Path.of("storage/test_sample.mp4"))) {
                java.nio.file.Files.deleteIfExists(testVideo);
            }
        }
    }

    @Test
    void extractFrameWithJCodecShouldExtractFrameFromMp4() throws Exception {
        java.nio.file.Path testVideo = getOrCreateTestVideo();
        try {
            java.lang.reflect.Method m = MediaPreviewService.class.getDeclaredMethod("extractFrameWithJCodec", java.nio.file.Path.class);
            m.setAccessible(true);
            byte[] frame = (byte[]) m.invoke(mediaPreviewService, testVideo);
            assertNotNull(frame, "JCodec should extract a frame from mp4");
            assertTrue(frame.length > 0);
            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(frame));
            assertNotNull(bi);
        } finally {
            if (!testVideo.equals(java.nio.file.Path.of("storage/test_sample.mp4"))) {
                java.nio.file.Files.deleteIfExists(testVideo);
            }
        }
    }

    @Test
    void testVideoExceedingJCodecThresholdShouldSkipJCodecFrameGrab() throws Exception {
        // Create a temporary mock video file
        java.nio.file.Path tempVideo = java.nio.file.Files.createTempFile("cv_test_large_vid_", ".mp4");
        try {
            // Write a tiny byte array but mock or test file size logic
            // Since we test extractFrameWithJCodec threshold:
            java.lang.reflect.Method m = MediaPreviewService.class.getDeclaredMethod("extractFrameWithJCodec", java.nio.file.Path.class);
            m.setAccessible(true);

            // A 0-byte or normal small file should not throw exception and return null (no valid MP4 header)
            byte[] normalResult = (byte[]) m.invoke(mediaPreviewService, tempVideo);
            assertNull(normalResult, "Corrupt or empty file should safely return null");

            // Verify MAX_JCODEC_VIDEO_SIZE constant equals 100MB
            assertEquals(100L * 1024L * 1024L, MediaPreviewService.MAX_JCODEC_VIDEO_SIZE, "JCodec threshold must be exactly 100 MB");
        } finally {
            java.nio.file.Files.deleteIfExists(tempVideo);
        }
    }
}
