package com.ciphervault.app;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MetadataSearchUnitTest {

    private final Gson gson = new Gson();

    @Test
    public void testStoredFileMetadataDeserialization() {
        String json = "{"
                + "\"id\": 1,"
                + "\"filename\": \"landscape.jpg\","
                + "\"fileSize\": 2048000,"
                + "\"contentType\": \"image/jpeg\","
                + "\"encrypted\": true,"
                + "\"sha256Hash\": \"a1b2c3d4e5f6\","
                + "\"category\": \"Images\","
                + "\"hasPreview\": true,"
                + "\"metadata\": {"
                + "  \"cameraMake\": \"Canon\","
                + "  \"cameraModel\": \"Canon EOS 80D\","
                + "  \"lens\": \"EF-S 18-135mm\","
                + "  \"resolution\": \"6000x4000\","
                + "  \"width\": 6000,"
                + "  \"height\": 4000,"
                + "  \"dateTaken\": \"2023:10:15 14:30:00\","
                + "  \"iso\": \"100\","
                + "  \"fNumber\": \"f/5.6\""
                + "}"
                + "}";

        StoredFile file = gson.fromJson(json, StoredFile.class);

        assertNotNull(file);
        assertEquals(Long.valueOf(1), file.getId());
        assertEquals("landscape.jpg", file.getFilename());
        assertEquals("Canon EOS 80D", file.getCameraInfo());
        assertEquals("6000x4000", file.getResolution());
        assertEquals(StoredFile.FileCategory.IMAGES, file.getCategory());
        assertTrue(file.isEncrypted());
        assertTrue(file.hasPreview());

        FileMetadataDTO meta = file.getMetadata();
        assertNotNull(meta);
        assertEquals("Canon", meta.getCameraMake());
        assertEquals("Canon EOS 80D", meta.getCameraModel());
        assertEquals("EF-S 18-135mm", meta.getLens());
        assertEquals(Integer.valueOf(6000), meta.getWidth());
        assertEquals(Integer.valueOf(4000), meta.getHeight());
        assertEquals("2023:10:15 14:30:00", meta.getDateTaken());
    }

    @Test
    public void testVideoMetadataDeserialization() {
        String json = "{"
                + "\"id\": 2,"
                + "\"filename\": \"presentation.mp4\","
                + "\"fileSize\": 15000000,"
                + "\"contentType\": \"video/mp4\","
                + "\"encrypted\": false,"
                + "\"metadata\": {"
                + "  \"resolution\": \"1920x1080\","
                + "  \"videoCodec\": \"H.264 / AVC\","
                + "  \"audioCodec\": \"AAC\","
                + "  \"duration\": \"05:32\","
                + "  \"frameRate\": \"30.00\""
                + "}"
                + "}";

        StoredFile file = gson.fromJson(json, StoredFile.class);

        assertNotNull(file);
        assertEquals("presentation.mp4", file.getFilename());
        assertEquals("1920x1080", file.getResolution());
        assertEquals("H.264 / AVC", file.getCodec());
        assertEquals("05:32", file.getDuration());
        assertEquals(StoredFile.FileCategory.VIDEOS, file.getCategory());
    }

    @Test
    public void testDocumentMetadataDeserialization() {
        String json = "{"
                + "\"id\": 3,"
                + "\"filename\": \"annual_report.pdf\","
                + "\"fileSize\": 1024000,"
                + "\"contentType\": \"application/pdf\","
                + "\"metadata\": {"
                + "  \"title\": \"CipherVault Annual Report 2026\","
                + "  \"author\": \"Engineering Team\","
                + "  \"docCreatedDate\": \"2026-01-15\""
                + "}"
                + "}";

        StoredFile file = gson.fromJson(json, StoredFile.class);

        assertNotNull(file);
        assertEquals("annual_report.pdf", file.getFilename());
        assertEquals("Engineering Team", file.getArtistOrAuthor());
        assertEquals(StoredFile.FileCategory.PDFS, file.getCategory());
    }

    @Test
    public void testNullMetadataSafety() {
        String json = "{"
                + "\"id\": 4,"
                + "\"filename\": \"archive.zip\","
                + "\"fileSize\": 500000,"
                + "\"contentType\": \"application/zip\""
                + "}";

        StoredFile file = gson.fromJson(json, StoredFile.class);

        assertNotNull(file);
        assertNull(file.getMetadata());
        assertNull(file.getCameraInfo());
        assertNull(file.getResolution());
        assertNull(file.getDuration());
        assertNull(file.getCodec());
        assertNull(file.getArtistOrAuthor());
        assertEquals(StoredFile.FileCategory.OTHER, file.getCategory());
    }

    @Test
    public void testCameraInfoFormatting() {
        // When model already starts with make: "Sony" and "Sony Alpha 7"
        String json1 = "{\"metadata\": {\"cameraMake\": \"Sony\", \"cameraModel\": \"Sony Alpha 7\"}}";
        StoredFile file1 = gson.fromJson(json1, StoredFile.class);
        assertEquals("Sony Alpha 7", file1.getCameraInfo());

        // When model does not contain make: "Apple" and "iPhone 15 Pro"
        String json2 = "{\"metadata\": {\"cameraMake\": \"Apple\", \"cameraModel\": \"iPhone 15 Pro\"}}";
        StoredFile file2 = gson.fromJson(json2, StoredFile.class);
        assertEquals("Apple iPhone 15 Pro", file2.getCameraInfo());

        // When only make is present:
        String json3 = "{\"metadata\": {\"cameraMake\": \"Nikon\"}}";
        StoredFile file3 = gson.fromJson(json3, StoredFile.class);
        assertEquals("Nikon", file3.getCameraInfo());
    }
}
