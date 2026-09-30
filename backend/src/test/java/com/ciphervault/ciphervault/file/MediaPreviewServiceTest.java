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
}
