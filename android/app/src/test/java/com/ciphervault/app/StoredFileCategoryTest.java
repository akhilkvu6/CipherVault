package com.ciphervault.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StoredFileCategoryTest {

    @Test
    public void testApkFileCategorizedAsOther() {
        StoredFile file1 = new StoredFile(1L, "sample-app.apk", 1024L, "application/vnd.android.package-archive", "2026-10-10", false);
        assertEquals(StoredFile.FileCategory.OTHER, file1.getCategory());

        StoredFile file2 = new StoredFile(2L, "sample-app.apk", 1024L, "application/zip", "2026-10-10", false);
        assertEquals(StoredFile.FileCategory.OTHER, file2.getCategory());

        StoredFile file3 = new StoredFile(3L, "payload.apk", 1024L, "application/octet-stream", "2026-10-10", false);
        file3.setBackendCategory("Archives");
        assertEquals(StoredFile.FileCategory.OTHER, file3.getCategory());
    }

    @Test
    public void testStandardArchivesRemainArchives() {
        StoredFile zipFile = new StoredFile(4L, "backup.zip", 2048L, "application/zip", "2026-10-10", false);
        assertEquals(StoredFile.FileCategory.ARCHIVES, zipFile.getCategory());

        StoredFile tarFile = new StoredFile(5L, "bundle.tar.gz", 2048L, "application/gzip", "2026-10-10", false);
        assertEquals(StoredFile.FileCategory.ARCHIVES, tarFile.getCategory());

        StoredFile sevenZFile = new StoredFile(6L, "data.7z", 2048L, "application/x-7z-compressed", "2026-10-10", false);
        assertEquals(StoredFile.FileCategory.ARCHIVES, sevenZFile.getCategory());
    }
}
