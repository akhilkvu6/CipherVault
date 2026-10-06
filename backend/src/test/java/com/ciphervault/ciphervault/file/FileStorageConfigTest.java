package com.ciphervault.ciphervault.file;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FileStorageConfigTest {

    @Test
    void testResolvePath_ValidRelative() {
        Path resolved = FileStorageConfig.resolvePath("uploads/test.txt");
        assertNotNull(resolved);
        assertTrue(resolved.normalize().startsWith(FileStorageConfig.STORAGE_ROOT.toAbsolutePath().normalize()));
    }

    @Test
    void testResolvePath_ValidStorageSegment() {
        Path resolved = FileStorageConfig.resolvePath("/var/www/html/storage/previews/image.jpg");
        assertNotNull(resolved);
        assertTrue(resolved.normalize().startsWith(FileStorageConfig.STORAGE_ROOT.toAbsolutePath().normalize()));
    }

    @Test
    void testResolvePath_ValidAbsoluteInsideRoot() {
        Path absolutePath = FileStorageConfig.STORAGE_ROOT.toAbsolutePath().resolve("encrypted/file.bin");
        Path resolved = FileStorageConfig.resolvePath(absolutePath.toString());
        assertNotNull(resolved);
        assertTrue(resolved.normalize().startsWith(FileStorageConfig.STORAGE_ROOT.toAbsolutePath().normalize()));
    }

    @Test
    void testResolvePath_TraversalThrowsSecurityException() {
        assertThrows(SecurityException.class, () -> {
            FileStorageConfig.resolvePath("../../../../etc/passwd");
        });
    }

    @Test
    void testResolvePath_TraversalWithStorageSegmentThrowsSecurityException() {
        assertThrows(SecurityException.class, () -> {
            FileStorageConfig.resolvePath("storage/../../../../Windows/System32/cmd.exe");
        });
    }

    @Test
    void testResolvePath_AbsoluteOutsideRootThrowsSecurityException() {
        assertThrows(SecurityException.class, () -> {
            FileStorageConfig.resolvePath("/etc/passwd");
        });
    }
}
